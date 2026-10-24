# Licence review

| Upstream | Licence | Can it ship in a closed app? | Obligation if used |
|---|---|---|---|
| libXray | MIT | yes | keep copyright + permission notice |
| Xray-core | MPL-2.0 | yes | publish changes to MPL-licensed files only |
| StormDNS / MasterDNS | MIT | yes | keep notice |
| WireGuard | Apache-2.0 | yes | NOTICE + patent grant |
| AmneziaWG | MIT | yes | keep notice |
| Psiphon core/library | GPL-3.0 | **no** | whole app becomes GPL-3.0, source offer |
| Shir o Khorshid | GPL-3.0 | **no** | same |
| PattNG | GPL-3.0 | **no** | same |
| UAC core (aether/quiche) | AGPL-3.0 | **no** | network-use clause: source to all users |
| UAC app | no licence | **no** | all rights reserved |
| WhiteDNS app | proprietary source-available | **no** | viewing only |
| Windscribe | GPL-2.0 | **no** | GPL-2.0 obligations, strongSwan too |

**Conclusion:** with a closed product, the shippable set is Xray/libXray + WireGuard/AmneziaWG + the MIT
DNS engines. Everything else is `blocked_license` until the product itself becomes GPL-3.0.
