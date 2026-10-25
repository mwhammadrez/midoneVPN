# Three build recipes

Each recipe is end-to-end: gradle, native lib, service code, config, and the failure points.

---

## Recipe A — TUN + hev-socks5-tunnel + SOCKS5 (MIT, smallest surface)

**Use when:** learning, or you own a SOCKS5/SSH endpoint and want a lean client.

### Native build
```bash
mkdir -p app/src/main/jni/hev
cd app/src/main/jni/hev
git clone --recursive https://github.com/heiher/hev-socks5-tunnel jni
# app/src/main/jni/Android.mk  ->  include $(call all-subdir-makefiles)
ndk-build NDK_PROJECT_PATH=. APP_ABI=arm64-v8a,armeabi-v7a,x86_64 APP_PLATFORM=android-24
# .so files land in app/src/main/jniLibs/<abi>/
```
C API (verified from `src/hev-socks5-tunnel.h`):
```c
int  hev_socks5_tunnel_init(int tun_fd);
int  hev_socks5_tunnel_run(void);     // blocking
void hev_socks5_tunnel_stop(void);
void hev_socks5_tunnel_fini(void);
void hev_socks5_tunnel_stats(size_t*, size_t*, size_t*, size_t*);
```

### Kotlin service sketch
```kotlin
private fun startTunnel() {
    val pfd = Builder()
        .setSession("MyVPN").setMtu(1400).setBlocking(true)
        .addAddress("198.18.0.1", 15)          // benchmarking range, no LAN collision
        .addRoute("0.0.0.0", 0)
        .addDnsServer("1.1.1.1")
        .addRoute("1.1.1.1", 32)
        .addDisallowedApplication(packageName) // don't tunnel yourself
        .establish() ?: return stopSelf()

    writeConfigYaml()                          // see below
    val fd = pfd.fd
    thread(name = "hev") {
        Native.init(fd)                        // hev_socks5_tunnel_init
        Native.run()                           // blocking until stop()
        Native.fini()
    }
}
```

### Config the native side reads
```yaml
tunnel: { name: tun0, mtu: 1400, ipv4: 198.18.0.1, ipv6: 'fc00::1', icmp: reply }
socks5: { port: 1080, address: 127.0.0.1, udp: udp, username: u, password: p }
# mapdns: { address: 198.18.0.2, port: 53, network: 100.64.0.0, netmask: 255.192.0.0 }
```

**Gotchas:** `init()` must receive a valid fd from `establish()`; `run()` blocks — never on main
thread; `stop()` alone does not unblock a pending read — close the pfd too.

---

## Recipe B — libXray (VLESS/Reality/VMess/Trojan) + hev tun2socks

**Use when:** the server runs Xray (VLESS+Reality, XHTTP, gRPC, WS). This is the Iran-market standard.

### Getting the AAR
Download `libxray-android.zip` from `XTLS/libXray` releases (v26.10.21 → ~52 MB), extract the `.aar`
into `app/libs/`, then:
```kotlin
implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))
```

### Flow
```
vless:// link --convertShareLinksToXrayJson--> xrayJson
                          |
             LibXray.invoke("runXray")  -> SOCKS inbound on 127.0.0.1:10808
                          |
   VpnService TUN --hev-socks5-tunnel--> 127.0.0.1:10808 --> VLESS/Reality --> server
```

### Code
```kotlin
// 1. once per process
LibXray.setDNS(controller, "1.1.1.1:53")   // Android loopback-resolver fix, MUST be before runXray
// 2. go
LibXray.invoke("""{"apiVersion":3,"method":"runXray",
  "payload":{"xrayJson":${jsonQuote(xrayJson)}}}""")
// 3. on stop
LibXray.invoke("""{"apiVersion":3,"method":"stopXray","payload":{}}""")
LibXray.resetDNS()
```
Request/response envelope is JSON, max 16 MiB, `apiVersion` fixed at **3** (v26.x). Response:
`{"success":bool,"data":{},"error":""}`.

Controller callbacks you must wire:
- **Socket protect** — the Go runtime opens sockets; give it `VpnService.protect()` or the tunnel loops.
- **Process finder** (per-app routing) — API 30+ `ConnectivityManager.getConnectionOwnerUid()`,
  fallback parses `/proc/net/tcp{,6}`.
- **`SetDNS` / `ResetDNS`** — see above; skipping this is the #1 cause of "Xray connects, apps can't resolve".

**Min Android API for the lib: 21.** Never link two gomobile-built Go libs in one process (single Go
runtime per process) — if you also need, say, a Go VPN core, merge them in one `gomobile bind`.

---

## Recipe C — WireGuard / AmneziaWG (Apache-2.0, best battery)

```kotlin
implementation("com.wireguard.android:tunnel:1.0.20230706")   // latest on Maven Central (2023)
```
The library ships `GoBackend` which **is** a `VpnService`; you either reuse it (simplest) or copy its
`Builder` setup into your own service:
```kotlin
val config = Config.parse(inputStream)            // .conf text
val tunnel = manager.create(config)
manager.setStateInternal(tunnel, Tunnel.State.UP, null)
```
WireGuard handshake is stateless: after a network change the tunnel recovers on the next packet,
which is why it survives handover better than TCP tunnels. Use **AmneziaWG** (wgtunnel/android is the
reference) if plain WG is fingerprinted/blocked: it adds junk packets and header obfuscation
(`Jc`, `Jmin`, `Jmax`, `S1`, `S2`, `H1..H4`) with an otherwise identical API.

Persist `Config` + keys in `EncryptedSharedPreferences`/Keystore. Never log private keys.

---

## Gradle settings that matter for all recipes

```kotlin
android {
  compileSdk = 36                 // Play requires targetSdk >= 36 (2026-08-31+)
  defaultConfig {
    minSdk = 24
    targetSdk = 36
    ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64") } // drop x86 for Play
  }
  buildFeatures { compose = true }
  packaging { jniLibs { useLegacyPackaging = false } }
  splits { abi { isEnable = true; reset(); include("arm64-v8a", "armeabi-v7a", "x86_64") } }
}
```
Ship an **AAB** (Play generates per-ABI splits); the universal APK with a 52 MB AAR + native libs
is ~35–60 MB and users will notice.
