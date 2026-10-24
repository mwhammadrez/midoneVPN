# Test plan

## Automated (runs in this environment)
| Suite | Scope |
|---|---|
| `Ipv4Test` | header parsing, checksum, ICMP reply |
| `EngineCoordinatorTest` | state machine, cleanup, switching, idempotency, blocked engines |
| `XrayAdapterTest` | descriptor/provenance, validation, API v3 envelope, TUN fd path, typed failures |
| `ConfigParsingTest` | format detection, base64/plain subscriptions, caps |
| `UiSnapshotTest` | Compose render on JVM (Paparazzi) |

## Manual / device (cannot run here)
- [ ] connect with a real profile on Wi-Fi
- [ ] connect on mobile data (and after a Wi-Fi↔LTE switch mid-session)
- [ ] screen off 30 min → tunnel still up, battery sane
- [ ] revoke VPN permission → clean teardown, no crash
- [ ] start/stop ×20 → no leak, no zombie fd
- [ ] switch engine A→B→A → only one active
- [ ] DNS leak test, IPv6 leak test
- [ ] airplane mode ×10
- [ ] release build (R8) behaves like debug

## Acceptance
`Connected` is only valid after a real HTTPS request traverses the tunnel (`generate_204` probe).
Any unexecuted item must be reported as `not_run` with a reason.
