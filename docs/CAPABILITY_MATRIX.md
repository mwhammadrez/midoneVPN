# Connection methods — honest status

Statuses are the only allowed set: `passed` · `failed` · `not_run` · `blocked_license` ·
`blocked_credentials` · `blocked_environment` · `implemented_but_unverified`.

| # | Method | Status | Engine | Evidence / blocker |
|---|---|---|---|---|
| 1 | VLESS / VMess / Trojan over WS · gRPC · XHTTP · TCP | **passed** (local e2e) | libXray 26.10.21 | real core accepts the generated config; live VLESS+WS and VLESS+WS+TLS carried an HTTP request end to end, and stopped when the server was killed. On-device: `blocked_environment` (no reachable test server from the sandbox). |
| 2 | REALITY / uTLS (SNI alignment, fingerprint) | **passed** (config) | libXray | `Configuration OK` from the real binary; live handshake needs a REALITY server, `not_run`. |
| 3 | CDN fronting (CDN IP + Host header + SNI) | **passed** (config) | libXray | Same path as #1; the app now also tries the site's own domain if the CDN apex refuses. |
| 4 | WireGuard | **implemented_but_unverified** | Xray `wireguard` outbound | parser + outbound renderer, generated config accepted by the real core (`Configuration OK`). No WireGuard server available to dial, so no handshake evidence yet. |
| 5 | DNS tunnel (StormDNS / MasterDNS) | **not_run** | none yet | MIT-licensed sources exist; needs a Go/NDK component plus a DNS server. |
| 6 | Psiphon | **blocked_license** | — | GPL-3.0; owner decided not to enable GPL engines. |
| 7 | OpenVPN | **blocked_license** | — | OpenVPN core is GPL-2.0. |
| 8 | IKEv2 | **blocked_license** | — | strongSwan is GPL-2.0. |

## Notes

- "SNI spoofing" as a separate method is covered by REALITY/uTLS in #2; a standalone fragment/noise
  layer is `not_run`.
- WStunnel adds nothing on top of #1 (the transport is already WebSocket over TLS).
- Anything GPL can be revisited the moment the owner says so; until then those rows stay blocked
  rather than silently shipped.
