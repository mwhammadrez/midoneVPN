# Expected findings: compose/01-state-misuse

## Critical / Major

**Major — state not held in Compose state.** `count` is a plain local
`var`, re-initialized to `0` every time `Counter` recomposes, and
mutating it does not schedule a recomposition at all — Compose has no way
to know the value changed. In practice the displayed count never advances
past what's shown before the first recomposition; the button appears to
do nothing.

**Fix:** hold it in `remember { mutableIntStateOf(0) }` (or
`mutableStateOf` for a non-primitive), destructured with `by` so `count`
reads/writes through the state object. See `compose-development`'s
"`remember`, `rememberSaveable`, `derivedStateOf`" section.

## Minor / Suggestion

- **Suggestion** — if this counter's value should survive process death
  (e.g. rotation while mid-count matters to the product), use
  `rememberSaveable` instead of `remember`. Not required for this
  fixture's fix to be correct, but worth surfacing as a follow-up
  question per `compose-development`.

## What a false positive looks like here

Flagging the composable as "should be hoisted to a ViewModel" is not
correct for this fixture as given — a purely local, no-business-meaning
counter is exactly the case `compose-development`'s state-hoisting
section says is fine to keep local. The actual defect is the missing
`mutableStateOf`/`remember`, not the ownership location.
