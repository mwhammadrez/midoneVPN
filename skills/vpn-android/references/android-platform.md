# Android platform mechanics

## 1. Manifest (the only part the system trusts)

```xml
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE"/>
<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>

<service android:name=".TunnelService"
         android:exported="true"
         android:permission="android.permission.BIND_VPN_SERVICE"
         android:foregroundServiceType="specialUse">
  <intent-filter><action android:name="android.net.VpnService"/></intent-filter>
  <property android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE" android:value="vpn"/>
  <!-- opt out of always-on: android:name="android.net.VpnService.SUPPORTS_ALWAYS_ON" value=false -->
</service>
```

There is **no dedicated VPN foreground-service type**. `specialUse` is the correct declaration;
Play asks you to justify it during review. Omitting the type when targeting API 34+ throws
`MissingForegroundServiceTypeException` at `startForeground()`.

## 2. Connect sequence (order matters)

1. `VpnService.prepare(context)` → `Intent?`. **Call every time.** Non-null = launch consent activity;
   `RESULT_OK` means you are the current prepared VPN app.
2. `startForegroundService(...)` → service calls `startForeground()` first thing.
3. Build/parse config, open the core's local SOCKS listener (Recipe B) or WG socket (Recipe C).
4. `protect(fd)` on every socket that talks to the server (or register a protect callback with the core).
5. `Builder` … `establish()` → `ParcelFileDescriptor` or `null` (not prepared / revoked).
6. Hand `pfd` to the pump (`hev_socks5_tunnel_init(fd)` + `run()` on a worker thread, or `libbox`/WG equivalent).
7. Publish state; keep the notification alive.

Teardown order is the reverse: stop pump → close pfd → stopForeground(STOP_FOREGROUND_REMOVE) → stopSelf.

## 3. Builder API — what each call is for

| Method | Purpose | Failure if missing |
|---|---|---|
| `addAddress(ip, prefix)` | address on the TUN interface | `establish()` throws / no traffic |
| `addRoute(net, prefix)` | what enters the tunnel (`0.0.0.0/0` + `::/0` = full) | traffic bypasses (leak) |
| `addDnsServer(ip)` | resolver handed to apps | DNS leaks / no resolution |
| `addSearchDomain` | local domains | minor |
| `allowFamily(AF_INET6)` | enables IPv6 on the TUN | IPv6 paths break |
| `excludeRoute(ip, prefix)` | **API 33+** carve-outs, e.g. LAN or the server IP | LAN/printers unreachable, server loop |
| `addAllowedApplication` / `addDisallowedApplication` | split tunnel (one list only, mutually exclusive) | — |
| `setBlocking(true)` | blocking reads on the fd | busy-loop / CPU burn |
| `setMtu(1400)` | MTU; TLS+VLESS overhead means do **not** ship 1500 blindly | black-hole on big packets, TCP stalls |
| `setSession(name)` | shown in Settings | cosmetic |
| `setUnderlyingNetworks(networks)` | tells the system which physical net carries the tunnel | broken handover Wi-Fi↔LTE |
| `setMetered(false)` | non-metered if the server is unmetered | OS may restrict background |
| `setHttpProxy` | proxy for apps that respect it | — |
| `allowBypass()` | apps may bind to other networks | — |

Rules of thumb:
- Per-app lists must be set **before** `establish()`; changing them requires a new connection.
- `excludeRoute` the VPN server's own IP (or `protect()` it) so the tunnel can reach the server when
  the server is inside the routed range (common with full-tunnel + Cloudflare/CDN fronting).
