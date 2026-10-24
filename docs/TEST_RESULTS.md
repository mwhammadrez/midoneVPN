# Test results

Environment: Linux x86_64 container, JDK 17 (Temurin), Android SDK 34, Gradle 8.9, AGP 8.5.2.
No device, no emulator, no server credentials, no Iranian network.

## Baseline build (2026-10-24)

| Check | Command | Result |
|---|---|---|
| Compile + tests | `./gradlew :app:testDebugUnitTest` | **61 tests, 0 failures** |
| Debug APK (all ABIs) | `./gradlew :app:assembleDebug` | 141 MB universal |
| Debug APK (per ABI, splits) | same | **arm64 41 MB · armv7 41 MB · x86_64 43 MB** |
| Release APK (R8) | `:app:assembleRelease` | 39 / 38 / 41 MB (unsigned) |
| Release AAB | `:app:bundleRelease` | **51 MB** (all ABIs) |
| UI snapshot (JVM) | `./gradlew recordPaparazziDebug` | PNG rendered, committed as baseline |

## Test inventory

| Suite | Cases | What it covers | Status |
|---|---|---|---|
| `Ipv4Test` | 4 | IPv4 header parse, ICMP echo reply, checksum, malformed input | passed |
| `UiSnapshotTest` | 1 | Compose screen renders on JVM (Paparazzi) | passed |
| `EngineCoordinatorTest` | 8 | state machine, cleanup, switching, idempotency, blocked engines | passed |
| `XrayAdapterTest` | 6 | descriptor/provenance, validation, API v3 envelope, typed failures, stop+DNS reset | passed |
| `ConfigParsingTest` | 9 | format detection, base64/plain subscriptions, caps, oversize | passed |
| `SubscriptionFetcherTest` | 7 | SSRF guard, HTTPS-only, redirect re-check, last-good fallback | passed |
| `EndpointScannerTest` | 5 | ranking, budgets, dedupe, total-timeout cancellation, crash isolation | passed |
| `PingBatchTest` | 4 | `pingBatch` envelope, result parsing, runner mapping | passed |
| `WireGuardConfigTest` | 10 | WG parsing, validation, unknown fields, redaction | passed |
| `ProfileCodecTest` | 5 | encrypted-store codec, sanitisation, malformed lines, redaction | passed |
| `VlessLinkTest` (skill asset) | 5 | `vless://` parsing + Xray JSON | passed (in `skills/`) |

## Engine-by-engine

| Engine | Unit | Build | Handshake | Tunnel | Traffic | Notes |
|---|---|---|---|---|---|---|
| Xray (libXray v26.10.21) | 6 adapter tests passed | **build_verified** (28 tests, 41 MB/ABI) | not_run | not_run | not_run | TUN fd path (`XRAY_TUN_FD`) implemented |
| hev-socks5-tunnel | not_run | not_run | — | not_run | not_run | NDK build not yet wired |
| WireGuard | not_run | not_run | not_run | not_run | not_run | not started |
| DNS tunnel (StormDNS/MasterDNS) | not_run | not_run | not_run | not_run | not_run | not started |
| Psiphon | — | — | — | — | — | `blocked_license` (GPL-3.0) |
| UAC / Aether | — | — | — | — | — | `blocked_license` (no licence / AGPL) |
| OpenVPN / IKEv2 / Windscribe | — | — | — | — | — | `blocked_license` (GPL) |

## Size findings (measured, not estimated)

`libgojni.so` (the Go runtime + Xray core) is ~33–36 MB **per ABI**. A universal APK is therefore
141 MB; ABI splits reduce it to 41–43 MB per device. Implications: ship an **AAB**, keep `arm64-v8a`
mandatory, and expect the native core — not our Kotlin — to dominate download size.

## Bugs found by these tests

1. `EngineCoordinator` skipped `stop()` when `start()` threw mid-way → leaked native handles.
   Fixed: cleanup is unconditional on the failure path. Caught by `startFailureCleansUp`.
2. `TunnelService` referenced `android.R.drawable.stat_sys_vpn_ic`, which does not exist → compile
   failure. Fixed in the skill asset.

## Network findings (2026-10-24)

- The user's VLESS link (`security=none`, port 80, host `arvancloud.ir`) is **rejected by upstream
  Xray ≥24.x**: `vless without TLS or other encryption is prohibited unless the server address is a
  private IP or domain`. Confirmed on 26.10.21 (libXray pin) and accepted by 1.8.24.
- From this sandbox, plain HTTP/80 egress is blocked (control test `example.com:80` also times out),
  and the ArvanCloud IPs do not answer on 80 or 443. **Server reachability therefore could not be
  verified here** — this is an environment limit, not evidence about the server.

## Not run (and why)

- `connectedDebugAndroidTest` — no device.
- Any real handshake — no server credentials and no authorised endpoint in this environment.
- Iranian-network behaviour — `Connectivity on an Iranian network has not been verified.`

## Repro

```bash
export JAVA_HOME=<jdk17>; export ANDROID_HOME=<sdk>
./gradlew :app:testDebugUnitTest :app:assembleDebug
```
