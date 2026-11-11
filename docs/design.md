# Community 0.1 - local OBD and demo

Approved scope: 11 November 2026, owner selected local OBD and demo first, server and voice later.

An independent Android application, `community.leo.obd`, Android 8+ (API 26), compile/target API 36, Java 17 source. It can coexist with private LEO Live. No networking permission, cloud client, provider key, account or telemetry upload in 0.1.

Entry screen: explain local privacy, allow offline demo or explicit BLE scan. Scan displays adapter candidates and requires selection; never auto-connect to another user's adapter. Initial adapter profile is vLinker FD+ BLE with known GATT characteristics. Other transports and profiles remain unsupported until validated.

Local telemetry: discover supported service 01 PID bitmaps; poll only supported RPM, speed, coolant, load, throttle, fuel percentage and module voltage at bounded cadence. Standard engine stored/pending DTC reads are explicit user actions, distinct from SRS/ABS. Each value has timestamp and source. Conflicting responders fail closed. No fuel litres or odometer claims.

Transport: one owner, one in-flight request, prompt-delimited ELM replies, bounded buffers, write callback checks, GATT notification setup, timeout, and generation checks. Only fixed adapter initialization and approved standard reads can reach the BLE write. Disconnect cancels requests and clears current measurements. No background reconnect. This first release stops real connection when the activity is stopped; say so in the UI/docs. A later recording foreground service requires its own acceptance.

Demo drives the same decoded-measurement presentation using synthetic ELM replies, a permanently visible DEMO label, no BLE and no network. Starting a real connection creates a new local session; never blend demo measurements with real ones.

Local session log: bounded CSV in app-private storage; export only through Android's document picker. Explicit export is the only way data leaves the app. No coordinates, VIN, audio or Bluetooth address in export. No automatic public Downloads copy. Clearing a local session must not send any vehicle command.

Source strategy: clean repository, original minimal implementation informed by the private architecture; reuse only small explicitly inspected permissively owned code when justified, with provenance. Do not copy the original monolithic engine or Mode 04 path. Historical vehicle acceptance does not apply to the new app.

Publication gate: clean-room build, host behavior tests, APK permission/artifact inspection, release file allowlist, owner text/license review, then parked real-device acceptance before labeling hardware support verified. A source preview may be labeled unverified; do not call it a stable vehicle release.

Later modes: authenticated self-hosted backend with per-user keys server-side, budgets and optional Telegram; separate consent-based hosted Auto API. These remain roadmap only.
