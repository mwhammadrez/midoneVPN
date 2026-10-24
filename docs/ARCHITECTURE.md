# Architecture

Single-app module today; packages carry the logical boundaries so they can be split into Gradle
modules without moving code.

```
dev.tunnelkit.app          UI (Compose), single diagnostic screen
dev.tunnelkit.core.model   ConnectionState, Failure, EngineMetrics, EngineId
dev.tunnelkit.core.engine  VpnEngine contract, EngineDescriptor, EngineCoordinator
dev.tunnelkit.core.config  (next) format detection, share links, subscriptions
dev.tunnelkit.engine.*     (next) one adapter per integrated upstream engine
```

## Engine contract

`VpnEngine` has six responsibilities and no more: descriptor, validate, prepare, start, stop,
metrics/diagnostics. Adapters translate; they never re-implement tunneling.

`EngineDescriptor` is the provenance record: engine name, version, upstream URL + commit, license id,
supported formats, TUN ownership, UDP capability, required server data, and integration status
(`IMPLEMENTED` … `BLOCKED_LICENSE`).

## State machine

```
Idle → Validating → Preparing → (Discovering) → Connecting → Connected
                          ↘ Failed(failure, engine)        ↘ Reconnecting(n)
Stopping → Stopped
Blocked(failure)   ← license / credentials / permission, requires the owner
```

Every failure is a typed `Failure` with a stable `code`; the UI maps code → message + action.

## Coordinator invariants

- One active engine, always. Android allows one `VpnService` per user profile, so concurrency is a bug,
  not a feature.
- `switchTo()` is `disconnect()` then `connect()` — never interleaved.
- Any throwable inside `connect()` triggers `stop()` on that engine (resources may be half-acquired).
  This was found by a test, not by review.
- `connect()` on the already-connected engine is a no-op.
- `EngineBlockedException` produces `Blocked`, not `Failed` — a licence problem is not a network problem.

## TUN ownership

| Engine kind | Owns TUN | Our job |
|---|---|---|
| libXray (Android) | app creates it, **engine consumes fd** (`APP_TUN_FD_PASSED`) | pass fd via `XRAY_TUN_FD`, no second pump |
| libXray (desktop/test) | no (`LOCAL_PROXY`) | local SOCKS + external pump |
| WireGuard `GoBackend` | yes (`ENGINE_OWNS_TUN`) | we hand it the config; no tun2socks in front |
| Future Psiphon | engine-provided | follow its integration contract |

Chaining two owners is explicitly forbidden; the coordinator does not model chains.

## Next increments

1. `core.config`: format detection + `vless://` (already exercised as a skill asset) + subscription
   fetcher with size/time bounds and last-good cache.
2. `engine.xray`: wrap libXray `Invoke(json)`, map its callbacks to `ConnectionState`, protect sockets.
3. `engine.dnstunnel`: StormDNS/MasterDNS (MIT) with its own provenance.
4. `engine.wireguard`: official tunnel library.
5. Health probe: authorised download through the tunnel → `Connected` is earned, not assumed.
