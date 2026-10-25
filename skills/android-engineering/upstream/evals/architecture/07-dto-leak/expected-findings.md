# Expected findings: architecture/07-dto-leak

## Major

**Major — DTO leaks into presentation.** `ProfileUiState.user` is typed as
`UserDto` (a `@Serializable` wire-format model), referenced directly from
`ProfileScreen`'s Compose code. Per `android-architecture`'s DTO/domain/UI
boundary: this couples the UI to the API's exact shape — a backend rename
of `display_name`, or a change from `is_verified_account: Int` to a real
boolean, breaks the UI layer directly instead of being absorbed by a
mapper. It also forces the composable to deal with wire-format
nullability and API quirks (`is_verified_account == 1`) that a domain
model should have already resolved.

**Fix:** introduce a `User` domain model and a `UserDto.toDomain()`
mapper inside the repository layer; `ProfileViewModel`/`ProfileUiState`/
`ProfileScreen` should only ever see `User`, never `UserDto`. Also: the
ViewModel calls `api.fetchUser` directly with no repository interface at
all — see fixture 8 for that specific violation, which this fixture's
`before.kt` also exhibits.

## Minor

- **Minor — no repository abstraction present at all** (related to, but
  distinct from, the DTO leak): `ProfileViewModel` depends on `ProfileApi`
  directly. Flag alongside the DTO-leak finding, but note it's really the
  fixture-8 mistake co-occurring here — see that fixture for the
  dedicated writeup.

## What a false positive looks like here

Flagging `UserDto`'s field naming (`display_name`, snake_case) as a bug is
incorrect — that's the DTO faithfully mirroring the wire format, which is
correct and expected for a DTO per `android-architecture`. The defect is
the DTO being visible outside `data`, not the DTO's own shape.
