# Device test results

Date: 2026-10-24. Build: `android-app` @ commit at time of writing, `dev.tunnelkit.app 0.1.0`.

## Environment

| Item | Value |
|---|---|
| Device | **none available** — `adb devices` empty, no USB bus in the container, no emulator (no KVM) |
| Android version | — |
| ABI | — |
| Network | container egress only (plain HTTP/80 blocked; several Iranian IPs unreachable) |
| Iranian network | **not tested** |

**Therefore every device test below is `blocked_environment`, not `failed`.** Nothing in this file
claims a working connection.

## Scanner (`pingBatch`) end to end

| Check | Status | Notes |
|---|---|---|
| Real VLESS config probe | blocked_environment | needs a device **and** a working profile |
| TCP reachability level | not_run | not separable without a device |
| TLS handshake level | not_run | — |
| Protocol handshake level | not_run | — |
| Tunnel established | not_run | — |
| HTTPS through tunnel | not_run | — |
| Cancellation on device | not_run | unit-tested only (`totalBudgetCancelsAndReports`) |
| Duplicate-scan prevention | not_run | UI guards with `scanning` flag; not exercised |
| Budget enforcement | passed (unit) | `enforcesTargetBudget`, `deduplicatesTargets` |

## Xray connection path

| Check | Status | Notes |
|---|---|---|
| VPN permission grant/deny | not_run | code path exists in `MainActivity` + `XrayTunnelService` |
| TUN creation/cleanup | not_run | `establish()` → fd passed via `XRAY_TUN_FD` |
| libXray startup | not_run | AAR packaged (`libgojni.so` per ABI) |
| protectFd | not_run | wired via `registerDialerController` |
| DNS configuration | not_run | `setDNS("1.1.1.1:53")` before `runXray` |
| Real HTTPS through tunnel | not_run | probe exists (`generate_204`), never executed |
| Stop / restart / reconnect | not_run | — |
| Wi-Fi ↔ mobile handover | not_run | — |

## Keystore storage

| Check | Status | Notes |
|---|---|---|
| Import + restart persistence | not_run | store compiles; codec unit-tested (5 tests) |
| Process death | not_run | — |
| Backup/restore leaks | not_run | `allowBackup=false` is set |
| Invalidated/corrupt Keystore | not_run | design: throws instead of falling back to plaintext |
| Migration from plaintext | not_run | code path exists (`migrateLegacy`) |
| Codec round-trip | passed (unit) | `ProfileCodecTest` |

## Release build

| Check | Status | Notes |
|---|---|---|
| R8 + shrinking | passed | AAB builds; missing-class rule for Tink added |
| Signed AAB | passed | verified with `jarsigner` using a throwaway key (not committed) |
| Unsigned release | intentionally impossible | build fails with an explicit message |
| Release app starts / imports / connects | blocked_environment | needs a device |
| Size | passed | AAB 51 MB · arm64 39 MB · armv7 38 MB · x86_64 41 MB |

## What the owner must provide to finish this phase

1. A working **VLESS profile** (`vless://…`) that actually connects today, ideally TLS/REALITY on 443.
   A `security=none` link on a public host cannot work with stock Xray (`see UPSTREAM_AUDIT`).
2. A **physical device** reachable via `adb connect <ip>:5555` (USB debugging on), or a USB pass-through.
3. Confirmation of whether the device is on an **Iranian network**.
