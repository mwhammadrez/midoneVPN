# Capability matrix

Status vocabulary is fixed by the brief:
`implemented` (code exists) · `build_verified` · `network_verified` · `not_run` ·
`blocked_license` · `blocked_credentials` · `blocked_environment` · `implemented_but_unverified`.

Nothing in this table is `network_verified`: no server credentials and no Iranian network are available
in this environment.

| Family | Engine | License | Code | Build | Network | Notes |
|---|---|---|---|---|---|---|
| VLESS / VMess / Trojan / Shadowsocks | Xray via libXray `v26.10.21` | MPL-2.0 / MIT | **adapter + TUN fd path** | **yes (41–43 MB/ABI)** | not_run | `XrayAdapter` + `LibXrayCore` + `XrayConfigBuilder`; AAR pinned by SHA-256 |
| REALITY, XHTTP, gRPC, WS transports | same | — | parser done (`VlessLink`) | yes | not_run | `password` vs `publicKey` handled; JSON emitted and validated |
| Subscription import (base64 / plain / JSON) | own parser | — | **implemented** | yes | not_run | bounded: 2 MB, 5k links, skip+count; fetcher not written yet |
| DNS tunnel (`stormdns://`, `soar://`) | StormDNS / MasterDNS (MIT) | MIT | not started | — | not_run | own integration, **not** WhiteDNS code |
| Endpoint scanning / ranking | own + libXray `pingBatch` | — | **implemented** | yes | not_run | ranking = real tunnel + traffic, budgets + cancel |
| WireGuard / AmneziaWG | official lib / AmneziaWG | Apache-2.0 / MIT | not started | — | not_run | smallest code path |
| Psiphon (+ CDN-fronted meek) | psiphon-tunnel-core | GPL-3.0 | **blocked_license** | — | — | only if product goes GPL; also `blocked_credentials` |
| UAC SNI spoofing | UAC app / Aether | none / AGPL-3.0 | **blocked_license** | — | — | ideas only |
| OpenVPN | ics-openvpn | GPL-2.0 | **blocked_license** | — | — | closed product |
| IKEv2 | strongSwan | GPL-2.0 | **blocked_license** | — | — | closed product |
| Windscribe Stealth / WStunnel | Windscribe app | GPL-2.0 | **blocked_license** | — | — | also `blocked_credentials` |
| PattNG Xray patches | PattNG | GPL-3.0 | **blocked_license** | — | — | weakens encryption by design; not adopted |

## Engine descriptors the app must carry

Each row above becomes an `EngineDescriptor` with: id, version, provenance URL + commit, license id,
supported config formats, required server/credential data, UDP capability, TUN ownership
(`owns-tun` vs `local-proxy`), supported diagnostics, and integration status.

## Deliberate exclusions

- No second Psiphon core. If Psiphon is ever integrated, one shared engine serves both plain and
  CDN-fronted-meek profiles.
- No engine chaining (no tun2socks in front of an engine that owns a TUN).
- No copying of WhiteDNS/UAC application code, assets, branding, or scheme names.
