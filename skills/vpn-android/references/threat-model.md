# Threat model — what actually gets attacked

A VPN client holds the user's entire traffic and their server credentials. The threats are boring and
specific; none of them is "someone breaks AES".

## Assets

1. **Traffic** (confidentiality + integrity) — the tunnel itself.
2. **Identity** — UUID, Reality public key, WireGuard private key, subscription URL.
3. **Server** — panel credentials, user list, billing.
4. **Device** — the app is a privileged network component; a bug here is a device-wide bug.

## Attack surface, ranked by how often it bites

| # | Surface | Attack | Impact | Mitigation |
|---|---|---|---|---|
| 1 | **Imported profile** (link, QR, subscription, deep link) | malicious/huge/malformed config; attacker-chosen server; config injection | traffic to attacker, core crash, OOM | strict parser (`VlessLink`), size caps, scheme allow-list, **confirm before connect** on deep links |
| 2 | **Subscription URL** | HTTP downgrade, redirect to attacker, zip-bomb, huge body | profile replacement, MITM | HTTPS only, ≤ 1 redirect, cap at ~2 MB, content-type allow-list, keep last-good cache |
| 3 | **Local SOCKS/HTTP port** | any app on the device connects to it and gets free tunnelling / can probe state | traffic attribution, abuse | bind `127.0.0.1` only, random port, no-auth only if loopback, log first-use |
| 4 | **DNS path** | Private DNS (DoT) or a disallowed app bypasses the tunnel resolver | leak of visited domains | see §3 |
| 5 | **IPv6** | only IPv4 routed | full leak with no UI signal | route `::/0` or disable IPv6 in the TUN |
| 6 | **Logs / crash reports** | UUID, keys, domains end up in Firebase/Crashlytics | credential + privacy loss | redact at the logging boundary, never log profile JSON |
| 7 | **Backup** | `adb backup` / cloud backup extracts keys | credential theft | `android:allowBackup="false"`, explicit encrypted export only |
| 8 | **Supply chain** | tampered AAR/native lib, unpinned CI action | full compromise | pin AAR by SHA-256, pin GitHub Actions by commit, verify release APK signature |
| 9 | **Server panel** | default creds, exposed port 2053, path traversal in panels | all users compromised | see `server.md` hardening |
| 10 | **Deep link / intent** | another app fires `ACTION_START` to toggle VPN | unwanted tunnel, or toggling off as a DoS | service `exported="false"` where possible; else verify caller/permission |

## 1. Untrusted profiles — the rule set

Parse → validate → **confirm** → store → connect. Never "connect because a link arrived".

```
vless://…  →  VlessLink.parse  →  validate()  →  show summary sheet  →  save → user taps Connect
```

- Cap the raw link at ~4 KB; cap decoded subscription at ~2 MB; cap config JSON at 1 MB
  (libXray rejects > 16 MiB, but you should be far stricter).
- Reject unknown schemes, `file://`, `content://` outside the picker, and any URI with a fragment that
  contains control characters.
- Guard against **homograph/IDN** hosts in the summary screen: show the punycode next to the Unicode.
- QR import must render the decoded text before saving it — a QR is still user input.

## 2. Local proxy exposure

If your core exposes SOCKS on `127.0.0.1:10808`, **every app on the device can use it**. That is
usually acceptable (it is the phone owner), but two things must hold:

- it never binds `0.0.0.0` (a hotel Wi-Fi would then proxy strangers through your server);
- the port is not advertised; use an ephemeral port chosen at start.

## 3. DNS: the leak everyone ships

Android 9+ has **Private DNS** (DoT). If the user has it set to a hostname:

- system lookups may go over TLS on the *underlying* network, not through your TUN;
- your VPN's `addDnsServer` covers apps that ask the system resolver, but DoT-configured lookups can
  bypass it.

Practical handling: read `Settings.Global.PRIVATE_DNS_MODE`; if it is `hostname`, show a one-time
notice and offer to route port 853 into the tunnel (or set the VPN's own resolver as the system one
and verify with a probe). Never silently claim "no leaks".

## 4. Secret handling

| Secret | Where | Never |
|---|---|---|
| Reality/WG private key, UUID | Keystore-backed `EncryptedSharedPreferences` | plaintext prefs, logs, analytics |
| Subscription URL (contains a token) | encrypted store | screenshot-free UI is optional, but don't put it in the notification |
| Server panel password | server only | app |

## 5. What to tell the user (honesty is a security feature)

- "Traffic goes to *this* server, operated by *you*" — a VPN app that hides the operator is a
  privacy risk by design.
- No-logs claim must match the code: if you keep a ring buffer of requests, say so.
- Play requires a privacy policy; it must match your Data safety form exactly.

## 6. Minimal security checklist before shipping

- [ ] profile import validated + confirmed
- [ ] subscription fetch: HTTPS, size-capped, cached
- [ ] local proxy on loopback + random port
- [ ] IPv6 routed or disabled
- [ ] Private DNS case handled (notice or route)
- [ ] logs redacted; crash reporting excludes profile data
- [ ] `allowBackup=false`
- [ ] AAR/native libs checksummed; CI actions pinned
- [ ] service not startable by other apps
- [ ] release APK signed with a key that never touches CI logs
