#!/usr/bin/env python3
"""Inspect an explicit public allowlist. Reports paths, never matched secret values."""
import argparse
import hashlib
import os
from pathlib import Path, PurePosixPath
import re
import stat
import zipfile

WRAPPER='android/gradle/wrapper/gradle-wrapper.jar'
WRAPPER_SHA='76805e32c009c0cf0dd5d206bddc9fb22ea42e84db904b764f3047de095493f3'
IGNORED={'.git','.private','.gradle','build','__pycache__','.idea'}
FORBIDDEN_SUFFIXES={'.apk','.aab','.db','.sqlite','.sqlite3','.zip','.jks','.keystore','.pem','.key','.csv','.log','.cbf','.smr-d'}
PATTERNS=[
    re.compile(r'sk-' + r'[A-Za-z0-9_-]{24,}'),
    re.compile(r'\b\d{8,12}:' + r'[A-Za-z0-9_-]{30,}\b'),
    re.compile(r'-----BEGIN ' + r'(?:RSA |EC |OPENSSH )?PRIVATE KEY-----'),
    re.compile('/'+'Users/'+r'[^/\s]+/'),
    re.compile(r'\bWDB' + r'[A-HJ-NPR-Z0-9]{14}\b'),
    re.compile(r'https?://' + r'(?:[a-z0-9-]+\.)*leo-live\.com\b'),
]
class AuditError(Exception):pass

def inspect(root,require_wrapper=True):
    root=Path(root).resolve()
    allow=root/'release-files.txt'
    if allow.is_symlink() or not allow.is_file():raise AuditError('Invalid allowlist: release-files.txt')
    names=[line.strip() for line in allow.read_text().splitlines() if line.strip() and not line.startswith('#')]
    if len(names)!=len(set(names)) or 'release-files.txt' not in names:raise AuditError('Duplicate or incomplete allowlist: release-files.txt')
    for name in names:
        p=PurePosixPath(name)
        if p.is_absolute() or '..' in p.parts or str(p)!=name or '\\' in name or any(x in IGNORED for x in p.parts):raise AuditError('Unsafe allowlist path')
        if p.suffix.lower() in FORBIDDEN_SUFFIXES or p.name.startswith('.env') or p.name=='local.properties':raise AuditError('Forbidden file: '+name)
        if p.suffix=='.jar' and name!=WRAPPER:raise AuditError('Unexpected binary: '+name)
        file=root/name
        if any(parent.is_symlink() for parent in [file,*file.parents] if parent!=root.parent):raise AuditError('Symlink: '+name)
        if not file.is_file() or not file.resolve().is_relative_to(root):raise AuditError('Missing or escaped file: '+name)
        if file.stat().st_size>1_000_000:raise AuditError('Oversized source file: '+name)
        body=file.read_bytes()
        if name==WRAPPER:
            if hashlib.sha256(body).hexdigest()!=WRAPPER_SHA:raise AuditError('Wrapper checksum mismatch: '+name)
            continue
        try:text=body.decode('utf-8')
        except UnicodeDecodeError:raise AuditError('Unexpected binary: '+name) from None
        if '\0' in text:raise AuditError('Unexpected binary: '+name)
        if any(pattern.search(text) for pattern in PATTERNS):raise AuditError('Sensitive-looking content: '+name)
        if name.endswith('.md'):
            for link in re.findall(r'\]\(([^)\s]+)\)',text):
                if '://' in link or link.startswith('#') or link.startswith('mailto:'):continue
                target=(file.parent/link.split('#')[0]).resolve()
                if not target.is_relative_to(root) or str(target.relative_to(root)) not in names:raise AuditError('Unreleased document link: '+name)
    hashes={name:hashlib.sha256((root/name).read_bytes()).hexdigest() for name in sorted(names)}
    manifest=root/'SHA256SUMS'
    if manifest.exists() or manifest.is_symlink():
        expected=''.join(f'{digest}  {name}\n' for name,digest in hashes.items())
        if manifest.is_symlink() or manifest.read_text()!=expected:raise AuditError('Invalid source manifest: SHA256SUMS')
    # Local build outputs are excluded; all other files need explicit review.
    for directory,dirs,files in os.walk(root,followlinks=False):
        dirs[:]=[d for d in dirs if d not in IGNORED]
        for d in dirs:
            if (Path(directory)/d).is_symlink():raise AuditError('Symlink directory: '+str((Path(directory)/d).relative_to(root)))
        for file in files:
            p=Path(directory)/file;name=p.relative_to(root).as_posix()
            if file in {'local.properties','.DS_Store'} or name=='SHA256SUMS':continue
            if name not in names:raise AuditError('Unreviewed file: '+name)
    if require_wrapper and WRAPPER not in names:raise AuditError('Wrapper missing from allowlist')
    return hashes

def archive(root,files,target):
    # Recheck bytes immediately before writing; no symlinks or paths outside root.
    root=Path(root).resolve();target=Path(target);target.parent.mkdir(parents=True,exist_ok=True)
    with zipfile.ZipFile(target,'w',compression=zipfile.ZIP_DEFLATED) as out:
        for name,digest in files.items():
            path=root/name
            if path.is_symlink() or not path.resolve().is_relative_to(root):raise AuditError('File changed during packaging: '+name)
            body=path.read_bytes()
            if hashlib.sha256(body).hexdigest()!=digest:raise AuditError('File changed during packaging: '+name)
            info=zipfile.ZipInfo(name,(2026,1,1,0,0,0));info.create_system=3
            info.external_attr=(stat.S_IFREG | (0o755 if path.stat().st_mode&0o111 else 0o644))<<16
            info.compress_type=zipfile.ZIP_DEFLATED;out.writestr(info,body)
        info=zipfile.ZipInfo('SHA256SUMS',(2026,1,1,0,0,0));info.create_system=3;info.external_attr=(stat.S_IFREG|0o644)<<16
        out.writestr(info,''.join(f'{digest}  {name}\n' for name,digest in files.items()))

def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--archive',action='store_true');args=parser.parse_args()
    root=Path(__file__).resolve().parents[1]
    try:
        files=inspect(root);print(f'PASS: {len(files)} reviewed source files; no publication performed')
        if args.archive:
            target=root/'.private/dist/community-source-preview.zip';archive(root,files,target)
            print('Archive: .private/dist/community-source-preview.zip');print('SHA256: '+hashlib.sha256(target.read_bytes()).hexdigest())
    except AuditError as error:parser.exit(1,'FAIL: '+str(error)+'\n')
if __name__=='__main__':main()
