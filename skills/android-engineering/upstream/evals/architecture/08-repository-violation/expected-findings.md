# Expected findings: architecture/08-repository-violation

## Major

**Major — ViewModel bypasses the repository abstraction.**
`OrdersViewModel` injects `OrderDao` and `OrdersApi` directly and contains
the local/remote fallback logic itself. Per `android-architecture`'s
repository-abstraction section, this logic (fetch remote, fall back to
local cache on failure) is exactly what a repository implementation
should own — the ViewModel now has to know about two different data
sources and their entity types (`OrderEntity`) instead of one domain-model
-returning interface, and this exact fallback logic would have to be
duplicated in any other ViewModel/use case that needs orders.

**Fix:** introduce an `OrdersRepository` interface returning domain `Order`
models, move the local/remote fallback into `OrdersRepositoryImpl`, and
have `OrdersViewModel` depend only on the interface.

## Minor

- **Minor — `OrderEntity` (a data-layer type) is used directly in
  `OrdersUiState`** in the original code, the same DTO/entity-leak
  category as fixture 7, co-occurring here because of the missing
  repository boundary.
- **Minor — swallowing the remote fetch exception with no logged/surfaced
  error state** — the UI has no way to tell the user "showing cached
  data because the network call failed," which `requirements-grill`'s
  offline/failure-behavior questions would normally have surfaced.

## What a false positive looks like here

Flagging the local/remote fallback strategy itself (cache-then-network
with fallback) as wrong is incorrect — that's a reasonable pattern. The
defect is *where* that logic lives (ViewModel instead of repository), not
the strategy itself.
