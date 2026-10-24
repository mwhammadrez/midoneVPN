# Engine integration matrix

| Engine | Integration type | Artifact | Pinned | Status |
|---|---|---|---|---|
| Xray via libXray | direct AAR + adapter | `libXRay.aar` | v26.10.21 / dbe9512 | implemented, build-verified |
| Xray TUN fd path | config adapter (`XRAY_TUN_FD`) | — | — | implemented, verified on Linux binary only |
| StormDNS / MasterDNS | adapter required | not vendored | — | not started |
| WireGuard | official library | not vendored | — | not started |
| AmneziaWG | native lib | not vendored | — | not started |
| Psiphon | permission + licence required | — | — | blocked_license / blocked_credentials |
| CDN-fronted meek (Shir o Khorshid) | fork/profile required | — | — | blocked_license |
| UAC SNI spoofing | not reusable | — | — | blocked_license |
| OpenVPN / IKEv2 | GPL components | — | — | blocked_license |
| Windscribe Stealth/WStunnel | permission required | — | — | blocked_license + blocked_credentials |
