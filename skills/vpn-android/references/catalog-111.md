# Catalog — 111 verified repos (sweep 2026-10-24)

Method: 30 GitHub search queries, 40 results each, all metadata pulled live from the API on
2026-10-24 (stars, last push, licence, archived flag). 860 unique repos seen; 773 survived the
relevance filter; this catalog keeps the ones worth a decision.

### What the sweep says about the ecosystem

- **Freshness is good:** 433 of 773 relevant repos were pushed within 90 days; 204 are stale (>1y).
- **Licensing is the real filter:** 214 relevant repos have **no licence at all**, 183 are GPL-3.0,
  196 MIT, 45 Apache-2.0. For a closed commercial app the usable pool is ~250 repos, not 773.
- **The market converged on two stacks:** Xray/VLESS (Iran/China) and sing-box (everywhere else).
  WireGuard is the third, and it only survives in censored networks with AmneziaWG-style obfuscation.
- **Nobody writes their own protocol.** Every serious client is a UI + tun2socks + an embedded core.

## 1. Shortlist — build with these

Twelve repos that can carry a product. Everything else here is context.

| Repo | ★ | last push | license | verdict |
|---|---:|---|---|---|
| `XTLS/libXray` | 233 | 2026-10-23 | MIT | MIT Xray wrapper; `Invoke(json)`, protect + DNS controllers |
| `XTLS/Xray-core` | 42,291 | 2026-10-23 | MPL-2.0 | the engine itself; MPL-2.0; v26.10.21 |
| `heiher/hev-socks5-tunnel` | 2,081 | 2026-10-24 | MIT | MIT tun2socks; TCP+UDP; v2.19.1 |
| `heiher/sockstun` | 742 | 2026-08-01 | MIT | complete MIT Android app on hev — best small reference |
| `WireGuard/wireguard-android` | 1,653 | 2026-06-29 | Apache-2.0 | official; Apache-2.0; `tunnel:1.0.20230706` |
| `wgtunnel/android` | 3,297 | 2026-10-24 | MIT | WG + AmneziaWG 2.0–3.1, lockdown mode, local proxy |
| `MHSanaei/3x-ui` | 48,500 | 2026-10-24 | GPL-3.0 | the panel this project's server already runs |
| `tun2proxy/tun2proxy` | 1,442 | 2026-10-24 | MIT | Rust TUN→SOCKS/HTTP; great for desktop/sidecar tests |
| `Loyalsoldier/v2ray-rules-dat` | 20,933 | 2026-10-23 | GPL-3.0 | geoip/geosite replacement; routing correctness |
| `ViRb3/wgcf` | 8,836 | 2026-10-20 | MIT | throwaway WARP configs for testing the client |
| `passteque/gluetun` | 15,869 | 2026-10-21 | MIT | commercial VPNs in Docker — perfect test rig |
| `XTLS/REALITY` | 5,559 | 2026-10-14 | MPL-2.0 | the protocol repo; read before debugging a handshake |

## 2. Android clients — study, don't copy

GPL/AGPL code cannot go into a closed app. Read the architecture, rewrite the parts you need.

| Repo | ★ | last push | license | verdict |
|---|---:|---|---|---|
| `2dust/v2rayNG` | 64,596 | 2026-10-24 | GPL-3.0 | most complete Android Xray client; GPL-3.0 |
| `MatsuriDayo/NekoBoxForAndroid` | 23,255 | 2026-02-09 | NOASSERTION | multi-core; slowing (last push 2026-02) |
| `hiddify/hiddify-app` | 33,408 | 2026-10-23 | NOASSERTION | Flutter + multi-core; best UX reference |
| `chen08209/FlClash` | 57,831 | 2026-10-24 | GPL-3.0 | Flutter + ClashMeta; 57k stars |
| `KaringX/karing` | 15,709 | 2026-10-23 | NOASSERTION | sing-box based, modern UI |
| `shadowsocks/shadowsocks-android` | 36,817 | 2026-08-09 | NOASSERTION | the original SS client; Go core |
| `SaeedDev94/Xray` | 522 | 2026-10-21 | MIT | small Kotlin Xray client — easiest to read |
| `meow-rs/meow-android` | 153 | 2026-10-21 | MIT | Flutter Clash/mihomo client |
| `amnezia-vpn/amnezia-client` | 15,536 | 2026-10-24 | GPL-3.0 | desktop+mobile, many protocols |
| `amnezia-vpn/amneziawg-android` | 574 | 2026-09-21 | Apache-2.0 | the AmneziaWG Android library |
| `ProtonVPN/android-app` | 4,203 | 2026-10-22 | GPL-3.0 | a real product's architecture |
| `tailscale/tailscale-android` | 2,725 | 2026-10-23 | BSD-3-Clause | mesh VPN; clean VpnService usage |
| `OutlineFoundation/outline-apps` | 9,268 | 2026-10-24 | Apache-2.0 | Shadowsocks-based, Apache-2.0, clean |
| `celzero/rethink-app` | 5,757 | 2026-10-23 | Apache-2.0 | DNS-first VPN/firewall; Apache-2.0 |
| `blokadaorg/blokada` | 3,267 | 2026-10-24 | MPL-2.0 | blocker built on VpnService; MPL-2.0 |
| `Mygod/VPNHotspot` | 6,619 | 2026-09-04 | Apache-2.0 | tethering + VPN; niche but instructive |
| `yuhaiin/yuhaiin` | 170 | 2026-10-24 | MIT | Rust core, cross-platform, MIT |
| `schwabe/ics-openvpn` | 4,021 | 2026-10-14 | none | OpenVPN for Android; the reference for a non-proxy VPN |
| `vernesong/OpenClash` | 27,912 | 2026-10-24 | MIT | router-side client (OpenWrt); route-rule patterns |
| `juewuy/ShellCrash` | 13,618 | 2026-10-24 | GPL-3.0 | shell client; headless deployment |
| `getlantern/lantern` | 16,496 | 2026-10-24 | GPL-3.0 | different model: auto-finding reachable servers |

