# Community local release implementation plan

> For agentic workers: use superpowers:executing-plans inline. The owner requested no subagents in the preceding audit; preserve this boundary. No product commits or remote publishing before review.

**Goal:** Prepare a standalone, reviewable Android local OBD/demo release candidate and accurate public instructions.

**Architecture:** A small platform-free protocol/session core, one BLE controller, and one native Android screen. Demo and physical readings share the presentation but never storage or transport ownership. No backend is required or claimed.

**Tech Stack:** Java 17, Android API 26–36, Android Gradle plugin 9.0.1, Gradle 9.1.0; platform Android UI/BLE and no runtime third-party libraries.

**Spec:** ../../design.md

## Global constraints

- Only new Community repository may change. Preserve private repositories byte-for-byte for tracked files.
- No network permission, AI calls, uploads, OEM probes, DTC clearing or automatic vehicle connection.
- Public publication waits for owner approval of the exact text/license/content.
- Source preview and physical hardware acceptance are different claims.

## Review focus

- Old callback after disconnect must not mutate a new session (Task 2/3 generation tests).
- Conflicting ECU responses must not yield a fabricated value (Task 1 codec tests).
- A pending DTC request must not bypass capability or transport policy (Task 1/3 tests).
- Timeout/buffer overflow must stop transmission without automatic retries (Task 2 tests).
- Demo/real switching and export must keep provenance separate (Task 1/3 tests).

## Task 1: Read-only protocol and synthetic session

Files: `android/app/src/main/java/community/leo/obd/core/{ReadPolicy,ObdCodec,SessionLog,DemoVehicle}.java`, `tests/CoreChecks.java`, `scripts/test-core.sh`.

Interfaces: `ReadPolicy.frame(String): byte[]` validates exact approved commands; `ObdCodec.supported(String,int): Set<Integer>`; `ObdCodec.value(String,int): double`; `ObdCodec.dtc(String,String): List<String>`; `SessionLog.append(long,String,String,String,String)` and `csv()`; `DemoVehicle.response(int,long): String`.

- [x] Write failing behavior tests for unsafe commands, valid/invalid/multiple-responder payloads, supported maps, DTC decoding, bounded isolated CSV and demo.
- [x] Run `scripts/test-core.sh`, verify expected missing behavior failure.
- [x] Implement the core without Android imports or private data.
- [x] Run the whole core suite.

## Task 2: Request ownership

Files: core `RequestQueue.java`, tests extended.

Interfaces: `RequestQueue.enqueue(String)`, `next(long)`, `receive(String,long)`, `expire(long)`, `cancel()`. One current request, bounded queue/buffer, a terminal failure event; controller owns callback generation.

- [x] RED tests for one-in-flight, echoed fragmented responses, prompt completion, overflow, timeout, cancellation and unsafe commands.
- [x] Implement and run the complete suite.

## Task 3: Android application

Files: build/manifest/resources, `MainActivity.java`, `BleConnection.java`.

- [x] Add boundary checks for no network permission, no exported diagnostics, required source policy before writes and distinct package.
- [x] Implement scan/select/connect, GATT profile validation, notifications, initialization, capability discovery, supported polling, explicit DTC read, stop, demo and document-picker CSV export.
- [x] Build APK; inspect manifest, dex contents and UI where an authorized test device/emulator is available. Do not install on the user's phone automatically.

## Task 4: Public documentation and release gate

Files: README, setup/user guide, architecture/compatibility/roadmap, LICENSE draft, NOTICE, SECURITY, CONTRIBUTING, release workflow, provenance allowlist and review draft.

- [x] Write tested paths and steps, no placeholder download URL or fake hosted service.
- [x] Add read-only release audit checking explicit file list, secrets/private material patterns, symlinks, artifacts and license gate.
- [x] Verify documentation links and full tests/build; inspect release candidate manually.
- [x] Show owner exact editable text and remaining field-validation blockers. No push until approval.

Local tasks complete to source-preview level; pending device/vehicle and publication gates are recorded in ../../progress.md. No hardware acceptance inferred.
