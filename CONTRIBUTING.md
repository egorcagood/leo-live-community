# Contributing

Start with a small issue explaining the symptom or feature. Do not post private captures, credentials or OEM databases. Community has its own source history; do not import private product Git history.

Run from the repository root:

```sh
sh scripts/test-core.sh
python3 -m unittest discover -s tests -p 'test_*.py'
python3 scripts/release-audit.py
cd android
./gradlew :app:assembleDebug :app:lintDebug
```

Use Java 17 source compatibility and Android API 26-compatible runtime APIs. Keep the protocol core free of Android imports so behavior is testable without a device. New supported data needs documented units, invalid/missing/multiple-responder cases and provenance. Synthetic fixtures should be clearly labelled.

The final transport write boundary must enforce `ReadPolicy`. Public MVP changes may not add write/clear/programming operations, raw command fields, hidden uploads or automatic paid calls. Never make an unsupported PID look supported by inserting a demo value.

Keep documentation honest about host tests, emulator tests and real-vehicle tests. They are different forms of evidence. Add intentional public files to `release-files.txt`; generated/local files are never released automatically. Review dependencies and their licenses before adding them. Contributions are proposed under the repository's Apache-2.0 license as stated in LICENSE.
