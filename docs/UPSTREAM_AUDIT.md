# Upstream audit

Date: 2026-10-24. Every commit below was cloned and inspected locally (`/home/user/audit`), not inferred
from repository descriptions. Licenses were read from the files in the tree at that commit.

## Summary

| # | Source | HEAD | License | Reusable in this product? |
|---|---|---|---|---|
| 1 | WhiteDNS/WhiteDNS-Android | `7eec3d4` (2026-10-23) | **Source-available proprietary, all rights reserved** | **No** — viewing only |
| 1a | iampedii/StormDNS (submodule) | not pinned in tree | **MIT** (per WhiteDNS notices) | **Yes**, as an independent engine |
| 1b | WhiteDNS/CottenDns (submodule) | not pinned in tree | needs separate verification | Unknown |
| 2 | Floxu1/UAC-SNI-Spoofer-Android | `b784ebd` (2026-10-14) | **No LICENSE file** at root → all rights reserved | **No** |
| 2a | `uac/core` (aether, quiche) | in-tree | **AGPL-3.0** | Only for an AGPL product |
| 2b | Xray-core used by UAC | `v26.7.28`, `5ca6f4b` | MPL-2.0 | Yes (via our own libXray pin) |
| 3 | v2RayTun | — | No public source or SDK found | **No source to integrate**; use Xray upstream |
| 3a | XTLS/Xray-core | `v26.10.21` | MPL-2.0 | **Yes** |
| 4 | Psiphon-Labs/psiphon-tunnel-core | `b35028d` (2026-10-20) | **GPL-3.0** | Only for a GPL-3.0 product |
| 4a | psiphon-tunnel-core-Android-library | `a53e1d1` (v2.0.42) | releases-only repo; inherits GPL-3.0 | Only for GPL |
| 5 | shirokhorshid/shirokhorshid-android | `2c71930` (2026-10-14) | **GPL-3.0** | Only for GPL |
| 6 | patterniha/PattNG | `7b3133f` (2026-10-24) | **GPL-3.0** | Only for GPL |
| 7 | Windscribe/Android-App | `2c585dd` (2026-09-01) | **GPL-2.0** | Only for GPLv2-compatible product |

**Decision this forces:** the product's own license decides which half of the requested feature set
exists. Xray/VLESS, WireGuard, hev-tun2socks and the MIT DNS engines are usable in a closed product;
Psiphon, Windscribe, Shir o Khorshid, PattNG and UAC are not. This is recorded as an explicit blocker,
not silently worked around.

---

## 1. WhiteDNS (`7eec3d4`)

**License file:** `LICENSE.MD` — "WhiteDNS Source-Available Proprietary License … All rights reserved.
This software is source-available, not open-source." Permission granted for viewing, security review
and contributions to the official project only. → `blocked_license` for reuse of any application code,
assets, or branding.

**What it actually is (read-only trace):**
- Config model: `app/src/main/java/shop/whitedns/client/model/WhiteDnsProfileLinks.kt` — parses
  `stormdns://` and `soar://` share links.
- Engines are **git submodules**, not app code: `third_party/StormDNS` → `iampedii/StormDNS`,
  `third_party/CottenDns` → `WhiteDNS/CottenDns`.
- `THIRD_PARTY_NOTICES.md` states MasterDNS (`masterking32/MasterDnsVPN`) and StormDNS are **MIT**.

**Conclusion:** integrate **StormDNS/MasterDNS as independent MIT engines with their own provenance**,
or write our own DNS-tunnel adapter against their documented interfaces. Do not copy WhiteDNS's
application layer, strings, assets or scheme names.

## 2. UAC SNI Spoofer (`b784ebd`)

**License:** no `LICENSE` at repository root → default copyright, no reuse rights. `THIRD_PARTY_NOTICES.md`
documents Xray-core `v26.7.28` (MPL-2.0) with per-ABI binary hashes, and `core/LICENSE` is **AGPL-3.0**
(the bundled aether/quiche work).

