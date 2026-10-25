# Expected findings: testing/09-missing-regression-test

This fixture is reviewed as a **PR under `workflows/code-review`**, not
implemented fresh — `before.kt` is the reported bug, `after.kt` is the fix
as shipped in the diff being reviewed. The prompt for this fixture: "here
is a bug report and the fix that was shipped for it — review the PR."

## Major

**Major — bug fix shipped with no regression test.** The fix itself is
correct (`>` → `>=` correctly includes the threshold value in the
discount), but the PR adds no test. Per `workflows/bug-fix`'s required
order and `android-testing`'s regression-test definition, every bug fix
needs a test that fails on the original code and passes on the fix —
without one, this exact off-by-one can silently reappear on a future
change to this function (e.g. someone "simplifying" the comparison back
to `>` without realizing it reintroduces the reported bug), with nothing
in the test suite to catch it.

**What the missing test should look like**, for scoring a fix
suggestion against:

```kotlin
@Test
fun `spend exactly at threshold gets the discount`() {
    val pricing = LoyaltyPricing()
    assertEquals(9_000L, pricing.applyDiscount(10_000L)) // was 10_000L before the fix
}
```

## What a false positive looks like here

Flagging the fix's correctness itself (the `>=` change) as wrong is
incorrect — the fix is right. The finding is specifically the missing
regression test, not the fix's logic.
