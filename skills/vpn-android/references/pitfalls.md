# Failure modes — symptom → cause → fix

Ordered by how often they show up in real projects. Use as a code-review and QA checklist.

## A. "It connects, but nothing loads"

| # | Symptom | Cause | Fix |
|---|---|---|---|
| 1 | VPN icon on, all traffic dead | Server socket not `protect()`ed → packets to the server re-enter the tunnel (loop) | protect every core socket; with libXray use its protect controller |
| 2 | Same | No `addRoute("0.0.0.0", 0)` (or only a subnet) | add full route + `::/0`, or accept split-tunnel by design |
| 3 | Same | tun2socks not running / pointed at wrong port | verify local SOCKS listening (`127.0.0.1:10808`), check core log |
| 4 | Small pages load, big ones hang | MTU 1500 + TLS/VLESS overhead → fragmentation black hole | `setMtu(1400)` (1380–1420 range), or MSS clamp |
| 5 | First request OK then stall | Core stopped after error but UI still says connected | propagate core state to UI; watchdog on `stats()` counters |

## B. Dead tunnel after a while / after moving

| # | Symptom | Cause | Fix |
|---|---|---|---|
| 6 | Dies on Wi-Fi↔LTE switch | TCP session to the server is gone; pfd still open | `setUnderlyingNetworks()`, listen to `ConnectivityManager` and rebuild the tunnel |
| 7 | Dies when screen off | Doze + background restrictions kill the process | FGS of correct type, `startForeground` early, no long wakelocks |
| 8 | Dies only on Xiaomi/Huawei/Oppo | OEM task killers | onboarding: enable Autostart + battery "No restrictions"; `WorkManager` re-arm |
| 9 | `establish()` returns null | Not prepared, or another VPN app took the slot, or user revoked | re-run `prepare()` each attempt; surface a clear error |
| 10 | Reconnect loop / battery drain | `onRevoke()` handler immediately reconnects | mark "revoked by system", require user action or exponential backoff |
| 11 | Breaks after reboot with always-on | Service started with null intent, code expected extras | persist profile; treat unflagged start as system start |

## C. DNS

| # | Symptom | Cause | Fix |
|---|---|---|---|
| 12 | Ping by IP works, names don't | no `addDnsServer` or DNS not routed | add DNS + route it; or fake-DNS in the core |
| 13 | Names resolve to the wrong country / leaks | system resolver used outside the tunnel | `addDnsServer` inside; block DoT/DoH bypass or document it |
| 14 | libXray: Xray up but resolution fails | Android loopback resolver captured by VPN | `LibXray.setDNS(controller,"1.1.1.1:53")` **before** `runXray`, `resetDNS()` after |
| 15 | Some apps can't resolve `.local`/LAN | DNS forced through tunnel | `excludeRoute` for LAN ranges, or split DNS by domain suffix |

## D. Leaks and security

| # | Symptom | Cause | Fix |
|---|---|---|---|
| 16 | IPv6 traffic bypasses the tunnel | only IPv4 routed | route `::/0` + `allowFamily(AF_INET6)`, or explicitly disable IPv6 in the TUN |
| 17 | Leak test shows real IP on some sites | split tunnel / disallowed app | review allow/disallow lists; verify with a leak-test page |
| 18 | Private keys in logcat | logging config verbatim | redact; store keys in Keystore/EncryptedSharedPreferences |
| 19 | Cleartext blocked in release | `networkSecurityConfig` / target 28+ cleartext default off | HTTPS only; localhost is exempt |

## E. Build / packaging / crash-at-start

| # | Symptom | Cause | Fix |
|---|---|---|---|
| 20 | `MissingForegroundServiceTypeException` (API 34+) | no `foregroundServiceType` | `specialUse` + permission + `startForeground(id,n,FOREGROUND_SERVICE_TYPE_SPECIAL_USE)` |
| 21 | `ForegroundServiceStartNotAllowedException` | starting FGS from background/receiver | start from an Activity, or rely on system always-on start |
| 22 | Crash on load, `duplicate symbol` / Go runtime panic | two gomobile libs in one process | one Go runtime per process; merge Go packages into one bind |
| 23 | `UnsatisfiedLinkError` on some phones | missing ABI | include `armeabi-v7a` + `arm64-v8a` (+`x86_64` for emulators) |
| 24 | Notification missing → service killed | channel not created (API 26+) | create channel before `startForeground` |
| 25 | libXray `Invoke` returns failure | wrong `apiVersion` (must be 3) or oversized payload (>16 MiB) | check envelope; don't inline huge routing rules |
| 26 | App works in debug, breaks in release | R8 stripping JNI/reflection | keep rules for JNI classes, `-keep class org.nekobox...` equivalents |

## F. Play submission

| # | Rejection | Fix |
|---|---|---|
| 27 | "Use of VpnService not documented" | submit the VPN declaration form; state VPN is core functionality |
| 28 | Target API too low | `targetSdk ≥ 36` |
| 29 | `specialUse` not justified | explain in the declaration; keep the property metadata |
| 30 | Data-safety mismatch | declare exactly what the tunnel logs; usually "no collection" |
| 31 | No privacy policy / no way to delete data | publish policy + in-app link |

## Instant triage (do this first)

```bash
adb logcat -s VpnService:* MyApp:* | grep -iE "establish|protect|revoke|exception"
adb shell dumpsys connectivity | grep -i vpn          # is a VPN actually active?
adb shell ip addr show tun0                           # interface + MTU
# then: does a raw IP work? (ping 1.1.1.1) -> isolates DNS from transport
```
