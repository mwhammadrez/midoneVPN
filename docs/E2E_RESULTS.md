# End-to-end results against the real Xray core

Date: 2026-10-24 · Xray-core `26.10.21` (linux-amd64, same version bundled in libXray)
Method: `scripts/e2e-local.sh` + `RealCoreConfigTest` — the app's *own* config builder feeds a real
Xray binary. No mocks, no device required.

Reproduce: `XRAY_BIN=/tmp/xray/xray ./scripts/e2e-local.sh`

## What passed

| Check | Result | Evidence |
|---|---|---|
| Generated config accepted for ws+tls | **passed** | `Configuration OK` |
| Generated config accepted for ws plaintext to a private host | **passed** | `Configuration OK` |
| Generated config accepted for REALITY | **passed** | `Configuration OK` |
| Generated config accepted for gRPC+TLS | **passed** | `Configuration OK` |
| TUN shape parses (routing included) | **passed** | `Configuration OK` |
| Pinned certificate (`pcs`) accepted | **passed** | `Configuration OK` |
| Traffic through VLESS+WS (real handshake, real payload) | **passed** | `MIDONE-TUNNEL-OK`, server log `received request` |
| Traffic through VLESS+WS+TLS (pinned, real handshake) | **passed** | `MIDONE-TUNNEL-OK`, server log `received request` |
| Control: kill the tunnel, traffic must stop | **passed** | request fails, no direct leak |

## Three defects this found

### 1. `geoip:private` killed every connection at parse time
`failed to open geoip.dat` — libXray ships no data files. Every connect died before a socket opened
and the UI blamed the server. Fixed: explicit private CIDRs.

### 2. `allowInsecure` no longer exists in Xray >= 26
Links carrying `allowInsecure=1` would fail with a cryptic config error. Now parsed, warned about
(`cert_not_trusted`), and never emitted; `pcs=` / `vcn=` are supported instead.

### 3. Traffic bypassed the tunnel entirely
`direct` was the first outbound. Xray falls back to `outbounds[0]` when no rule matches, so the app
reported **Connected** while every packet left unproxied. Proven by pointing the client at a dead
VLESS server: the request still succeeded. Fixed: proxy first + explicit catch-all rule, with a
regression test (`DefaultRouteTest`).

## Not covered here (needs a device)

`VpnService.establish()` fd handover, `protectFd`, DNS through the TUN, handover Wi-Fi↔LTE, and the
Android Keystore store. Those remain `blocked_environment` in `DEVICE_TEST_RESULTS.md`.
