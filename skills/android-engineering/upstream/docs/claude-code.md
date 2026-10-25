# Claude Code

See `integrations/claude-code/README.md` for the placement mechanics.
This page is the narrative version.

Claude Code discovers skills from `.claude/skills/<name>/SKILL.md` in
whatever project it's working in — no configuration step beyond having
run `install.sh`, which places a copy of every skill and workflow there.
Once installed, `android-architecture`, `implement`, `code-review`, and
the rest appear in Claude Code's normal skill listing for that project,
exactly like any other skill it has access to.

## Using the intended workflow in a session

Ask for something Android-shaped ("add a screen that lets a user view
their order history") and let the skill descriptions do the routing —
they're written to trigger on request shape (see
`docs/authoring-skills.md`), so `requirements-grill` and `feature-to-spec`
should activate on their own for anything with real ambiguity. If Claude
Code doesn't pick up a workflow you expect, you can invoke it directly by
name.

## Reviewing a diff

`code-review` (the workflow) applies `android-code-review` (the skill's
taxonomy) — ask for a review of a diff/PR and expect a report shaped like
`templates/code-review.md`, findings ranked Critical → Major → Minor →
Suggestion, with a Positive finding when one genuinely applies.

## If this project already has a skill with a matching name

`install.sh` warns about this but doesn't try to auto-resolve it — see
`integrations/claude-code/README.md`'s "Name collisions" section. Decide
per-name whether to rename the project's own skill or treat the shared
one as authoritative for that name.
