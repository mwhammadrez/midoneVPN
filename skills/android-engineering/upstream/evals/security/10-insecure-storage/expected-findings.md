# Expected findings: security/10-insecure-storage

## Critical

**Critical — auth token stored in plain SharedPreferences.** `TokenStore`
persists the access token with `Context.getSharedPreferences(...,
MODE_PRIVATE)` and no encryption at rest. Per `android-security`'s secure
local storage section, `MODE_PRIVATE` only restricts other *apps* via
normal Android sandboxing — it does not protect against a rooted device,
a device backup extraction, or any access to the app's data directory
outside the normal permission model. A leaked access token is a direct
account-takeover risk, which is why this is Critical rather than Major
even though the code "works."

**Fix:** back the store with `EncryptedSharedPreferences` (Keystore
-derived key, never a manually stored key) as in `after.kt`, or an
equivalent Keystore-backed secure storage mechanism.

## Minor

- **Minor — no way to clear the token on logout** in the original code
  (`after.kt` adds `clearToken()`); leaving a stale token in storage
  after logout is a smaller but real exposure window.

## What a false positive looks like here

Flagging `MODE_PRIVATE` itself as deprecated/wrong API usage (it's not —
it's still the correct mode for a private preferences file) instead of
identifying the actual issue (no encryption at rest for sensitive data)
misses the substance of the finding.
