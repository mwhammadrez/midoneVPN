# assets/ — drop-in code

All Kotlin here is **dependency-free** (no AndroidX, no libXray types) so it compiles and unit-tests
on the JVM. Copy into `app/src/main/java/dev/tunnelkit/core/`.

| File | What it is | Status |
|---|---|---|
| `VlessLink.kt` | `vless://` parser → validated model → Xray client JSON | **verified**: 5 unit tests + generated JSON validated with `json.tool` |
| `HevBridge.kt` | JNI face over hev-socks5-tunnel + YAML config builder | compile-only (needs native lib) |
| `hev_jni.c` | the 5 JNI functions the bridge needs | compile-only |
| `TunnelService.kt` | production-shaped VpnService: lifecycle, watchdog, handover | template — wire your core into `startPump()` |

## Why these four

They encode the four things every home-grown client gets wrong: link parsing with the
`publicKey`/`password` rename, MTU defaults, teardown ordering, and treating "connected" as success
instead of probing it.

## VlessLink — usage

```kotlin
val result = VlessLink.parse(clipboardText)
result.onSuccess { link ->
    val problems = link.validate()          // empty = usable
    if (problems.isEmpty()) runXray(link.toXrayJson())
}.onFailure { show("لینک نامعتبر: ${it.message}") }
```

Verified behaviour (tests in `VlessLinkTest`):
- reality link with `pbk`/`sid`/`sni`/`flow` parses and emits `"password"` (Xray 26.x name)
- `flow=xtls-rprx-vision` + `type=ws` is rejected instead of failing at runtime
- IPv6 authority `[2001:db8::1]:8443` parses
- malformed links return `Result.failure`, never throw
- output is valid JSON with `direct` + `proxy` outbounds and a private-IP routing rule

## HevBridge — build

```bash
cd app/src/main/jni
git clone --recursive https://github.com/heiher/hev-socks5-tunnel jni
cp ../../../../skills/.../assets/hev_jni.c jni/          # or paste
ndk-build NDK_PROJECT_PATH=. APP_ABI=arm64-v8a,armeabi-v7a,x86_64
```
Then `HevBridge.init(pfd.fd)` → `run()` on a worker thread → `stop()` → `join` → `close(pfd)` →
`fini()`. That exact order is what the 78-comment "Android Crashed" issue is about.

## TunnelService — wire points

1. `startPump()` — replace `HevBridge` with your core.
2. `onTransportChanged()` — must restart the **upstream**, not the TUN.
3. Persist profile in `onStartCommand` when `intent == null` (always-on).
4. Keep `teardown()` idempotent; it is called from three places.
