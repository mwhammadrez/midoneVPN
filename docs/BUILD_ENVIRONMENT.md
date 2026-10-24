# Build environment

| Item | Value |
|---|---|
| OS | Linux x86_64 container (no KVM) |
| JDK | Temurin 17 (`/home/user/toolchain/jdk17`) |
| Android SDK | platform 34, build-tools 34.0.0 |
| Gradle | 8.9 (wrapper) |
| AGP / Kotlin | 8.5.2 / 2.0.21 |
| RAM constraint | 3.9 GB → `org.gradle.jvmargs=-Xmx1024m`, `workers.max=2`, in-process Kotlin |

```bash
export JAVA_HOME=/path/to/jdk17
export ANDROID_HOME=/path/to/android-sdk
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

Outputs: `app/build/outputs/apk/debug/app-<abi>-debug.apk` (41 MB arm64, 41 MB armv7, 43 MB x86_64).
Release builds unsigned (`app-<abi>-release-unsigned.apk`) — no keystore in this environment.
