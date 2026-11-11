# LEO Live Community

Standalone public-source candidate. Never read or change production services, credentials, private vehicle histories, or neighboring repositories as part of building this application.

- First release: local read-only OBD and an offline synthetic demo. No cloud, AI, Telegram or account integration is implemented in this release.
- One Android application with a separate package and signing identity from private LEO Live.
- Vehicle command policy belongs at the final transport write boundary. No arbitrary commands, DTC clearing, OEM probes, coding, actuators or ECU programming.
- Demo data must stay visibly synthetic and separate from real sessions and exports.
- Never promote absent, old, conflicting or unsupported data to a current measurement or zero.
- One in-flight OBD request. Timeout or desynchronization closes the connection; never blindly retry commands.
- Publish only an explicit reviewed file allowlist. No inherited Git history, user captures, credentials, OEM definition databases or local build artifacts.
- Tests must cover behavior, including transport policy and failure paths. Build evidence is not vehicle acceptance.
- Do not publish/push/release until the owner approves the exact README, license and release contents.
- Record release blockers honestly. Do not mark planned backend or voice features as available.
