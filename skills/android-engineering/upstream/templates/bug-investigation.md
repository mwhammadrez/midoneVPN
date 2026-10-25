<!--
Template for workflows/bug-fix steps 1-2 (Reproduce, Root cause
analysis). Delete this comment block once filled in.
-->

# Bug Investigation: <short description>

**Reported by / observed:**
**Date:**

## Reproduction

- **Steps:**
- **Expected behavior:**
- **Actual behavior:**
- **Conditions required:** <device/OS/build variant/data state/timing>
- **Reproducible?** Yes / Intermittent / Not yet reproduced — <if not, say so explicitly and treat root cause as a hypothesis>

## Root cause analysis

- **Mechanism:** <the actual cause, with evidence — a log trace, a code
  path read, a failing assertion — not a guess>
- **Relevant skill(s):** <e.g. kotlin-coroutines-flow (race condition),
  compose-development (stale remember), android-architecture (mapping bug)>
- **Why it wasn't caught earlier:** <missing test, untested edge case,
  recent change that introduced it — informs whether step 6 (refactor) is
  justified>

## Characterization / regression test plan

<What test will be written to fail on this exact bug before the fix.>

## Fix scope

<The minimal fix target. If a structural refactor is also justified per
workflows/bug-fix step 6, name it explicitly and get separate sign-off
rather than folding it into this fix silently.>
