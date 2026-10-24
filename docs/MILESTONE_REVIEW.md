# Milestone review record

## Gate A — audit & foundation
| Check | Result |
|---|---|
| Licences read from files at the reviewed commit | yes — 7 sources, hashes/commits in `UPSTREAM_AUDIT.md` |
| Every capability tied to a commit/file | yes for the decision-relevant ones; see evidence column |
| App vs engine vs service separated | yes — `CAPABILITY_MATRIX.md` |
| Prohibited code excluded | WhiteDNS and UAC application code absent; only ideas recorded |
| Coordinator has tests | 8 tests, green |
| Baseline build executed | yes — 28 → 35 tests, APK per ABI |
| Uncertainty documented | `KNOWN_LIMITATIONS.md` |

**Findings fixed during review:** cleanup was skipped when `start()` threw mid-way (found by
`startFailureCleansUp`), then fixed.

## Gate B — engine integration
| Check | Result |
|---|---|
| Adapter calls a real engine | `XrayAdapter` → `LibXrayCore` → `LibXray.invoke` (real AAR) |
| Repeated start/stop/switch safe | covered by `EngineCoordinatorTest` (switch, idempotent reconnect, disconnect×2) |
| fd/thread/socket release | teardown order fixed in `XrayTunnelService`; engine `stop()` unconditional on failure |
| Imported config reaches engine fields | `VlessLink` → `XrayConfigBuilder` (socks + tun shapes), tested |
| address / SNI / Host separated | modelled as distinct fields; not merged anywhere |
| Release build works | `assembleRelease` produces unsigned APK per ABI |
| Duplicate cores | none — single Xray core, single engine active |
| Scanner limits | not implemented yet; policy written |
| No secrets in logs | redaction rules documented; no logging of payloads exists |

## Gate C — verification
| Check | Result |
|---|---|
| Clean build | yes (repeatable in this container) |
| Unit tests | 35, 0 failures |
| Real connection | **not_run** — no credentials, no device, no Iranian network |
| Emulator/device | **blocked_environment** (no KVM; emulator wedged this 4 GB box) |
| Sizes measured | universal 141 MB; arm64 41 MB, armv7 41 MB, x86_64 43 MB |
| Licences respected | yes; GPL/AGPL components excluded |
| Secrets in git | no git repository exists yet |
| Honest gaps | `KNOWN_LIMITATIONS.md` |

**Statement:** `Connectivity on an Iranian network has not been verified.`