## 3. Cores and protocol engines

You embed one of these; you do not write your own crypto stack.

| Repo | ★ | last push | license | verdict |
|---|---:|---|---|---|
| `v2fly/v2ray-core` | 34,707 | 2026-10-20 | MIT | legacy VMess core; MIT |
| `SagerNet/sing-box` | 39,191 | 2026-10-24 | NOASSERTION | universal; GPL-3.0 **+ no-affiliation clause** |
| `HyNetworks/hysteria` | 22,721 | 2026-10-21 | MIT | QUIC-based; excellent on lossy mobile links |
| `netchx/netch` | 17,691 | 2026-10-22 | GPL-3.0 | Windows/.NET client, good TUN reference |
| `v2rayA/v2rayA` | 15,741 | 2026-10-21 | AGPL-3.0 | web UI over Xray; good config-model reading |
| `go-gost/gost` | 7,697 | 2026-10-21 | MIT | Go tunnel/proxy swiss-army knife; MIT |
| `nadoo/glider` | 3,706 | 2026-10-09 | GPL-3.0 | forward proxy, many protocols; GPL-3.0 |
| `snail007/goproxy` | 17,146 | 2026-08-17 | GPL-3.0 | mature Go proxy suite; GPL-3.0 |
| `3proxy/3proxy` | 5,546 | 2026-10-24 | NOASSERTION | tiny C proxy server; battle-tested |
| `rofl0r/microsocks` | 2,222 | 2026-09-24 | NOASSERTION | minimal SOCKS5 server, ~1000 lines |
| `MatsuriDayo/nekoray` | 15,284 | 2024-12-12 | GPL-3.0 | desktop sing-box GUI; last push 2024-12 |
| `2dust/v2rayN` | 119,004 | 2026-10-21 | GPL-3.0 | the desktop counterpart of v2rayNG; Windows/Linux/macOS |
| `throneproj/Throne` | 7,575 | 2026-10-24 | GPL-3.0 | sing-box GUI, cross-platform |
| `GUI-for-Cores/GUI.for.SingBox` | 8,205 | 2026-10-24 | GPL-3.0 | Wails/Go desktop GUI |

## 4. tun2socks / packet pumps

This is the layer that makes a TUN fd usable. Choose by platform, not by stars.

| Repo | ★ | last push | license | verdict |
|---|---:|---|---|---|
| `xjasonlyu/tun2socks` | 5,552 | 2026-10-11 | MIT | Go + gVisor stack; server/desktop |

## 5. WireGuard family

Smallest code path to a working tunnel; weakest against fingerprinting without AmneziaWG.

| Repo | ★ | last push | license | verdict |
|---|---:|---|---|---|
| `windtf/wireproxy` | 5,853 | 2026-10-12 | ISC | WG → SOCKS5/HTTP; ISC licence |
| `mullvad/mullvadvpn-app` | 7,703 | 2026-10-24 | GPL-3.0 | privacy-first product; GPL-3.0 |
| `celzero/firestack` | 178 | 2026-10-23 | MPL-2.0 | userspace wireguard + netmon; MPL-2.0 |
| `hwdsl2/wireguard-install` | 3,125 | 2026-10-24 | MIT | server installer; MIT |
| `bivlked/amneziawg-installer` | 1,407 | 2026-10-22 | MIT | AmneziaWG server installer |

