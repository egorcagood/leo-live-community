"""Synthetic public-tree checks. Never inspect private product files."""
import importlib.util
from pathlib import Path
import tempfile
import unittest
import zipfile

ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('audit', ROOT/'scripts/release-audit.py')
audit=importlib.util.module_from_spec(spec)
spec.loader.exec_module(audit)

class ReleaseAudit(unittest.TestCase):
    def setUp(self):
        self.temp=tempfile.TemporaryDirectory();self.root=Path(self.temp.name)
        (self.root/'README.md').write_text('Synthetic fixture\n')
        (self.root/'release-files.txt').write_text('README.md\nrelease-files.txt\n')
    def tearDown(self):self.temp.cleanup()
    def test_allowed_archive_contains_only_reviewed_files(self):
        files=audit.inspect(self.root,require_wrapper=False)
        (self.root/'.private').mkdir();(self.root/'.private'/'never.txt').write_text('local')
        target=self.root/'.private'/'preview.zip';audit.archive(self.root,files,target)
        with zipfile.ZipFile(target) as z:
            self.assertEqual(set(z.namelist()),{'README.md','release-files.txt','SHA256SUMS'})
    def test_extracted_preview_can_be_audited(self):
        files=audit.inspect(self.root,require_wrapper=False)
        target=self.root/'out.zip';audit.archive(self.root,files,target)
        with tempfile.TemporaryDirectory() as extracted:
            with zipfile.ZipFile(target) as z:z.extractall(extracted)
            self.assertEqual(audit.inspect(Path(extracted),require_wrapper=False),files)
    def test_traversal_rejected(self):
        (self.root/'release-files.txt').write_text('../outside\n')
        with self.assertRaises(audit.AuditError):audit.inspect(self.root,require_wrapper=False)
    def test_secret_value_not_printed(self):
        secret='sk-'+('fictitious'*6)
        (self.root/'README.md').write_text(secret)
        with self.assertRaises(audit.AuditError) as e:audit.inspect(self.root,require_wrapper=False)
        self.assertNotIn(secret,str(e.exception));self.assertIn('README.md',str(e.exception))
    def test_symlink_rejected(self):
        (self.root/'README.md').unlink();(self.root/'README.md').symlink_to('/dev/null')
        with self.assertRaises(audit.AuditError):audit.inspect(self.root,require_wrapper=False)
    def test_unreviewed_file_rejected(self):
        (self.root/'unexpected.txt').write_text('not reviewed')
        with self.assertRaises(audit.AuditError):audit.inspect(self.root,require_wrapper=False)
    def test_artifact_rejected_even_if_allowlisted(self):
        (self.root/'release-files.txt').write_text('dump.db\nrelease-files.txt\nREADME.md\n');(self.root/'dump.db').write_bytes(b'example')
        with self.assertRaises(audit.AuditError):audit.inspect(self.root,require_wrapper=False)
    def test_broken_relative_doc_link(self):
        (self.root/'README.md').write_text('[missing](docs/no.md)')
        with self.assertRaises(audit.AuditError):audit.inspect(self.root,require_wrapper=False)
    def test_crlf_and_mode_preserved_in_archive(self):
        (self.root/'README.md').write_bytes(b'line\r\n');(self.root/'README.md').chmod(0o755)
        files=audit.inspect(self.root,require_wrapper=False);target=self.root/'out.zip';audit.archive(self.root,files,target)
        with zipfile.ZipFile(target) as z:
            self.assertEqual(z.read('README.md'),b'line\r\n');self.assertTrue(z.getinfo('README.md').external_attr>>16 & 0o111)
if __name__=='__main__':unittest.main()
