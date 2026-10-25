# Expected findings: coroutines/05-dispatcher-misuse

## Critical

**Critical — blocking I/O with no dispatcher switch, freezes the UI.**
`exportToFile` performs blocking disk I/O (`File.writeText`) inside a
suspend function with no `withContext(Dispatchers.IO)` wrapper. Since
this is realistically called from a ViewModel's `viewModelScope`
(`Dispatchers.Main.immediate`), the blocking write runs directly on the
main thread — for a large report this is a visible UI freeze/ANR risk,
not just an inefficiency. Per `kotlin-coroutines-flow`'s dispatcher
section, this is the "real bug" case (blocking call, no switch), distinct
from the "mild inefficiency" case of redundantly wrapping an
already-non-blocking suspend call.

**Fix:** wrap the write in `withContext(dispatchers.io) { ... }`, with the
dispatcher injected via a `DispatcherProvider` abstraction rather than a
hardcoded `Dispatchers.IO` reference, so the class stays testable with a
`TestDispatcher`.

## What a false positive looks like here

Flagging this as a coroutine leak (fixture 4) or a cancellation bug
(fixture 6) misidentifies the defect — the function is a normal,
properly-scoped suspend function; the specific problem is the missing
dispatcher switch around a blocking call.