**Actual connection path (evidence, not description):**
- `app/src/main/java/com/uacspoofer/mobile/engine/pow/PowTun2Socks.kt` — its own tun2socks wrapper, not hev.
- `PowPsiphonClient.kt`, `AetherNative.kt`, `PowConnectionCoordinator.kt` — Psiphon + Aether + native.
- `core/` holds `aether`, `quiche`, `build-android.sh`.

**Conclusion:** `blocked_license` for all of the app code and for `core/` in a non-AGPL product.
Reusable knowledge only: the *shape* of an SNI-testing pipeline (dial address vs SNI vs Host separation).

## 3. v2RayTun

No public application source or SDK located; the site is a product page. Decompiling the APK is
prohibited by the brief and by general practice. → **not integrable as such**; implement the same
capabilities on **XTLS/Xray-core** `v26.10.21` (MPL-2.0) through **XTLS/libXray** (MIT), which is what
the rest of this project already assumes.

## 4. Psiphon (`b35028d` / `a53e1d1`)

`psiphon-tunnel-core` is **GPL-3.0** (`LICENSE` read). The Android repo is a releases-only repository
(published AARs, `ca.psiphon.aar`), so the license travels with the core, not the wrapper.
Bootstrap/discovery require server entries and (for the paid network) entitlements — no credentials are
included. → integrate only if the product is GPL-3.0; otherwise `blocked_license`.

## 5. Shir o Khorshid (`2c71930`)

**GPL-3.0.** Package namespace is `com.psiphon3`, i.e. it is a Psiphon-based client with a "CDN Front"
option. The requested behaviour ("CDN-fronted meek only") lives inside that Psiphon integration, so it
cannot be extracted independently without carrying GPL-3.0. → `blocked_license` for a closed product;
if the product is GPL, reimplement as a *profile* of one shared Psiphon engine (never two engines).

## 6. PattNG (`7b3133f`)

**GPL-3.0**; a v2rayNG fork (submodule `V2rayNG`) plus `AndroidLibXrayLite`, `hev-socks5-tunnel`, and
`aether` submodules. README declares Xray-core changes including "connecting to non-encrypted configs
for public addresses in VLESS and Trojan" — a **security-relevant weakening**. Under the brief's rules
this must never become a default, and it is another reason to use upstream Xray rather than the fork.
→ `blocked_license` for closed distribution; the fork's patches are documented but not adopted.

## 7. Windscribe (`2c585dd`)

**GPL-2.0** monorepo (`base`, `common`, `config`, `docs/features/ANTI_CENSORSHIP.md`), submodule
`strongswan-src` → `strongswan/strongswan` (GPL-2.0). Stealth/WStunnel are implemented inside this tree
and depend on Windscribe's own infrastructure and account entitlements. → `blocked_license` for a
closed product; also requires credentials for anything beyond the transport code.

---

## What is actually available to a non-GPL product

| Engine | Version / commit | License | Integration path |
|---|---|---|---|
| Xray-core | `v26.10.21` | MPL-2.0 | via libXray |
| libXray (Android AAR) | `v26.10.21` | MIT | `Invoke(json)` |
| hev-socks5-tunnel | `2.19.1` | MIT | JNI, `ndk-build` |
| WireGuard tunnel lib | `1.0.20230706` | Apache-2.0 | Gradle artifact |
| AmneziaWG (wgtunnel) | HEAD 2026-10-24 | MIT | native lib |
| StormDNS / MasterDNS | see notices | MIT | separate engine, own provenance |
| OpenVPN (ics-openvpn) | — | GPL-2.0 | **blocked** for closed product |
| strongSwan (IKEv2) | — | GPL-2.0 | **blocked** for closed product |

## Open questions that only the owner can answer

1. Is the product GPL-3.0 (then Psiphon/PattNG/shiro become available), or closed (then they stay blocked)?
2. Are Windscribe/Psiphon entitlements held? Without account/credentials those integrations cannot be
   network-verified at all, regardless of license.
