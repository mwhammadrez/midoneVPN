# Expected findings: coroutines/04-coroutine-leak

## Critical

**Critical — unmanaged CoroutineScope, never cancelled: a coroutine and
memory leak.** `SyncManager` creates `CoroutineScope(Dispatchers.IO)` as
an instance property with no owner ever cancelling it. Combined with the
infinite `while (true)` loop in `startBackgroundSync`, every
`SyncManager` instance that's ever created keeps a coroutine running
network calls every 30 seconds forever, even after the instance itself is
no longer referenced anywhere else — this is a real, ongoing resource
leak (network, battery, and a retained reference chain preventing GC of
anything the coroutine closes over), not a hypothetical one, exactly the
"forbidden by default" pattern in `kotlin-coroutines-flow`.

**Fix:** inject the `CoroutineScope` from a caller that owns and cancels
it (or, if `SyncManager` should own its own lifecycle, give it an
explicit `stop()`/`close()` that cancels the scope), and track the launched
`Job` so a second `startBackgroundSync()` call doesn't stack a second
infinite loop on top of the first.

## Major

- **Major — no way to stop the sync loop.** Even with a properly owned
  scope, there's no `stopBackgroundSync`/job-cancellation path in the
  original code — the loop runs unconditionally until the whole scope is
  torn down, with no way to pause it (e.g. user disables background sync
  in settings).

## What a false positive looks like here

Flagging `Dispatchers.IO` as the wrong dispatcher choice is not correct
here — IO is the right choice for a network-pushing loop; the actual
defect is scope ownership/cancellation, not dispatcher selection.