- No `addDnsServer` + full route = every app's DNS lookups die or leak. Always add at least one
  in-tunnel DNS (e.g. `1.1.1.1`, or the core's fake-DNS address).


## 3b. Private DNS and multi-profile reality

- **Private DNS (DoT, Android 9+).** If `Settings.Global.PRIVATE_DNS_MODE == "hostname"`, the system may
  resolve over TLS on the *underlying* network, outside your TUN. Your `addDnsServer` is not enough.
  Detect it and either route port 853 into the tunnel or tell the user once. Never claim "no leaks"
  without a probe.
- **Work profile.** A VPN is per user profile: a work profile can run its own VPN, and
  `addAllowedApplication`/`addDisallowedApplication` only see apps in the current profile. Test the app
  with a work profile present — "connects but Chrome doesn't" is often the other profile's VPN.
- **Multiple users / secondary profiles.** Each gets its own `VpnService` instance; do not assume a
  single global tunnel or a single set of prefs.
- **Captive portals.** Portal detection runs outside the VPN. If your app blocks everything before the
  tunnel is up (lockdown), the user cannot log into hotel Wi-Fi. Detect the portal and pause.

## 4. Lifecycle facts people get wrong

- **Always-on VPN** starts the service with a **null/empty intent** — never assume extras.
  Detect "system started me" by flagging your own start intents with an extra and treating unflagged
  starts as always-on/system.
- In always-on **lockdown** ("Block connections without VPN"), non-VPN traffic is dropped. Your app
  must not show a Disconnect button then, and must retry forever instead of giving up.
- `onRevoke()` is **not guaranteed on the main thread**; do not touch UI directly, and close the fd
  there (draining is unnecessary).
- Only **one** VPN per user profile. Starting a second service silently stops the first.
- The process can be killed at any time; state belongs in persistent storage (DataStore/MMKV), not in
  the service object.
- `POST_NOTIFICATIONS` (API 33+) is separate from VPN consent; ask for it before `startForeground`,
  and keep working if denied (FGS still runs, notification is hidden).

## 5. Version requirements (as of 2026-10)

| Concern | Requirement |
|---|---|
| Play submission (new app **or** update) | **targetSdk ≥ 36** (Android 16), since 2026-08-31 |
| Current stable platform | **API 37** (Android 17, stable 2026-06-16) |
| Current platform SDK | 37 (Android 17) — v2rayNG already builds against it |
| FGS type | required from API 34 (`specialUse` for VPN) |
| 64-bit | Play requires arm64-v8a; ship per-ABI splits (AAB handles it) |
| Edge-to-edge | enforced from API 35 — handle insets or the UI underlaps system bars |
| AAB | required for Play; APK only for sideload/F-Droid |

## 6. Play policy for VPN apps (support.google.com/googleplay/android-developer/answer/12564964)

- **Every** app using `VpnService` must submit the VPN declaration form in Play Console.
- The VPN must be **core functionality** of the app; a VPN bolted onto an unrelated app is rejected.
- Traffic must be **encrypted** end to end (this rules out plain SOCKS5-as-the-product).
- No collection of personal/sensitive data through the tunnel without prominent disclosure and consent;
  no ad-fraud/redirect use. Privacy policy required.
- `specialUse` FGS requires a written justification in the declaration.
- Expect manual review; a demo video and clear description speed it up.

## 6b. Android 16 / 17 (API 36 / 37) — what changed for VPN apps

API 37 (Android 17) went stable **2026-06-16**. Play requires **targetSdk ≥ 36** since 2026-08-31;
expect 37 to be required around 2027-08.

| Change (targeting 36+) | Effect on a VPN app | What to do |
|---|---|---|
| **Edge-to-edge opt-out removed** (`windowOptOutEdgeToEdgeEnforcement` ignored) | UI underlaps system bars | handle insets in Compose (`WindowInsets.safeDrawing`) |
| **Predictive back enforced** — `onBackPressed()` is no longer called | custom back handling silently dies | use `OnBackInvokedCallback` / Navigation, or set `android:enableOnBackInvokedCallback="false"` |
| **`scheduleAtFixedRate` replays at most one missed run** | a reconnect timer that missed ticks now fires once | use `WorkManager` / explicit coroutine loop for backoff, not fixed-rate |
| **Orientation/resizability ignored on sw ≥ 600dp** | tablet/desktop layouts stretch | build adaptive layouts; don't assume portrait lock |
| **Local Network Permission (LNP)** — LAN ranges (`10/8`, `172.16/12`, `192.168/16`, `100.64/10`, `169.254/16`, multicast) gated by a new runtime permission | split-tunnel users lose printers/NAS/Cast; raw sockets to LAN fail with `EPERM` | keep LAN out of the tunnel and document the permission; LAN DNS on port 53 is exempt, and **VPN interfaces are not treated as LAN** |
| Safer Intents (opt-in via `intentMatchingFlags`) | stricter intent resolution when enabled | only opt in once your filters are exact |

LNP is opt-in during Android 16 and slated for enforcement later — design for it now if you plan to
ship a split-tunnel mode.

## 7. Doze, OEMs, battery

- Request battery-optimization exemption only if you have a real always-on story; Play dislikes it otherwise.
- Xiaomi/Huawei/Oppo/Vivo kill background services aggressively: document "Autostart" + "No restrictions"
  in onboarding, and re-arm via `WorkManager` heartbeat rather than a raw AlarmManager loop.
- Never hold a wakelock for the whole session; the pump is I/O-bound and sleeps in `read()`.
- A UDP/QUIC transport (Hysteria2/TUIC) costs noticeably more battery than TCP/VLESS behind a CDN.
