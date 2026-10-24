# Build environment

| Item | Value |
|---|---|
| OS | Linux x86_64 container (no KVM) |
| JDK | Temurin 17 (`/home/user/toolchain/jdk17`) |
| Android SDK | platforms 34 + 35 + 36, build-tools 34.0.0 + 36.0.0 |
| Gradle | 8.9 (wrapper) |
| AGP / Kotlin | 8.7.3 / 2.0.21 |
| compileSdk / targetSdk / minSdk | 35 / 36 / 26 |
| RAM constraint | 3.9 GB → `org.gradle.jvmargs=-Xmx1024m`, `workers.max=2`, in-process Kotlin |

```bash
export JAVA_HOME=/path/to/jdk17
export ANDROID_HOME=/path/to/android-sdk
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

Outputs: `app/build/outputs/apk/debug/app-<abi>-debug.apk` (41 MB arm64, 41 MB armv7, 43 MB x86_64).

## Release signing (owner must do this)

1. Create a keystore **once**, outside the repository:

```bash
keytool -genkeypair -v -keystore ~/keys/midone-release.jks -alias midone   -keyalg RSA -keysize 4096 -validity 10000 -dname "CN=MidONe, O=MidONe, C=IR"
```

2. Create `keystore.properties` at the project root (git-ignored, never commit it):

```properties
storeFile=/absolute/path/to/midone-release.jks
storePassword=…
keyAlias=midone
keyPassword=…
```

3. Build:

```bash
./gradlew :app:bundleRelease     # signed AAB for Play
./gradlew :app:assembleRelease   # per-ABI signed APKs for sideloading
```

Without `keystore.properties`, any release task fails immediately with an explicit message — the
project never emits an unsigned "release" artifact by accident.
