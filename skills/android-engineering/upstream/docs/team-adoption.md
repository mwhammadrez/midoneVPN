# Adopting this across a team

## Rolling out to multiple repos

Run `./install.sh <path>` once per Android repository the team owns.
There's no central registration step — each repo's `.agent-engineering/INSTALLED`
lock file is the only place "which version is where" is tracked, so
if you want a team-wide view of that, it's a simple script away (read
`VERSION`/`REF` out of each repo's lock file) rather than something this
repository needs to build and maintain itself in v0.1.

## Keeping repos in sync vs. letting them drift deliberately

`update.sh`'s confirm-and-diff flow means upgrading is always a deliberate
per-repo action, not an automatic push. A team can choose to standardize
on "upgrade everything together on each `android-agent-engineering`
release" or let individual repos upgrade on their own schedule — both are
supported equally; this repo doesn't enforce either policy.

## Recommended workflow for the team's own PRs into this repo

Same shape as the workflow this repo teaches for Android features:
`requirements-grill` → `feature-to-spec` (what guidance is changing and
why) → human approval → the actual `SKILL.md` edit → `scripts/validate-skills.sh`
→ human review → merge. A change to a skill's guidance is a real change
to how the whole team's agents behave — treat it with the same rigor as
a change to production code, not as "just docs."

## Running the eval suite as a gate

Not required for every change, but worth running `evals/README.md`'s
protocol against the specific fixture(s) related to a skill before
shipping a meaningfully reworded version of that skill — see
`evals/README.md`'s "What a good result looks like" for the bar (skill
run should outperform baseline on its own fixture, with no increase in
false positives).

## Onboarding a new team member

Point them at the root `README.md` for the what/why, then
`docs/getting-started.md` to install into a repo they're already working
in. They don't need to read every skill up front — the whole point of the
skill-description-triggered design (`docs/authoring-skills.md`) is that
the right skill surfaces when it's relevant, not that anyone memorizes
the catalog.
