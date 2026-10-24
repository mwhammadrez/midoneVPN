# Configuration formats

## Detected by the app (`ConfigDetector`)

| Input | Detection | Notes |
|---|---|---|
| `vless://…` | `VLESS_LINK` | parsed by `VlessLink`, validated (uuid, port, reality fields, flow/transport rules) |
| `vmess/trojan/ss/hysteria2/tuic://…` | `SHARE_LINK` | detected, not yet parsed |
| `stormdns://` / `soar://` | `DNS_TUNNEL_LINK` | format must come from StormDNS source |
| `[Interface]` + `PrivateKey` | `WIREGUARD_CONF` | detected, not yet parsed |
| OpenVPN profile markers | `OPENVPN_PROFILE` | detected, blocked by licence |
| `{…"outbounds"…}` | `XRAY_JSON` | passed through after a shape check |
| many lines / base64 | `SUBSCRIPTION_TEXT` | `SubscriptionParser`: ≤2 MB, ≤5000 links, counts skipped |
| anything else | `Unsupported` | never guessed |

## VLESS link requirements

`UUID@host:port`, `security=reality` → requires `pbk` and `sni`; `type=ws` → requires `path`;
`type=grpc` → requires `serviceName`; `flow=xtls-rprx-vision` only with `type=tcp`.

## Not yet implemented

Subscription **fetching** over the network (HTTPS-only, redirect cap, private-address block, last-good
cache). Parsing of an already-downloaded body exists.
