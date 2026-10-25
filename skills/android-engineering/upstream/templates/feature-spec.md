<!--
Template for workflows/feature-to-spec. Copy this into the consuming
project's own spec location (e.g. docs/specs/YYYY-MM-DD-<feature>.md) and
fill it in. Delete this comment block once filled in.
-->

# Feature Spec: <name>

**Status:** Draft | Approved
**Date:** <YYYY-MM-DD>
**Author (agent + human approver):**

## Problem

<The user/business problem, not the solution.>

## Scope

<What this spec covers, concretely.>

## Non-goals

<What this spec deliberately does not cover.>

## Requirements

<Testable statements, derived from requirements-grill.>

## User flows

<Step sequences, including edge-case/failure branches, not just the happy path.>

## Architecture

<Layers/modules touched; point at the relevant skills rather than restating them.>

## Components/modules affected

<Concrete file/module list, as far as known at spec time.>

## State model

<The UI states this feature introduces or changes.>

## Error states

<One entry per distinct failure mode: user-visible behavior + recovery.>

## API/data implications

<New/changed endpoints or fields, marked assumed vs. confirmed; any migration needed.>

## Testing strategy

<Which layers get unit vs. integration vs. Compose UI coverage.>

## Security implications

<Anything touching android-security's scope; state "none" explicitly if none.>

## Acceptance criteria

- [ ] <Independently checkable criterion>
- [ ] <...>

## Open questions

<Anything unresolved; not a failure state, but must be explicit.>
