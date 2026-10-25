<!--
Template for workflows/code-review output. Delete this comment block
once filled in.
-->

# Code Review: <feature/PR name>

**Reviewed diff:** <range/link>
**Source spec/plan:** <link or path>
**Reviewer:** AI review (android-agent-engineering code-review workflow)

## Spec/plan conformance

<Does the diff implement what was approved — no more, no less?>

## Findings

### Critical

- **<title>** — `<file>:<line>` — <what's wrong> — <concrete failure
  scenario> — <what a fix looks like>

### Major

- **<title>** — `<file>:<line>` — ...

### Minor

- **<title>** — `<file>:<line>` — ...

### Suggestion

- **<title>** — `<file>:<line>` — ...

### Positive

- **<title>** — `<file>:<line>` — <what's done well and why it's worth repeating>

<!-- Omit a section entirely if it has no findings, rather than leaving it empty. -->

## Test quality check

<Does the test suite for this diff actually cover the RED/regression/
characterization requirement from the workflow that produced it? Would it
have caught the bug/regression it's meant to guard against?>

## Summary

<One or two sentences: is this ready for human review, or does it need
another pass first?>
