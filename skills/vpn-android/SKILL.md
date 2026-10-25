---
name: vpn-android
description: Use when building, debugging, or reviewing an Android VPN / proxy client (VpnService, tun2socks, VLESS-Xray, WireGuard, AmneziaWG), when choosing the client stack or its license, or when provisioning the matching server. Covers architecture recipes, licensing traps, API-36 Play requirements, and the failure modes that make VPN apps look "buggy".
---

**خلاصه:** این skill مسیر ساخت یک اپ VPN اندرویدی حرفه‌ای را از پایه تا انتشار می‌دهد: چهار لایه‌ی نرم‌افزار، انتخاب هسته (tun2socks / libXray / WireGuard)، تله‌های لایسنس، الزامات API 36 و Play، و لیست باگ‌هایی که ۹۰٪ اپ‌های VPN را «خراب» نشان می‌دهد.

# Android VPN client — engineering skill

## When to use

- Building an Android app that routes device traffic (VPN, proxy client, ad-blocker, firewall).
- Choosing between tun2socks / Xray / WireGuard / sing-box cores.
- Debugging "connects but no internet", DNS leaks, dead tunnels after network change, battery drain, OEM kills.
- Reviewing a VPN app for Play submission (VPN declaration, target API, FGS type).
- Provisioning the server side (Xray/VLESS, WireGuard, SOCKS5) that the app talks to.

## The four layers (never confuse them)

```
1. UI/state      Compose/MVVM: profiles, connect button, stats
2. VpnService    consent -> TUN device -> fd -> foreground service -> lifecycle
3. Packet pump   fd <-> (tun2socks | proxy core | WireGuard lib)
4. Transport     what crosses the wire: VLESS/Reality, WG, SOCKS5, Hysteria2
```

Most "VPN is buggy" reports are layer-2 or layer-3 bugs (lifecycle, fd, socket protection),
not protocol bugs. Diagnose in that order.

## Decision tree — pick the core before writing UI

1. **Server already exists and speaks VLESS/VMess/Trojan/Hysteria?**
   → Recipe B: `libXray` (MIT) + `hev-socks5-tunnel` (MIT). Non-negotiable if you want a custom UI and MIT licensing.
2. **You control both ends and want minimum code / best battery?**
   → Recipe C: WireGuard official `com.wireguard.android:tunnel` (Apache-2.0). Server = `wg-quick`.
3. **Learning / teaching the stack, or a locked-down corporate proxy?**
   → Recipe A: TUN + `hev-socks5-tunnel` + any SOCKS5. Smallest surface, all layers visible.
4. **Need 20+ protocols and accept GPL?**
   → sing-box `libbox` (GPL-3.0 + no-affiliation clause — your app must be GPL and cannot imply endorsement).

Do **not** hand-write TLS/Reality/VLESS. Reality needs uTLS fingerprinting and XTLS padding; it is
a multi-month project and will be detected. Use a maintained core.

## Non-negotiables (violate one and the app fails in the field)

1. `VpnService.prepare()` on **every** connect attempt — another app may have taken the VPN slot since last time.
2. `protect(socket)` every socket the core uses to reach the server, or you get a routing loop / no traffic.
3. Route **both** `0.0.0.0/0` and `::/0`, or explicitly block IPv6 — otherwise IPv6 bypasses the tunnel (leak).
4. Add a DNS server to the TUN **and** route it, or DNS goes outside the tunnel (leak) and some apps fail to resolve.
5. `startForeground()` immediately; on API 34+ include a matching `foregroundServiceType`.
6. Persist the profile/config — always-on VPN starts the service with **no** intent extras.
7. Handle `onRevoke()` (may be off the main thread) and network changes; assume the process can be killed.

## Reference map

