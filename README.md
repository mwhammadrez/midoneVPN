# MidONe

Android VPN/proxy client for censored networks. Package `dev.tunnelkit.app`.
Built around the Xray core through libXray's JNI bridge, MIT/MPL-only stack, no root.

> **Status honesty rule for this repo:** every capability row is one of `passed` ·
> `failed` · `not_run` · `blocked_license` · `blocked_credentials` · `blocked_environment` ·
> `implemented_but_unverified`. Nothing is marked working without evidence you can reproduce.

Current build: **0.10.0** (`versionCode 14`) · 91 JVM tests · see `docs/CAPABILITY_MATRIX.md`.

## What works today

| Area | State | Evidence |
|---|---|---|
| VLESS/VMess/Trojan over WS · gRPC · XHTTP · TCP | passed | real Xray 26.10.21 accepts the generated config; live tunnel carried HTTP end to end (`docs/E2E_RESULTS.md`) |
| REALITY / uTLS | passed (config) | `Configuration OK` from the real binary |
| CDN fronting (CDN address + Host + SNI) | passed (config) | same path; the app also tries the site's own domain when the apex refuses |
| WireGuard | implemented_but_unverified | outbound rendered and accepted by the core; no WG server dialled yet |
| DNS tunnel | not_run | StormDNS/MasterDNS (MIT) not integrated |
| Psiphon / OpenVPN / IKEv2 | blocked_license | GPL engines; owner has not enabled GPL |
| On-device VPN path | blocked_environment | needs a physical device on a real network |

## Build

```bash
export JAVA_HOME=/path/to/jdk17
export ANDROID_HOME=/path/to/android-sdk
./gradlew :app:assembleDebug            # APK per ABI in app/build/outputs/apk/debug/
./gradlew :app:testDebugUnitTest        # 91 tests, no device needed
```

Release builds require `keystore.properties` (never committed); the build fails loudly without it
instead of producing an unsigned artifact.

## Verify without a phone

```bash
# real Xray binary, same version as the bundled libXray
curl -L -o /tmp/xray.zip https://github.com/XTLS/Xray-core/releases/download/v26.10.21/Xray-linux-64.zip
unzip -o /tmp/xray.zip -d /tmp/xray
XRAY_BIN=/tmp/xray/xray ./scripts/e2e-local.sh
```

That script feeds the configs this app generates to the real core and pushes traffic through a live
VLESS tunnel, including a negative control (kill the tunnel, traffic must stop).

## Layout

```
app/src/main/java/dev/tunnelkit/
  core/engine/    VpnEngine contract, EngineCoordinator (one engine at a time)
  core/config/    VlessLink, XrayConfigBuilder, WireGuardConfig/Outbound, subscriptions
  core/net/       Preflight (TCP/TLS/HTTP), EndpointScanner
  core/profile/   Keystore-backed ProfileStore
  engine/xray/    libXray binding, protocol envelope, probe runner
  app/            VpnService, UI (Compose), diagnostics log
docs/             architecture, capability matrix, test plan, licence audit, e2e evidence
skills/           reusable agent skills that produced this app
scripts/          libXray fetch (SHA-256) and local end-to-end harness
third_party/      dependency manifest with licences
```

## Security notes

- Profile storage is `EncryptedSharedPreferences` (Keystore); no plaintext fallback.
- `DiagLog` redacts links, UUIDs and keys on write, so a pasted log is safe to share.
- The app never logs the UUID that identifies the user to the server.
- Upstream audit (licences, pinned commits) in `docs/UPSTREAM_AUDIT.md`.

## Licences

libXray MIT · Xray-core MPL-2.0 · WireGuard Apache-2.0 · see `THIRD_PARTY_NOTICES.md`.
GPL components are deliberately absent: this is a closed product.
