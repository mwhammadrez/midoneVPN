# Expected findings: coroutines/03-lifecycle-unaware-flow

## Major

**Major — Flow collected without lifecycle awareness.** `ticks.collect`
runs inside a bare `lifecycleScope.launch`, which keeps the collection
active as long as the Fragment's view exists — including while the
Fragment is stopped (backgrounded, covered by another screen). Whatever
produces `ticks` upstream (a timer, location updates, a socket
subscription) keeps running and doing work for a screen the user can't
see, and `render()` may be called on a view that's about to be destroyed.

**Fix:** wrap the collection in
`viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { ... }`
launched from `viewLifecycleOwner.lifecycleScope`, per
`kotlin-coroutines-flow`'s "Lifecycle interaction" section — collection
then automatically stops on `STOP` and resumes on the next `START`.

## Minor

- **Minor — uses `lifecycleScope` instead of `viewLifecycleOwner.lifecycleScope`
  in a Fragment.** `lifecycleScope` here is the Fragment's own lifecycle
  (survives longer than the view), not the view lifecycle; for
  view-bound work, `viewLifecycleOwner.lifecycleScope` is the more
  correct scope to launch from even independent of the `repeatOnLifecycle`
  fix.

## What a false positive looks like here

Flagging `lifecycleScope.launch` itself as a coroutine leak (fixture 4's
mistake) is incorrect here — the coroutine *is* owned and *does* get
cancelled when the Fragment is destroyed. The defect is specifically the
missing `repeatOnLifecycle`/state-gating, not an unmanaged scope.
