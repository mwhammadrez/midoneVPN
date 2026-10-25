# Expected findings: coroutines/06-broken-cancellation

## Critical

**Critical — swallowed CancellationException breaks cancellation.** The
`catch (e: Exception)` block catches `CancellationException` (a subclass
of `Exception`) along with real failures and only logs it, never
rethrowing. Per `kotlin-coroutines-flow`'s cancellation section, this
breaks structured concurrency: if the coroutine running `uploadAll` is
cancelled (screen closed mid-upload), the loop does not stop — it keeps
uploading every remaining file — and the cancellation signal never
reaches whatever is awaiting this coroutine, which will hang or behave as
if the work completed normally.

**Fix:** add a specific `catch (e: CancellationException) { throw e }`
before the general `catch (e: Exception)`, so cancellation propagates
while genuine upload failures are still logged and the loop continues to
the next file.

## What a false positive looks like here

Flagging the broad `catch (e: Exception)` itself as the problem (e.g.
"should catch specific exception types instead") without identifying that
the specific consequence is broken cancellation propagation misses the
actual severity — a broad catch that didn't include `CancellationException`
handling would only be a Minor/Suggestion-level style concern, not
Critical.
