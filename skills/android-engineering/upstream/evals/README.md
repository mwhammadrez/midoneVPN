# Eval suite

Ten small fixtures, each demonstrating one common Android mistake, used to
check whether a skill in this repository actually changes agent behavior
for the better — not just whether the skill *reads* well.

## Why a manual protocol in v0.1

A fully automated agent-runner (spin up two isolated agent sessions, feed
them each fixture, score the output programmatically) is the obvious
long-term shape of this suite, and is deliberately **out of scope for
v0.1** — building a reliable automated harness is real infrastructure work
that would compete with getting the skills themselves right first. The
protocol below is manual, but structured enough to produce comparable,
repeatable scores across skill revisions.

## Fixtures

| # | Path | Mistake |
|---|---|---|
| 1 | `compose/01-state-misuse` | Compose state not hoisted/held in `mutableStateOf` |
| 2 | `compose/02-oversized-composable` | One composable doing too much |
| 3 | `coroutines/03-lifecycle-unaware-flow` | `Flow` collected without lifecycle awareness |
| 4 | `coroutines/04-coroutine-leak` | Unmanaged `CoroutineScope`/`GlobalScope` |
| 5 | `coroutines/05-dispatcher-misuse` | Blocking call with no dispatcher switch |
| 6 | `coroutines/06-broken-cancellation` | Swallowed `CancellationException` |
| 7 | `architecture/07-dto-leak` | DTO exposed to `presentation` |
| 8 | `architecture/08-repository-violation` | ViewModel bypasses the repository interface |
| 9 | `testing/09-missing-regression-test` | Bug fix shipped with no regression test |
| 10 | `security/10-insecure-storage` | Auth token in plain `SharedPreferences` |

Each fixture directory has:

- `before.kt` — the broken snippet (the prompt target).
- `expected-findings.md` — what a competent reviewer should catch, mapped
  to `android-code-review`'s Critical/Major/Minor/Suggestion/Positive
  taxonomy.
- `after.kt` — a corrected version, for scoring fix quality (not just
  detection).

## Protocol: agent-without-skill vs. agent-with-skill

For each fixture:

1. **Baseline run** — a fresh agent session with android-agent-engineering
   **not** installed/available. Give it `before.kt` and a review or
   fix-it prompt appropriate to the fixture (see
   `scripts/run-eval.sh <fixture-path>` for the exact framing). Save the
   full transcript/output.
2. **Skill run** — a fresh agent session with the relevant skill(s)
   available (this repository installed, or just the specific
   skill/workflow file provided directly). Identical prompt. Save the
   full output.
3. **Score both** against `expected-findings.md` using
   `scorecard-template.md`, across:
   - **Issue detection** — did it find the core mistake, at the right
     severity?
   - **Correct fixes** — if asked to fix, does the fix actually resolve
     the root cause (compare to `after.kt`), not just silence a symptom?
   - **False positives** — did it flag something that isn't actually
     wrong, or invent a fix for a non-issue?
   - **Test quality** — if it added/changed a test, does it actually
     exercise the bug (see `android-testing`)?
   - **Architecture compliance** — does a suggested fix follow
     `android-architecture`'s layering rather than patching around it?
   - **Security detection** — for fixture 10 especially, but also whether
     any fixture's fix accidentally introduces a security issue while
     fixing something else.
4. Keep both scorecards next to the fixture (or in your team's own eval
   log) so results are comparable across skill revisions over time — a
   skill wording change that *lowers* a score on its own fixture is a
   real regression worth noticing.

## What a good result looks like

The skill run should score meaningfully higher on issue detection and
fix correctness than the baseline run, with **no increase** in false
positives — a skill that makes an agent flag more real issues but also
invent more fake ones isn't a net improvement. If a skill run doesn't
outperform baseline on its own fixture, that's a signal the skill's
guidance for that mistake needs to be sharper, not that the fixture is
wrong.
