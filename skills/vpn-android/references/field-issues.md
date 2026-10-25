# Field evidence — what actually breaks (mined from issue trackers, 2026-10-24)

Not theory. Counts come from GitHub issue search on each repo; titles are real issues.

## Where the pain concentrates

| Repo | Query | Matching issues |
|---|---|---|
| `2dust/v2rayNG` | `dns` | **514** |
| `2dust/v2rayNG` | "can't browse / 无法上网" | **228** |
| `SagerNet/sing-box` | `android tun` | **506** |
| `heiher/hev-socks5-tunnel` | all | **243** |

Translation: **DNS and "connects but no traffic" outnumber every other class by an order of
magnitude.** Security bugs are rare; dead tunnels are the product experience.

## Representative failures

| Repo | Issue (date) | Class | Lesson |
|---|---|---|---|
| v2rayNG | "same node works on PC, not on phone" (2022-01, 92 comments) | config/transport | desktop and mobile differ in MTU + TLS path; test on mobile data, not Wi-Fi |
| v2rayNG | "EMUI 10 can't start VPN" (2019-10, 48c) | OEM | OEM ROMs block VPN creation; need a clear, actionable error, not a spinner |
| v2rayNG | "after 1.8.12 some working nodes fail" (2024-01, 58c) | core/config regression | pin core versions; never auto-update the core without a fallback |
| v2rayNG | "remote DNS 1.1.1.1 unreachable → no browsing" (2023-02, 42c) | DNS | the resolver you hardcode may itself be blocked in-country; use the tunnel's DNS |
| v2rayNG | "ColorOS 15: connects, no proxy" (2025-03, 31c) | OEM/network | "connected" state must be validated by a real probe, not by service state |
| v2rayNG | "CPU abnormal after long run" (2025-06, 23c) | resource leak | long-run soak test is mandatory; stats watchdog catches it |
| v2rayNG | "don't expose socks5 port, protect TUN" (2026-04, 347c) | security/architecture | local SOCKS must be loopback-only and ideally protected; never `0.0.0.0` |
| sing-box | "too many open files" (2024-06, 29c) | connection leak | close sessions on idle; set max-session-count |
| sing-box | "[Android] WireGuard broken after network outage/recovery" (2024-01, 16c) | handover | this is *the* cross-network bug; requires a transport restart |
| sing-box | "WireGuard keepalive stops when screen off" (2025-09, **open**) | doze | keepalive alone does not survive Doze; needs re-arm strategy |
| sing-box | "IPv6 does not work" (2025-12, 15c) | IPv6 | dual-stack is where clients quietly break |
| hev-socks5-tunnel | "[bug] Android Crashed" (2025-05, 78c) | native crash | JNI lifetime: init/fini ordering and fd ownership must be exact |
| hev-socks5-tunnel | "Android VPN native crash" (2025-02, 36c) | native crash | never call run() twice; teardown must join the thread before fini() |

## The three takeaways

1. **DNS is the product.** If name resolution is not inside the tunnel and not resilient to the
   in-country blocking of public resolvers, users call the app broken.
2. **Handover + Doze are the second product.** A VPN that dies at the first Wi-Fi→LTE switch or after
   the screen turns off gets one-star reviews regardless of protocol quality.
3. **Native crashes dominate the core repos.** JNI teardown ordering (`stop` → `join` → `close fd` →
   `fini`) is worth more than any feature.

## How to reproduce these before shipping

- Throttle to 5% loss / 300 ms RTT and open 20 pages → exposes MTU and timeout bugs.
- Toggle airplane mode 20× while downloading → exposes handover bugs.
- Leave connected 8 h with screen off → exposes Doze keepalive bugs.
- Run `adb shell dumpsys meminfo <pkg>` before/after 2 h → exposes leaks.
- Point the profile at a wrong DNS and a wrong shortId → confirms the error surface is honest.

## Config generation: three traps that look like "the server is dead"

Found the hard way on MidONe (Xray 26.10.21, 2026-10). All three pass string-based unit tests and
all three reach the user as "it doesn't connect".

### 1. `geoip:private` / `geosite:*` — data files are not bundled
```json
{"type":"field","ip":["geoip:private"],"outboundTag":"direct"}
```
→ `illegal ip rule: geoip:private > failed to open geoip.dat > /system/bin/geoip.dat: no such file`
libXray (and most AAR wrappers) ship no `geoip.dat`. Xray aborts **while parsing**, before any socket.
Use explicit CIDRs (`10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`, `127.0.0.0/8`, `169.254.0.0/16`,
`::1/128`, `fc00::/7`, `fe80::/10`).

### 2. `direct` as the first outbound = silent, total leak
Xray routes unmatched traffic to `outbounds[0]`. With `[{direct},{proxy}]` every packet leaves
unproxied and the UI still shows Connected. Put the proxy outbound first **and** add an explicit
catch-all rule. Detect it with a negative control: point the client at a dead server — if the request
still succeeds, traffic is not going through the tunnel.

### 3. `allowInsecure` was removed in Xray 26
Replaced by `pinnedPeerCertSha256` (hex string, **not** an array) and `verifyPeerCertByName`.
Links with `allowInsecure=1` must surface a warning instead of failing with a config error.

## Verify, don't assume
`xray run -test -c config.json` from an official linux-64 release of the *same* version validates the
whole config in milliseconds. It catches trap 1 and 3, not trap 2 — that needs a live loopback server
plus a negative control. Emulator not required.

## Giving libXray the Android TUN fd (Xray 26.x)

Two silent killers, both verified on a real TUN device:

```json
"env": {"xray.tun.fd": "<fd>"}     // correct  (libXray removed SetTunFd)
"env": {"XRAY_TUN_FD": "<fd>"}     // ignored by the core: the tun inbound never attaches
```

```json
"settings": {"name": "tun0", ...}   // must equal the real interface name behind the fd
```
Mismatch → `proxy/tun: invalid xray.tun.fd: TUN device name tun9 does not match configured name tun0`.
Android interface names are not guaranteed to be `tun0`; diff `NetworkInterface.getNetworkInterfaces()`
around `VpnService.Builder.establish()` and pass the new name.

Test recipe that needs no phone: on a Linux box with CAP_NET_ADMIN, open `/dev/net/tun`, ioctl
`TUNSETIFF`, keep the fd inheritable, run `xray run -c cfg` with the fd inherited, add a route for one
public IP via that device for a *different uid* (`ip rule add uidrange`), then curl as that uid.
The server side needs `sockopt.mark` + a policy route back to eth0, otherwise the server's own answer
loops back into the tun and the test stalls forever.