## 6. Panels and one-click servers

What the server side of this project actually looks like.

| Repo | ★ | last push | license | verdict |
|---|---:|---|---|---|
| `Incognito-Coder/tx-ui` | 491 | 2026-10-23 | GPL-3.0 | 3x-ui fork, multi-protocol |
| `AlexeyLCP/lucx-ui` | 272 | 2026-10-24 | GPL-3.0 | 3x-ui fork with native AmneziaWG |
| `Gozargah/Marzban` | 7,474 | 2026-10-14 | AGPL-3.0 | clean API; good for automation |
| `marzneshin/marzneshin` | 702 | 2025-10-04 | AGPL-3.0 | Marzban fork for scale; last push 2025-10 |
| `rebeccapanel/Rebecca` | 419 | 2026-10-24 | AGPL-3.0 | Marzban fork, active |
| `hiddify/Hiddify-Manager` | 9,402 | 2026-10-23 | GPL-3.0 | one-click multi-user |
| `alireza0/s-ui` | 10,128 | 2026-10-22 | GPL-3.0 | sing-box panel |
| `mack-a/v2ray-agent` | 22,155 | 2026-10-20 | AGPL-3.0 | one-key multi-protocol script |
| `yonggekkk/sing-box-yg` | 10,689 | 2026-10-22 | GPL-3.0 | 5-in-1 script incl. Argo tunnels |
| `bia-pain-bache/BPB-Worker-Panel` | 13,802 | 2026-07-20 | GPL-3.0 | Cloudflare-Workers panel |
| `primeZdev/whale-panel` | 201 | 2026-10-15 | MIT | admin dashboard over X-UI |
| `HamedAp/ShahanPanel` | 1,203 | 2026-10-24 | MIT | panel with online users |
| `yukh975/3X-UI-Manager` | 88 | 2026-10-22 | MIT | native Android manager for 3x-ui |
| `kutovoys/xray-checker` | 930 | 2026-09-10 | AGPL-3.0 | Prometheus exporter for Xray health |
| `remnawave/panel` | 5,247 | 2026-10-22 | AGPL-3.0 | modern Xray panel; clean API (AGPL) |
| `IRNova/Nova-Proxy` | 3,418 | 2026-10-21 | NOASSERTION | Persian-language Cloudflare-Workers panel |
| `fscarmen/sing-box` | 5,870 | 2026-10-22 | GPL-3.0 | one-key script: Reality + Hysteria2 + TUIC |
| `233boy/sing-box` | 4,999 | 2026-06-04 | GPL-3.0 | widely used one-key sing-box script |
| `wulabing/Xray_onekey` | 9,251 | 2026-08-15 | MIT | the classic VLESS+XTLS installer |
| `yonggekkk/argosbx` | 5,922 | 2026-10-23 | GPL-3.0 | multi-core one-key script with Argo |
| `XTLS/Xray-install` | 3,634 | 2026-10-23 | GPL-3.0 | official install/upgrade script |
| `hwdsl2/docker-ipsec-vpn-server` | 7,151 | 2026-10-24 | NOASSERTION | IPsec/L2TP server in Docker |
| `eooce/node-ws` | 6,241 | 2026-10-11 | GPL-3.0 | serverless VLESS/Trojan/SS/VMess on Workers |
| `cmliu/WorkerVless2sub` | 6,295 | 2026-09-11 | Apache-2.0 | Worker that generates optimised subscriptions |
| `IAmStoxe/wirehole` | 4,979 | 2026-08-25 | NOASSERTION | WireGuard + Pi-hole in one stack |

## 7. DNS, firewall, privacy layer

DNS is the #1 failure class in the field data; these are the reference implementations.

| Repo | ★ | last push | license | verdict |
|---|---:|---|---|---|
| `AdAway/AdAway` | 9,537 | 2026-09-13 | GPL-3.0 | hosts-based blocker; GPL-3.0 |
| `x-falcon/Virtual-Hosts` | 1,663 | 2026-01-06 | GPL-3.0 | hosts via VpnService; good DNS interception code |
| `pass-with-high-score/blockads-android` | 2,292 | 2026-10-22 | GPL-3.0 | modern local-VPN blocker |
| `WhiteDNS/WhiteDNS-Android` | 1,429 | 2026-10-23 | NOASSERTION | DNS-tunnelling client (source-available) |
| `anonvector/SlipNet` | 1,516 | 2026-08-21 | AGPL-3.0 | DNS tunnelling (DNSTT/NoizDNS) |
| `AhmetCanArslan/ShizuWall` | 2,395 | 2026-10-24 | GPL-3.0 | Shizuku-based firewall, no VPN slot |
| `Kin69/Athena` | 726 | 2026-01-26 | GPL-3.0 | Material 3 firewall/adblock |
| `privacy-protection-tools/anti-AD` | 10,883 | 2026-10-24 | MIT | large blocklist, MIT |
| `E2Kv5/amneziaguard` | 25 | 2026-08-12 | NOASSERTION | AmneziaWG + per-app firewall |

