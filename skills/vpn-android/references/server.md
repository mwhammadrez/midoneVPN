# Server side

## What the user's box already is (recon 2026-10-24, read-only)

Aeza VPS, Ubuntu 24.04, **1 vCPU / 3.8 GB / 9.8 GB disk (84% used)**, running:

- **3x-ui 3.10.2** on `127.0.0.1:2053` behind nginx, with **15 VLESS inbounds**
  (Reality on 4443/8443, XHTTP 11444/11445/11448/11449/23457/23458, gRPC 11446,
  WS 23456, plus MCI-specific 11453–11455 and a relay 18443)
- subscription server, Telegram bot, `slipgate` SOCKS5 (`127.0.0.1:1080`),
  nginx (80/443/4444-4446/8444/2052), Postgres, Redis, docker
- unrelated load: news site, STT (faster-whisper), voicebot, translate

**Implication:** it can serve the app as-is (Recipe B). Do **not** install a second VPN stack without
explicit permission — disk is at 84% and one busy vCPU is already at load ~1.0.
If a clean end is needed, provision a separate small VPS (~$3–5) rather than touching this one.

## Adding a client to 3x-ui (the practical path)

1. Panel → Inbounds → pick the inbound (prefer **VLESS + Reality, TCP, :443-like port**).
2. Add client: email/label, UUID (auto), flow `xtls-rprx-vision`, limit IP/traffic if selling.
3. Copy the `vless://` link; the app can consume it directly or via the subscription URL.
4. Verify from a laptop before shipping the app: `xray run -c client.json` or any desktop client.

Reality essentials (get these wrong and it fails silently):
- `dest`/`serverNames` must be a **real TLS site** reachable from the server, with matching SNI.
- `privateKey`/`publicKey` pair must match; `shortIds` non-empty; `spiderX` can stay empty.
- Server clock must be accurate (NTP) — a few minutes of skew breaks the handshake.
- Client `fingerprint` (uTLS) must be a browser-like value (`chrome`), not `random`.

## WireGuard server (if going Recipe C)

```bash
apt install wireguard
wg genkey | tee /etc/wireguard/server.key | wg pubkey > /etc/wireguard/server.pub
# wg0.conf: Address 10.66.66.1/24, ListenPort 51820, one [Peer] per client
sysctl -w net.ipv4.ip_forward=1
nft/iptables: masquerade 10.66.66.0/24 out of the physical iface
wg-quick up wg0
```
Plain WG UDP is trivially fingerprintable — if the target network blocks it, use **AmneziaWG**
(`Jc/Jmin/Jmax/S1/S2/H1..H4` parameters) and mirror the same params in the app.

## SOCKS5 endpoint (Recipe A)

`heiher/hev-socks5-server` or `danted`. Bind to `127.0.0.1` and reach it through an encrypted transport
(SSH tunnel / WireGuard / stunnel). **A public SOCKS5 is an open proxy** — never expose it; Play also
rejects unencrypted VPN products.

## Hardening checklist (any of the above)

- [ ] SSH: keys only (`PasswordAuthentication no`), non-root user + sudo, `fail2ban` jail on sshd
- [ ] `ufw`: allow only 22/80/443 + the VPN port; default deny incoming
- [ ] auto-updates for security patches (`unattended-upgrades`)
- [ ] disk guard: `logrotate` + alert at 80%; journald `SystemMaxUse=200M`
- [ ] monitoring: uptime + per-inbound health check (`xray-checker` for Xray)
- [ ] backups of panel DB (`/etc/x-ui/x-ui.db`) off-box
- [ ] no secrets in shell history (`HISTCONTROL=ignorespace`, use env files with 600)

## Capacity sanity

One vCPU with VLESS/Reality realistically sustains ~30–60 active users at 10–20 Mbps aggregate
(Reality is cheap; TLS handshakes are the spike). Torrent/UDP-heavy users will exhaust it first.
