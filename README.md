# MidONe — Android VPN client (engine foundation)

Multi-engine VPN client for users on heavily filtered networks. Built engine-first: honest state
machine, real protocol adapters, bounded probing — UI last.

**Status: early. One engine works (Xray/VLESS), the rest are blocked or not started. Nothing has been
verified on a physical device or on an Iranian network.**

## What is real today

| Area | State |
|---|---|
| Xray (VLESS / Reality / WS / XHTTP) via libXray `v26.10.21` | implemented, build-verified |
| TUN fd passed to Xray (`XRAY_TUN_FD`) | implemented; semantics verified on the Linux binary |
| `vless://` parsing + Xray config generation | implemented + tested |
| Subscription parsing and HTTPS fetching (SSRF guard, last-good cache) | implemented + tested |
| WireGuard `.conf` parsing and validation | implemented + tested (engine **not** integrated) |
| Endpoint scanner (`pingBatch`, budgets, cancel, ranking) | implemented + tested (no UI yet) |
| Single-screen UI, profile import, 7-method picker | implemented |
| **56 unit tests** | passing |

## What is not

- Any real connection: no credentials/device here. `Connectivity on an Iranian network has not been verified.`
- WireGuard, DNS tunnel engines — parsing exists, transport does not.
- Psiphon, CDN-fronted meek, SNI spoofing, OpenVPN/IKEv2/Stealth/WStunnel — `blocked_license` for a
  non-GPL product (see `docs/LICENSE_REVIEW.md`).
- `targetSdk` is 34; Play requires 36.
- Profile storage is plain SharedPreferences (not Keystore-backed yet).

## Build

```bash
./scripts/fetch-libxray.sh          # downloads the pinned AAR and verifies SHA-256
export JAVA_HOME=<jdk17> ANDROID_HOME=<sdk>
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug        # per-ABI APKs, ~41 MB each
./gradlew :app:recordPaparazziDebug # UI snapshots
```

The AAR is intentionally not committed (51 MB); the script pins `v26.10.21` and its SHA-256.

## Layout

```
app/src/main/java/dev/tunnelkit/
  core/model/     ConnectionState, Failure, EngineMetrics
  core/engine/    VpnEngine contract, EngineDescriptor, EngineCoordinator
  core/config/    vless, Xray builder, subscriptions, WireGuard
  core/net/       EndpointScanner, ProbeRunner
  core/profile/   Profile, ProfileStore
  engine/xray/    XrayCore, LibXrayCore, XrayAdapter, XrayProbeRunner
  ui/             HomeScreen, Tokens, Method catalogue
docs/             upstream audit, licence review, architecture, test results, limitations
```

## Documentation

`docs/UPSTREAM_AUDIT.md` · `CAPABILITY_MATRIX.md` · `ARCHITECTURE.md` · `TEST_RESULTS.md` ·
`KNOWN_LIMITATIONS.md` · `LICENSE_REVIEW.md` · `SECURITY.md` · `SCANNER_POLICY.md` ·
`BUILD_ENVIRONMENT.md` · `MILESTONE_REVIEW.md` · `CONFIG_FORMATS.md`

## Licence

Not yet chosen. Third-party components and their obligations: see `THIRD_PARTY_NOTICES.md` and
`docs/LICENSE_REVIEW.md`. No GPL/AGPL component is included.
