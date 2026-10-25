# MidONe — device test checklist (for the owner)

Install build `0.5-debug`. Everything below takes ~5 minutes.

## 1. Install

```bash
adb install -r MidONe-0.5-debug-arm64.apk     # or copy to the phone and tap it
```

arm64 covers modern phones; use `armv7` only for older 32-bit devices.

## 2. Capture logs (start this BEFORE connecting)

```bash
adb logcat -c
adb logcat -s MidONe:V VpnService:* > midone-test.log
```

Without adb? The app writes nothing to a file yet — in that case just report the **exact text**
shown under the server name on screen.

## 3. Import a profile

Tap **+ ADD** → paste a `vless://…` link → IMPORT.

Accepted: `security=tls` or `security=reality` on any port.
Rejected on purpose: `security=none` on a public host (stock Xray refuses it — this is not an app bug).

For WireGuard, paste a `.conf` with `[Interface]` / `[Peer]`. It is parsed, validated and stored, but
**cannot connect yet** (engine not integrated) — the app says so.

## 4. Connect

Tap the power button → Android asks for VPN permission → allow.

Expected outcomes:

| What you see | Meaning |
|---|---|
| `ON` + green dot, then it stays | tunnel up **and** a real HTTPS request passed |
| `⚠ timeout: tunnel up but probe failed` | interface came up, traffic did not flow |
| `⚠ unsupported_feature: …` | this link cannot work with the stock engine |
| `⚠ permission_denied` | permission was refused or revoked |
| `⛔ license_blocked` | never happens for VLESS — would indicate a bug |

## 5. Test the scanner

Tap **TEST** next to SERVERS. It probes each imported profile with `pingBatch` (real tunnel + traffic).
Progress and CANCEL appear in the same row. A result like `2 از 3 تونل واقعی · بهترین 128ms` means two
profiles actually carried traffic.

## 6. Then the rest

- turn VPN off from the notification, reconnect
- switch Wi-Fi ↔ mobile data while connected
- force-stop the app, reopen → profiles must still be there (Keystore storage)
- revoke VPN permission in Settings → app must fail cleanly, not crash

## 7. What to send back

1. `midone-test.log` (or the exact on-screen text)
2. Phone model + Android version
3. Whether you were on Wi-Fi or mobile data
4. Whether the network is inside Iran

That is enough for me to move each row of `docs/DEVICE_TEST_RESULTS.md` from `blocked_environment` to
`passed` or `failed` — with evidence instead of assumptions.
