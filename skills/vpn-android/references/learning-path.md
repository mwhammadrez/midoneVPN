# Zero → shipped: the learning path

Eight stages. Each has a deliverable you can show and a "done" test. Do not skip 2–4: they are where
the mental model of "what a VPN app actually is" is built.

## Stage 0 — the network model (a day)
**Goal:** know what a packet is and where TLS sits.
- IPv4 header fields, MTU, fragmentation; TCP handshake; UDP; DNS resolution order; TLS SNI.
- **Deliverable:** hand-draw the path of one HTTPS request: app → socket → kernel → NIC.
- **Done when:** you can explain why a TUN device sees IP packets, not TCP streams.
- **Read:** Beej's Guide to Network Programming; RFC 791 skim; Cloudflare "what is a TLS handshake".

## Stage 1 — Android app skeleton (1–2 days)
- Activity/Services lifecycle, `startForeground`, notifications, Compose basics, DataStore.
- **Deliverable:** an app with a button that starts/stops a foreground service with a notification.
- **Done when:** service survives 10 min in background on a real phone.

## Stage 2 — the toy tunnel (2–3 days)
- `VpnService.prepare` → `Builder` → `establish` → read/write fd; parse the IPv4 header.
- **Deliverable:** connect, then `ping 10.111.0.1` answered by your own code (this is exactly the
  `tunnel-android` stage-1 app we built — `Ipv4.kt` + `TunnelLoop.kt`).
- **Done when:** ping works and the UI shows live packet counts. Tests: checksum, echo reply.

## Stage 3 — packet plumbing (3–5 days)
- ICMP types, checksums, MTU, why DNS needs its own path, IPv6, `protect()`.
- **Deliverable:** DNS query to `10.111.0.1:53` answered by a tiny UDP responder in the pump.
- **Done when:** you can explain the difference between "packet in", "packet out", "packet dropped".

## Stage 4 — real traffic: tun2socks (1 week)
- Why userspace TCP is required; SOCKS5 protocol (CONNECT + UDP ASSOCIATE); `hev-socks5-tunnel`.
- **Deliverable:** full-tunnel browsing through a SOCKS5 on your VPS (Recipe A).
- **Done when:** a browser works, IPv6 does not leak, and battery stays sane for an hour.

## Stage 5 — a real protocol: VLESS + Reality (1 week)
- Xray config model (inbounds/outbounds/routing), VLESS, Reality (SNI borrowing), XTLS Vision.
- **Deliverable:** `vless://` link → `libXray.invoke(runXray)` → hev tun2socks (Recipe B).
- **Done when:** the app connects to the existing 3x-ui server and censored sites open.

## Stage 6 — product hardening (1–2 weeks)
- Always-on + lockdown, per-app split tunnel, handover, Doze/OEM, DNS policies, reconnect/backoff.
- **Deliverable:** 24-hour soak test with a written report (battery %/h, disconnects, recoveries).
- **Done when:** no manual reconnect needed across a day of Wi-Fi↔LTE switches.

## Stage 7 — server + subscriptions (1 week)
- 3x-ui/Marzban client management, subscription URL format, per-user limits, backups.
- **Deliverable:** a subscription link the app can import and refresh.
- **Done when:** adding a user takes < 1 minute and revocation actually cuts them off.

## Stage 8 — release (1 week)
- target API 36, signing, AAB, Play VPN declaration, privacy policy, crash reporting, update channel.
- **Deliverable:** internal-testing track build accepted by Play.
- **Done when:** review passes and a stranger can install and connect without your help.

## What to read in which order (source material)

1. `developer.android.com/develop/connectivity/vpn` — the only authoritative platform doc.
2. `android.googlesource.com/.../samples/ToyVpn` — the canonical minimal client.
3. `github.com/heiher/sockstun` — a complete, small, MIT Android app on tun2socks.
4. `github.com/heiher/hev-socks5-tunnel` — the tun2socks itself + YAML config surface.
5. `github.com/XTLS/libXray` — the Invoke API, DNS controller, protect controller.
6. `github.com/2dust/v2rayNG` — production-grade reference for UI, config import, routing (GPL: read only).
7. `github.com/wgtunnel/android` — best example of WG/AmneziaWG + lockdown mode + proxy mode.
8. `github.com/MHSanaei/3x-ui` — server side matching the app.
