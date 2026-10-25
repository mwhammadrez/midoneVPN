# Landscape — verified 2026-10-24

All numbers pulled live from the GitHub API on 2026-10-24. Re-verify before pinning versions;
this ecosystem moves weekly.

## Cores and building blocks (pick from here)

| Repo | Stars | Last push | License | What it is | Verdict |
|---|---|---|---|---|---|
| `XTLS/libXray` | 233 | 2026-10-23 | **MIT** | Xray-core wrapped for Android/iOS/Linux/Windows via gomobile; single `Invoke(json)` API | **Best base for a VLESS/Reality client.** MIT = closed-source OK |
| `XTLS/Xray-core` | — | v26.10.21 | MPL-2.0 | the actual proxy engine | the core behind libXray |
| `heiher/hev-socks5-tunnel` | 2081 | 2026-10-23 | **MIT** | C tun2socks (TCP+UDP, ICMP reply, dual-stack), `ndk-build`, tiny | **Default tun2socks.** 2.19.1 |
| `SagerNet/sing-box` | 39191 | 2026-10-24 | **GPL-3.0 + no-affiliation clause** | universal proxy platform, `libbox` gomobile | Great tech, **license trap** for commercial apps |
| `WireGuard/wireguard-android` | 1653 | 2026-06-29 | Apache-2.0 | official WG client (mirror) | with `com.wireguard.android:tunnel:1.0.20230706` |
| `wgtunnel/android` | 3297 | 2026-10-24 | MIT | WG + **AmneziaWG 2.0–3.1**, lockdown mode, local SOCKS/HTTP proxy | best modern WG client to study |
| `2dust/v2rayNG` | 64596 | 2026-10-24 | GPL-3.0 | reference VLESS/VMess client, `compileSdk 37`, local `libs/*.aar` | read it, don't copy it (GPL) |
| `hiddify/hiddify-app` | 33408 | 2026-10-23 | NOASSERTION | Flutter multi-core client | study UX, not code |
| `MatsuriDayo/NekoBoxForAndroid` | 23254 | 2026-02-09 | NOASSERTION | multi-core client | slowing down |
| `heiher/sockstun` | 742 | 2026-08-01 | MIT | complete minimal Android app on hev-socks5-tunnel | **best small reference for Recipe A** |
| `xjasonlyu/tun2socks` | 5552 | 2026-10-11 | MIT | Go + gVisor tun2socks | server/desktop, not for Android app |
| `tun2proxy/tun2proxy` | 1442 | 2026-10-24 | MIT | Rust TUN→SOCKS/HTTP | desktop/sidecar |
| `hexene/LocalVPN` | 966 | 2019-02-27 | Apache-2.0 | VpnService packet interceptor, no real transport | **learning only**, dead |
| `ambrop72/badvpn` | 1931 | 2021-08-22 | NOASSERTION | classic badvpn tun2socks | dead, do not use |

## Server / panel side

| Repo | Stars | License | Notes |
|---|---|---|---|
| `MHSanaei/3x-ui` | 48500 | GPL-3.0 | what most Iranian-market servers run (VLESS/Reality/XHTTP/WS, subscription links) |
| `hiddify/Hiddify-Manager` | 9401 | GPL-3.0 | multi-user, one-click, auto CDN |
| `Gozargah/Marzban` | 7474 | AGPL-3.0 | clean API, good for automation |
| `alireza0/s-ui` | 10127 | GPL-3.0 | sing-box panel |
| `mack-a/v2ray-agent` | 22155 | AGPL-3.0 | one-key multi-protocol script |

## Protocol reality check

| Protocol | Client cost in Kotlin | Detection resistance | Notes |
|---|---|---|---|
| VLESS + Reality | needs libXray (Go core) | high (borrows real TLS sites) | current default for Iran; TCP/443 |
| VLESS + XHTTP | needs libXray | high, CDN-friendly | extra config surface, more failure modes |
| VMess/Trojan | needs core | medium | legacy-ish |
| WireGuard | native lib, small | low (fingerprintable) | best battery; needs obfuscation where blocked |
| AmneziaWG | native lib (wgtunnel) | medium-high | WG with junk packets / header tricks |
| Hysteria2 / TUIC | needs core (QUIC) | high on lossy mobile | great on bad MCI links, heavy battery |
| ShadowSocks | small, doable in Kotlin | low | still useful behind CDN |

## License traps (read before copying code)

- **sing-box** = GPL-3.0 **plus** "no derivative may use the name or imply association". Linking `libbox` forces your app to GPL.
- **v2rayNG / 3x-ui / Hiddify / Marzban** = GPL/AGPL. Copying code into a closed app is a violation.
- **libXray = MIT**, **hev-socks5-tunnel = MIT**, **WireGuard = Apache-2.0** → the clean commercial path.
- Xray-core itself is MPL-2.0 (file-level copyleft) — fine to *use* as a library, changes to its files must be published.