## 8. Other VpnService apps worth reading

Smaller projects that solve one hard thing well (per-app routing, DPI bypass, SNI tricks).

| Repo | ★ | last push | license | verdict |
|---|---:|---|---|---|
| `mbm110/MSN-GUARD` | 688 | 2026-10-24 | AGPL-3.0 | WG + MASQUE + WARP-on-WARP |
| `CluvexStudio/ZedSecure` | 476 | 2026-10-23 | AGPL-3.0 | multi-core (Xray, sing-box, Psiphon, Tor) |
| `ArasTey/ArasClient` | 274 | 2026-10-22 | GPL-3.0 | modern Android client; KMP-ish |
| `Leadaxe/LxBox` | 656 | 2026-10-24 | GPL-3.0 | VLESS/REALITY/XHTTP/WARP client |
| `throneproj/ThroneForAndroid` | 637 | 2026-10-24 | GPL-3.0 | sing-box core; cross-platform GUI |
| `IEAmir/NetValve` | 11 | 2026-07-25 | NOASSERTION | root-free per-app traffic controller |
| `popeye-mk/Noxa` | 8 | 2026-10-20 | NOASSERTION | minimal VpnService privacy blocker |
| `tokyoxpa3/5G-Proxy-Client` | 12 | 2026-10-10 | MIT | SOCKS5 TUN tunnel client |
| `Enki013/Netrix` | 22 | 2026-07-02 | GPL-3.0 | DPI bypass for Android |
| `umutxyp/Shroudly` | 245 | 2026-10-24 | MIT | cross-platform DPI bypass |

## 8b. Documentation and data you will actually open

Reference material that saves days.

| Repo | ★ | last push | license | verdict |
|---|---:|---|---|---|
| `XTLS/Xray-examples` | 3,969 | 2026-09-16 | CC-BY-SA-4.0 | official config examples for every transport |
| `pirate/wireguard-docs` | 5,058 | 2026-03-21 | MIT | the unofficial WireGuard book; best WG reference |
| `Loyalsoldier/geoip` | 6,635 | 2026-10-23 | CC-BY-SA-4.0 | Mainland-China GeoIP, multiple formats |

## 9. Traps — recognise and avoid

Not bad software; bad choices for this project.

| Repo | ★ | last push | license | verdict |
|---|---:|---|---|---|
| `hexene/LocalVPN` | 966 | 2019-02-27 | Apache-2.0 | Apache-2.0 but dead since 2019 — read, never ship |
| `ambrop72/badvpn` | 1,931 | 2021-08-22 | NOASSERTION | dead since 2021; do not adopt the tun2socks |
| `DiningFactory/panda-vpn-pro` | 16,795 | 2026-10-20 | Apache-2.0 | SEO spam, not software |
| `Pawdroid/Free-servers` | 19,613 | 2026-10-24 | none | free-node feed; unvetted, privacy risk |
| `ermaozi/get_subscribe` | 9,382 | 2026-10-24 | GPL-3.0 | free-node feed; unvetted |
| `Jsnzkpg/Jsnzkpg` | 4,289 | 2026-10-20 | none | free-node feed; unvetted |
| `ermaozi01/free_clash_vpn` | 815 | 2026-10-24 | GPL-3.0 | free-node feed; unvetted |
| `Qv2ray/Qv2ray` | 16,875 | 2025-10-27 | GPL-3.0 | abandoned 2025-10; do not start from it |
| `peasoft/NoMoreWalls` | 3,563 | 2026-10-24 | NOASSERTION | aggregated free nodes; unvetted, privacy risk |
| `free18/v2ray` | 5,742 | 2026-10-24 | none | free-node feed; unvetted |
| `limbopro/Paolujichang` | 7,612 | 2026-09-14 | none | a public list of providers that ran off — read before trusting cheap airports |

## 10. How to use this

- Pick **one** core (Xray via libXray *or* sing-box *or* WireGuard), one pump (hev), one panel (3x-ui).
- Anything GPL/AGPL: read, learn, reimplement. Anything with no licence: read the *ideas* only.
- Re-run this sweep before pinning versions — the top of this list changes monthly.