# Codex

See `integrations/codex/README.md` for the mechanics and why the
`AGENTS.md` pointer, not assumed auto-discovery, is this repo's reliable
baseline for Codex. This page is the narrative version.

After `install.sh`, the consuming project's `AGENTS.md` has a marked
`android-agent-engineering` block naming which `SKILL.md` to read for
which kind of task (new feature → `requirements-grill` then
`feature-to-spec`; bug → `bug-fix`; review → `code-review`, and so on).
Codex reads `AGENTS.md` as repository instructions at the start of a
session, so this works even without any Codex-specific skill-discovery
behavior — worst case, Codex reads the pointer and then reads the named
`SKILL.md` file directly with its ordinary file tools.

If your Codex setup does support project-local `SKILL.md` discovery,
point it at `.agent-engineering/skills/` and `.agent-engineering/workflows/`
directly — same files Claude Code reads from `.claude/skills/`, no
Codex-specific copy.

For org-wide availability across every repo (not just installed-per-repo
projects), see the "Org-wide availability" section of
`integrations/codex/README.md` — a manual, team-level choice, outside
`install.sh`'s scope.
