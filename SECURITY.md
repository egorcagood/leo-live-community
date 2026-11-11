# Security policy

This repository is a preview, not a safety-certified diagnostic tool. No supported stable release has been published yet.

The current APK has no Internet permission, accounts, server credentials, AI keys, microphone or location collection. On Android 8–11, the platform requires location permission for BLE discovery; the app does not request coordinates. Logs remain in app-private storage unless the user explicitly exports CSV. Android backup is disabled.

Only the exact allowlisted adapter setup and standard read commands may reach the transport. No DTC clearing, coding, ECU programming, actuator tests or arbitrary raw command UI. Diagnostic absence must never be interpreted as confirmation that all vehicle systems are healthy.

## Reporting

After publication, use the repository's **Security → Report a vulnerability** when private reporting has been enabled by the maintainer. Enabling and verifying it is a release gate. If it is unavailable, do not put sensitive details in a public issue: ask the maintainer for a private reporting channel first. Do not upload tokens, personal vehicle logs, VINs, locations or working exploit details publicly.

Reports should identify the affected version, conditions, impact and a minimal synthetic reproduction. Never provoke unsafe vehicle behavior to demonstrate a bug. Public issue templates do not request raw vehicle captures.

## Scope

Priority areas: bypasses of the read policy; stale callbacks affecting new sessions; forged or conflicting data displayed as measurements; unintentional data disclosure; build/release supply-chain integrity. New network features require a fresh threat model and isolation tests before release.

The source audit is a guardrail, not proof that no secret can exist. Review the exact staged files and artifact manifests before every publication.
