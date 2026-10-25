# Evals — acceptance checks for a VPN client

Run these before calling any stage "done". Each item is binary: pass / fail / not-run.
A failed item is a blocker, not a nit. (Companion to `pitfalls.md`, which explains *why*.)

## A. Lifecycle (static review)

| # | Check | Pass criterion |
|---|---|---|
| A1 | `prepare()` called on every connect | code path exists; not cached in a boolean |
| A2 | `establish()` null-checked | failure path stops service and surfaces an error |
| A3 | `startForeground` before any long work | within the first statements of `onStartCommand` |
| A4 | FGS type matches manifest | `specialUse` + permission + `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` |
| A5 | null-intent start handled | always-on path loads persisted config; no crash |
| A6 | `onRevoke()` implemented | teardown + stopSelf; no auto-reconnect loop |
| A7 | teardown idempotent | guard flag; double-stop is safe |
| A8 | no blocking call on main thread | pump on its own thread; `run()` never on main |

## B. Packet path

| # | Check | Pass criterion |
|---|---|---|
| B1 | server socket `protect()`ed | every outbound socket, incl. core internals |
| B2 | routes installed | `0.0.0.0/0` **and** `::/0` (or IPv6 deliberately disabled) |
| B3 | MTU sane | ≤ 1400 for TLS-based transports; verified on mobile data |
| B4 | `setBlocking(true)` | no busy-loop; CPU < 2% idle |
| B5 | UDP works | a QUIC/WhatsApp call succeeds (if the core supports UDP) |
| B6 | self-exclusion | own package in disallowed list or server IP excluded |

## C. DNS

| # | Check | Pass criterion |
|---|---|---|
| C1 | in-tunnel resolver | `addDnsServer` + route to it |
| C2 | no leak | dnsleaktest shows only the tunnel's resolver |
| C3 | resolver failure path | blocked/removed resolver produces an error, not a silent hang |
| C4 | powering core DNS | libXray: `SetDNS` before `runXray`, `resetDNS()` after |

## D. Resilience

| # | Check | Pass criterion |
|---|---|---|
| D1 | Wi-Fi → LTE mid-download | transfer resumes within ~5 s, no manual reconnect |
| D2 | airplane mode ×20 | reconnects each time; no zombie state |
| D3 | 8 h screen-off soak | still connected, battery < 2%/h, memory flat |
| D4 | watchdog | 30 s of zero packets triggers a transport restart once (not a loop) |
| D5 | backoff | repeated failures back off exponentially, no battery-burning retry storm |
| D6 | OEM kill survival | Xiaomi/Huawei: documented settings + re-arm works |

## E. Security / privacy

| # | Check | Pass criterion |
|---|---|---|
| E1 | local proxy bound to 127.0.0.1 | `ss -tlnp` on device shows loopback only |
| E2 | no keys in logs/crash reports | grep logcat after a session |
| E3 | keys encrypted at rest | Keystore / EncryptedSharedPreferences |
| E4 | `allowBackup=false` | profile export is explicit, not via ADB backup |
| E5 | no traffic before the tunnel is up | connect → immediately `curl` an IP; must not leak |

## F. Release

| # | Check | Pass criterion |
|---|---|---|
| F1 | `targetSdk ≥ 36` | Play requirement since 2026-08-31 |
| F2 | edge-to-edge | no inset overlap on API 35+ (opt-out is gone at 36) |
| F3 | predictive back | `OnBackInvokedCallback`, or explicit `enableOnBackInvokedCallback=false` |
| F4 | per-ABI splits | AAB; APK per ABI ≤ ~35 MB |
| F5 | VPN declaration submitted | Play Console form + privacy policy URL |
| F6 | crash reporting | symbolicated native crashes (Native symbols uploaded) |
| F7 | upgrade path | old profile formats still parse after update |


## G. UX / i18n (skip these and the app feels unfinished)

| # | Check | Pass criterion |
|---|---|---|
| G1 | every failure has a code, cause and action | no "unknown error" without a copy-log button |
| G2 | "Connected" is probe-verified | a 3 s probe passes before the state flips |
| G3 | degraded state exists | icon-on-but-dead is shown, not hidden |
| G4 | RTL mirrors | arrows, chevrons, progress direction |
| G5 | bidi-safe technical strings | UUIDs/links wrapped in LTR embedding or rendered LTR |
| G6 | Persian text fits | longest label («قطع اتصال») fits the button at 320 dp |
| G7 | TalkBack | status and primary action readable; 48 dp targets |
| G8 | contrast | body text ≥ 4.5:1 on the dark theme |

## H. Threat model (see `threat-model.md`)

| # | Check | Pass criterion |
|---|---|---|
| H1 | imported profile confirmed | parse → summary → user tap → connect |
| H2 | caps enforced | link ≤ 4 KB, subscription ≤ 2 MB, config ≤ 1 MB |
| H3 | subscription over HTTPS, cached | no downgrade, last-good survives a bad response |
| H4 | Private DNS handled | notice shown or port 853 routed |
| H5 | service not startable by others | `exported=false` or caller check |

## Scoring

- **Ship** = all A + B + C pass, ≥ 80% of D, all E + F + H, ≥ 80% of G.
- Anything in A or E failing means "do not ship", regardless of how good the protocol is.
- Re-run A–E after **every** core upgrade: the field data shows core upgrades regress working nodes.
