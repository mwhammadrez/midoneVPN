# Client architectures — read from their build files (2026-10-24)

Not guesses: these numbers come from each project's `app/build.gradle.kts`, module list and README
on 2026-10-24.

| Project | compileSdk / min / target | Core | Pump | UI | Notable choices |
|---|---|---|---|---|---|
| `2dust/v2rayNG` | 37 / 24 / 37 | Xray (local `libs/*.aar`) | native + tun2socks | Compose + CameraX (QR) | MMKV, WorkManager **multiprocess**, local AAR, no Maven core |
| `SaeedDev94/Xray` | 37 / 26 / 37 | Xray (`libs/*.aar`) | `ndkBuild` via `src/main/jni/Android.mk` | Compose | **JDK 21**, NDK `30.0.16248370`, Room, libsu (root mode), ABI splits |
| `wgtunnel/android` | 37 / 26 / 37 | WireGuard + AmneziaWG | GoBackend | Compose | modularised: `networkmonitor`, `pinger`, `logcatter` as separate modules |
| `heiher/sockstun` | — | SOCKS5 only | `hev-socks5-tunnel` | classic views | smallest complete app; `ndk-build` + recursive submodule |
| `hiddify/hiddify-app` | — | multi (sing-box, Xray…) | per-core | Flutter | one UI, many cores; heavy but broad compat |
| `SagerNet/sing-box` (SFA) | — | sing-box `libbox` | libbox TUN | Compose | monorepo; GPL + name clause |

## What each one teaches

**v2rayNG** — how to survive 64k stars with a hand-managed AAR. Copy the *pattern*: core as a local
AAR, version pinned in `libs/`, `abiFilters` per build type, QR import through CameraX (the single
most-requested feature in every VPN client).

**SaeedDev94/Xray** — the cleanest small Kotlin client. It proves you do not need Flutter or a
multi-module monster: Compose + Room + one NDK directory + one AAR. Its `Android.mk` is a working
example of wiring a native pump into an app module — start here if you write your own service.

**wgtunnel** — proves the value of splitting `networkmonitor`/`pinger`/`logcatter` into modules: the
parts that cause the hardest bugs (network state, liveness, log capture) become independently
testable.

**sockstun** — the minimum viable shape: one Activity, one service, one native lib. Best "stage 3"
reference for someone learning the stack.

## Numbers that repeat across all of them

- `compileSdk = 37` and `targetSdk = 37` on every actively maintained client (Play floor is 36).
- `minSdk = 24–26`. Going to 24 costs you API-26 niceties (notification channels are 26+, but you
  need a channel anyway); 26 is the pragmatic floor.
- JDK 21 is now common (`SaeedDev94/Xray` compiles with it) — AGP 8.5+ supports it.
- NDK `30.x` in 2026 builds (`ndkVersion = "30.0.16248370"`); older `26.x` still works for hev.
- Everyone ships **ABI splits** and no universal APK.

## The common shape (what your app should look like)

```
app/                 Compose UI, state, profile store (Room/DataStore)
service/ (or inside app)
  TunnelService      VpnService, foreground, lifecycle, watchdog
  pump/              tun2socks JNI (hev) or core's own TUN
  core/              libXray AAR (or WG GoBackend)
native/              jni/Android.mk -> libhev-socks5-tunnel.so
```

Anything more elaborate is optional. Anything less is missing a lifecycle path.
