# Security notes

## Secrets
- Profile storage is currently **plain SharedPreferences** — a tracked limitation, not a design choice.
  Target: Keystore-backed `EncryptedSharedPreferences`. `android:allowBackup="false"` is set.
- Logs and diagnostics must be redacted: no UUID, no keys, no full URLs. `DiagnosticLine` carries text
  only; call sites must redact before appending.

## Untrusted input
- Imported links/files are untrusted: parse → validate → show → user confirms → store.
- Bounds: link ≤ 4 KB, subscription body ≤ 2 MB, ≤ 5000 links, config JSON ≤ 1 MB.
- Never auto-connect from a deep link.

## Network
- TLS verification is never disabled for tests.
- Local SOCKS (when used) binds `127.0.0.1` only, ephemeral port.
- IPv6 is routed into the tunnel (`::/0`) rather than silently leaking.
- Private DNS (DoT) is not yet detected/handled — tracked in KNOWN_LIMITATIONS.

## Platform
- One `VpnService` active at a time; the coordinator enforces it.
- `foregroundServiceType="specialUse"` with the FGS subtype property.
- `onRevoke()` tears everything down; teardown order is coordinator → fd close → foreground stop.
