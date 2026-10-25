# Protocol internals — VLESS / Reality / XHTTP (verified against XTLS docs, 2026-10)

Enough to build a client config generator that does not produce silently-broken tunnels.

## VLESS

- Stateless; auth = UUID; **no dependence on system clock** (unlike VMess).
- Always needs an outer security layer (TLS / Reality) or `VLESS Encryption`, unless the peer is a
  private address on a trusted link.
- `flow: "xtls-rprx-vision"` is the performance mode; it must be set on **both** client and server,
  and only works with TCP-based transports (not WS/XHTTP/gRPC).

Client outbound (shape from the official docs):
```json
{
  "protocol": "vless",
  "settings": {
    "address": "example.com", "port": 443,
    "id": "5783a3e7-e373-51cd-8642-c83782b807c5",
    "encryption": "none",
    "flow": "xtls-rprx-vision"
  }
}
```
Older/other configs use `settings.vnext[]` with the same fields — both are parsed; keep one style
per codebase.

## REALITY

Camouflage, not encryption of the payload: it wraps a real TLS handshake against a real third-party
site (`target`) and only the server that holds the private key can complete it.

**Server side**
| Field | Meaning | Trap |
|---|---|---|
| `target` (alias `dest`) | real site, `host:443` | must be reachable **from the server**; behind Cloudflare you become a port-forwarder |
| `serverNames` | allowed SNI list | must match the target's certificate SANs; `*` not supported |
| `privateKey` | from `xray x25519` | regenerate = every client breaks |
| `shortIds` | client discriminators | at least one; `""` allowed only if listed |
| `maxTimeDiff` | clock tolerance | skew > tolerance = handshake fails |
| `mldsa65Seed` | post-quantum signing | target cert must be > 3500 bytes |

**Client side**
| Field | Meaning | Trap |
|---|---|---|
| `serverName` | SNI to present | must be in server `serverNames` |
| `fingerprint` | uTLS profile, e.g. `chrome` | `random`/`firefox` can fail on some servers |
| `password` | **x25519 public key** (renamed from `publicKey`) | this is the #1 wrong-field bug |
| `shortId` | one of the server's values | must be an exact member (list, not prefix) |
| `spiderX` | path mimicry, usually `/` | optional |

Do **not** send `target` in a client config — the core detects client vs server by its presence.

## Transports

| Type | URI param | Notes |
|---|---|---|
| TCP (`raw`) | `type=tcp` | pairs with Vision; best throughput |
| XHTTP | `type=xhttp` | CDN-friendly, more moving parts (`path`, `mode`, `host`) |
| WS | `type=ws` | CDN + nginx; needs `path` and often `host` header |
| gRPC | `type=grpc` | `serviceName`; not every CDN accepts it |

## Share-link format (what the app must parse)

```
vless://<uuid>@<host>:<port>?encryption=none&security=reality&sni=<serverName>
        &fp=chrome&pbk=<publicKey>&sid=<shortId>&type=tcp&flow=xtls-rprx-vision#<label>
```
- `pbk` = public key (= config `password`), `sid` = shortId, `sni` = serverName.
- `type=ws` adds `path`, `host`; `type=grpc` adds `serviceName`; `type=xhttp` adds `path`, `mode`, `host`.
- Labels are URL-encoded — Persian names arrive percent-encoded; decode before showing.

## Minimal client config the app should generate

```json
{
  "log": {"loglevel": "warning"},
  "inbounds": [{
    "tag": "socks-in", "listen": "127.0.0.1", "port": 10808,
    "protocol": "socks", "settings": {"udp": true, "auth": "noauth"}
  }],
  "outbounds": [{
    "tag": "proxy", "protocol": "vless",
    "settings": {"address": "SERVER", "port": 443, "id": "UUID",
                 "encryption": "none", "flow": "xtls-rprx-vision"},
    "streamSettings": {
      "network": "tcp", "security": "reality",
      "realitySettings": {"serverName": "SNI", "fingerprint": "chrome",
                          "password": "PUBKEY", "shortId": "SID", "spiderX": "/"}
    }
  }],
  "routing": {"domainStrategy": "AsIs", "rules": [
    {"type": "field", "ip": ["geoip:private"], "outboundTag": "direct"}
  ]}
}
```
Add a `direct` outbound (`"protocol":"freedom"`) whenever you reference one, or the core refuses to
start with "outbound not found".

## Subscription formats

- Plain text, one URI per line, or a **base64** blob of the same.
- Some panels return JSON (`{"servers":[...]}`, Clash YAML, sing-box JSON). Detect by first
  non-whitespace byte: `{` → JSON, `p`/`s` → YAML, else try base64 then line-split.
- Always cache the last good config; a server returning a captive-portal HTML page must not wipe the
  user's profile.


## Protocol matrix — what each one costs you

| Protocol | Core needed | UDP | Battery | Censorship resistance | When to choose |
|---|---|---|---|---|---|
| VLESS + Reality (TCP) | libXray | via Vision | good | **high** (real-TLS camouflage) | default for Iran; 443-like port |
| VLESS + XHTTP | libXray | no | medium | high + CDN-friendly | when direct IPs are blocked |
| VLESS + WS/gRPC | libXray | no | medium | medium (needs TLS + CDN) | behind Cloudflare only |
| VMess | libXray | no | medium | low-medium | legacy compatibility |
| Trojan | libXray | no | medium | medium | TLS sites that must look like HTTPS |
| Shadowsocks (AEAD) | small, Kotlin-able | yes | good | **low** (fingerprintable) | simple relays, fallback |
| WireGuard | native lib | yes | **best** | low | trusted networks, low-power |
| AmneziaWG | native lib | yes | best | medium-high | WG where plain WG is blocked |
| Hysteria2 | separate core (QUIC) | yes | higher | high on lossy links | bad mobile links (high loss/latency) |
| TUIC | separate core (QUIC) | yes | higher | high | similar to Hysteria2 |

Rule: one TCP+TLS family (VLESS/Reality) **and** one UDP family (Hysteria2 or AmneziaWG) covers
almost every network condition. Shipping only one means a fraction of users cannot connect at all.

## Misconfiguration cheat-sheet (silent failures)

| Symptom | Usually |
|---|---|
| Handshake works, no data | `flow` mismatch between client and server |
| Fails only on mobile data | SNI/`serverName` not in `serverNames`, or MTU/fragmentation |
| Works in v2rayN, not in your app | you sent `publicKey` where the core expects `password` (or vice versa) |
| Intermittent drops at ~1 min | `maxTimeDiff`/clock skew, or `shortId` typo |
| Everything times out after a panel update | Reality keys rotated |
