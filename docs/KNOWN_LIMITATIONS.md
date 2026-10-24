# Known limitations

Written to be believed, not to look good.

## Not verified at all

**Phase "device verification" is open**: no device is attached to this environment, so every
device-level test is `blocked_environment`. See `DEVICE_TEST_RESULTS.md` for the full matrix and for
the exact inputs (a working VLESS profile + a device) needed to close it.

- **No device, no emulator, no Iranian network.** `Connectivity on an Iranian network has not been verified.`
- No VLESS/Reality handshake has ever been attempted from this code. The adapter's envelope and error
  mapping are unit-tested; the engine itself has never been started.
- `LibXrayCore` compiles against the AAR but the Go runtime has never been loaded here.

## Verified only at the JVM/build level

- `XrayConfigBuilder` output shape (unit tested; JSON not yet schema-validated against a real Xray run).
- `XRAY_TUN_FD` semantics: verified against the **Linux xray 26.10.21 binary** — with `XRAY_TUN_FD=999`
  it fails with "bad file descriptor" (variable was read), without it "operation not permitted"
  (it tried to create its own TUN). Android's VpnService fd has not been exercised.
- The 41 MB per-ABI APK size is measured; runtime memory is not.

## Deliberately temporary

- `ProfileStore` now uses Keystore-backed EncryptedSharedPreferences with a one-way migration from the
  legacy plaintext file — but this has never run on a device (no key invalidation / process-death test).
- The UI drives `XrayTunnelService` and exposes import, method picker and a scanner action.
- Two `VpnService` classes exist (`TunnelVpnService` demo, `XrayTunnelService` real). Android permits
  only one active, but the app should expose only the real one.
- `EndpointScanner` is wired to the UI but has never executed `pingBatch` on a device.
- Health probe URL is still hard-coded in the service (`generate_204`); the scanner uses the same URL.

## Blocked by licence (not by effort)

Psiphon, UAC/Aether, OpenVPN, IKEv2/strongSwan and Windscribe remain `blocked_license` for a
non-GPL product. See `UPSTREAM_AUDIT.md`.

## Missing pieces from the brief

- Subscription **fetcher** (network, redirects, SSRF guard, last-good cache) — parser exists, fetcher not written.
- Endpoint scanner/ranker and its budgets.
- Engine switching UI, connection metrics surface, diagnostics export.
- `THIRD_PARTY_NOTICES.md` and `third_party/manifest.json`.
