# Third-party components and provenance

## Distributed with source

- **Gradle wrapper 9.1.0**: `android/gradlew`, `android/gradlew.bat`, `android/gradle/wrapper/gradle-wrapper.jar`. Apache-2.0; original notices in scripts preserved. [Gradle source](https://github.com/gradle/gradle/tree/v9.1.0), [license](https://github.com/gradle/gradle/blob/v9.1.0/LICENSE).
- The wrapper JAR SHA-256 is `76805e32c009c0cf0dd5d206bddc9fb22ea42e84db904b764f3047de095493f3`, verified against the [official Gradle checksum list](https://gradle.org/release-checksums/). The 9.1.0 distribution checksum is pinned in wrapper properties.
- Application code, tests, icon and documentation were prepared specifically for Community. No vehicle capture, OEM definition database, user history, credential file or private Git object is part of the release file allowlist.

## Build-time tooling (not bundled source)

The build downloads Gradle 9.1.0 and Android Gradle Plugin 9.0.1 with their transitive dependencies from official repositories. AGP is part of Android's Apache-2.0-licensed tooling. Android SDK packages are obtained separately under their applicable Android SDK terms; they are not redistributed in this source preview. A JDK is also installed separately.

There are no declared third-party application runtime dependencies in `app/build.gradle.kts`. Android build tooling may generate compatibility/desugaring code in the APK. This is not a claim that the APK consists exclusively of handwritten application code.

Before adding dependencies, record their license, exact version, source and distribution obligations here. Before a signed binary release, inspect the resulting APK and build dependency report. Do not reuse private project dependency lists as evidence for this separate application.
