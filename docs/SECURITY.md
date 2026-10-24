# Security notes

## Secrets
- Profile storage is **Keystore-backed `EncryptedSharedPreferences`** (AES256-SIV keys, AES256-GCM values)
  with a one-way migration from the legacy plaintext file. If the Keystore key is invalidated the store
  throws instead of falling back to plaintext. `android:allowBackup="false"` is set.
- Release signing material (`keystore.properties`, `*.jks`) is git-ignored and must never be committed.
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