| File | Read it when |
|---|---|
| `references/catalog-111.md` | **111 verified repos** (stars/licence/last-push/verdict) from a 30-query sweep; start here when picking a library |
| `references/landscape-2026.md` | the short narrative version: what to pick and why, licence traps |
| `references/client-architectures.md` | how the six mainstream clients are actually built (build files, modules, SDK levels) |
| `references/android-platform.md` | VpnService mechanics, Builder API, always-on/lockdown, per-app, API 34/35/36/37, Play policy |
| `references/recipes.md` | step-by-step build for the three stacks, file layout, gradle config, JNI |
| `references/pitfalls.md` | something is broken; before shipping; code review checklist |
| `references/server.md` | provisioning Xray/3x-ui/WireGuard; client config; hardening |
| `references/testing-release.md` | QA matrix, testing without a device, ABI splits, signing, CI |
| `references/protocol-internals.md` | VLESS/Reality/XHTTP config fields, share-link format, silent-failure cheat-sheet |
| `references/field-issues.md` | evidence from 1000+ real issues: what actually breaks and how to reproduce it |
| `references/evals.md` | binary acceptance checklist before "done" / "ship" |
| `references/threat-model.md` | untrusted profiles, local-proxy exposure, DNS leaks, secret handling, supply chain |
| `references/ux-and-i18n.md` | error taxonomy, onboarding, RTL/Persian bidi traps, accessibility |
| `references/learning-path.md` | teaching the user from zero; sequencing the work into stages |
| `assets/` | drop-in Kotlin: `vless://` parser + Xray config, JNI bridge, service skeleton |

Companion skill: `android-engineering` (vendored MIT knowledge base) for architecture, Compose,
coroutines, testing and code review outside the VPN-specific parts.

## Hard truths to tell the user up front

- Raw TCP packets cannot be "forwarded". You need a userspace TCP stack (tun2socks) or a core that owns the flows.
- A VPN that "connects" but carries no traffic is the normal first state; routing + DNS + protect is the real work.
- Play requires target API 36 (since 2026-08-31), a VpnService declaration form, and VPN as core functionality.
- MIT cores (libXray, hev) let you ship closed-source. GPL cores (sing-box, v2rayNG code) do not.

## First 10 minutes on a new machine

1. `java -version` → need JDK 17; `sdkmanager --list_installed` → need platform 36 + build-tools.
2. Compile first, wire later: an empty VpnService that only calls `establish()` proves manifest +
   consent + FGS type before any protocol code exists.
3. Build one `vless://` link by hand and keep it as a fixture; the parser is the cheapest thing to
   test and the most expensive thing to get wrong.
4. Check `references/evals.md` section A before writing the pump.

## Scope and non-goals

**In scope:** Android client (VpnService, TUN, tun2socks), VLESS/Reality/VMess/Trojan wiring,
WireGuard/AmneziaWG, the matching server (Xray/3x-ui, wg-quick), Play submission.

**Out of scope (deliberately):** iOS/desktop clients, billing and subscription economy, legal advice
on provider ToS/sanctions, inventing a new crypto protocol, obfuscation research beyond AmneziaWG.
If a task lands there, say so instead of improvising.

## Verification status — what is tested vs merely read

| Claim | Evidence |
|---|---|
| `vless://` parser + Xray JSON output | 5 unit tests pass; output validated with `json.tool`; 1 bug found and fixed this way |
| `TunnelService` / `HevBridge` compile | compiled against AGP 8.5 / SDK 34; 1 bug found (missing icon resource) and fixed |
| Library metadata (stars, licence, last push) | GitHub API, 2026-10-24 |
| Play requires targetSdk 36 from 2026-08-31 | Play Console docs + 4 independent sources |
| Android 16/17 behaviour changes | developer.android.com `behavior-changes-16/17` |
| VLESS/Reality field names | XTLS docs repo, raw markdown, 2026-10 |
| Field bug frequencies | GitHub issue-search counts per repo |
| The project's server layout | read-only recon, 2026-10-24 |

**Not verified:** anything that needs a device or emulator (real traffic, battery, Doze, OEM kills,
Play review). No device is available in this sandbox — device behaviour must be tested by the user.
