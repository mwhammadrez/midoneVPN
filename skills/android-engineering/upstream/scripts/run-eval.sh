#!/usr/bin/env bash
# Prints the manual evaluation protocol for one eval fixture.
# This does not run an agent for you — see evals/README.md for why the
# harness is a documented manual protocol in v0.1, not an automated
# runner.
#
# Usage: ./scripts/run-eval.sh evals/compose/01-state-misuse
set -euo pipefail

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FIXTURE="${1:-}"

[[ -n "$FIXTURE" ]] || { echo "Usage: $0 <path-to-eval-fixture>" >&2; exit 1; }
FIXTURE_DIR="$REPO_DIR/$FIXTURE"
[[ -d "$FIXTURE_DIR" ]] && FIXTURE_DIR="$FIXTURE_DIR" || FIXTURE_DIR="$FIXTURE"
[[ -d "$FIXTURE_DIR" ]] || { echo "No such fixture directory: $FIXTURE" >&2; exit 1; }

before_file="$(find "$FIXTURE_DIR" -maxdepth 1 -name 'before.*' | head -n1)"
findings_file="$FIXTURE_DIR/expected-findings.md"

cat <<EOF
Eval fixture: ${FIXTURE_DIR#"$REPO_DIR"/}

1. RUN A (baseline, no skill):
   Start a fresh agent session with android-agent-engineering NOT
   installed/available. Ask it to review (or fix, per the fixture) the
   file below. Save its full output.

     $before_file

2. RUN B (with skill):
   Start a fresh agent session with this repository's relevant skill
   available (see the fixture's category — compose-development,
   kotlin-coroutines-flow, android-architecture, android-testing, or
   android-security — plus android-code-review for a review task). Give
   it the identical prompt as Run A. Save its full output.

3. SCORE both outputs against the expected findings:

     $findings_file

   using evals/scorecard-template.md, across: issue detection, correct
   fixes, false positives, test quality, architecture compliance,
   security detection (whichever apply to this fixture's category).

4. Record both scorecards next to this fixture (or in your own eval log)
   so results are comparable across skill/prompt revisions over time.
EOF
