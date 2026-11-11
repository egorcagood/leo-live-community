# Preparation record - 2026-11-11

## Owner decisions

Local OBD and offline demo first; server, BYOK, Voice and optional hosted integration are later phases. Publishing waits for review of the exact public text, license and release contents.

Independent Community repository with a new history, not a fork or a linked worktree of a private product. No private Git objects copied. No remote repository, commit, push, deployment, vehicle probe or user-phone installation performed during this preparation.

## Implemented and locally checked

- Platform-free read policy, strict codecs, single-flight queue, synthetic demo and bounded CSV session storage.
- Android BLE selection, known-profile validation, capability discovery, polling, explicit engine DTC reads, stop and local persistence.
- No Internet/microphone permission; separate application ID; backup disabled.
- Public documentation, Apache-2.0 proposal, third-party wrapper provenance, release allowlist, tested archive audit and pinned GitHub CI definition.
- Host tests: 90 core assertions and 13 Python checks.
- Android debug build and lint pass. Lint retains 22 warnings (20 SetTextI18n, one version-dependent attribute, one newer-AGP advisory); zero lint errors. Russian-only preview UI has not undergone localization.
- Android 36 ARM64 ATD emulator: launch, labelled demo, changing measurements, synthetic DTC, bounded private journal, replacement confirmation, background stop/cleared current values and journal recovery after restart observed. Rendered screen inspected at 320×640.
- Compiled APK permission and DEX checks confirm the distinct package, read-policy class, no Internet permission and no private service domain.

## Remaining acceptance

This is a source-preview candidate, not a field-accepted binary release. Real BLE/ECU operation, SRS (not implemented), other Android versions and signing/update behavior are not verified by the emulator.

The ATD image has no ACTION_CREATE_DOCUMENT handler. The app handles this without crashing and explains the missing file picker. End-to-end system-picker CSV export still needs an ordinary Android device. CSV serialization, private storage and recovery are separately verified.

BLE Android callback generation guards are implemented and source-reviewed; pure core queue tests do not replace full instrumented BLE callback/race testing. GitHub CI is prepared but has not run remotely. Dependencies are version-pinned; a full transitive dependency lock/SBOM and signed APK review remain binary-release work.

Background collection is deliberately absent: leaving the app stops it. No automatic reconnect, unsupported OEM commands or paid AI calls.

## Next actions

1. Owner edits README and publication draft, approves license and release scope.
2. Run the documented phone/parked-vehicle checklist; record actual hardware compatibility.
3. Create the separate public repository only after approval, enable security reporting and branch protections, run CI.
4. Create a separate release-signing identity and validate any public APK before publishing it.

Local audit artifacts and private source-preservation evidence are excluded from the public allowlist. Central project-context registration remains pending; this file and the local handoff preserve the continuation state.

## Publication authorization update, 2026-11-11

Owner edited the announcement, approved publication as an early prerelease without a vehicle test, and requested conversational Russian documentation. This supersedes the earlier pending-publication gate for 0.1.0-preview.1 source only. No APK is included in the public release. Historical verification notes above describe the preparation stage. iOS is not implemented.
