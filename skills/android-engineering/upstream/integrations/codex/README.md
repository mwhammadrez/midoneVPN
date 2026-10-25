# Codex integration

Codex's reliable, well-established entry point is `AGENTS.md` at a
project's root — Codex reads it as repository instructions at the start
of a session. Codex also supports a `SKILL.md`-based plugin/skills
mechanism, but this repository does not assume Codex will
auto-discover an arbitrary project-local `skills/`/`workflows/` directory
outside that plugin system, since that behavior isn't something we could
confirm from this machine's setup. The integration below is written to
work regardless of whether auto-discovery happens to apply.

## What `install.sh` does for Codex

Inserts (or creates) a marked block in the consuming project's
`AGENTS.md`:

```markdown
<!-- BEGIN android-agent-engineering -->
## Shared Android engineering skills and workflows
...points at .agent-engineering/skills and .agent-engineering/workflows,
and lists which SKILL.md to read for which kind of task...
<!-- END android-agent-engineering -->
```

Because Codex reads `AGENTS.md` explicitly at session start, this block
is a reliable baseline: even a Codex build with no special SKILL.md
discovery will see the pointer and can read the relevant `SKILL.md` file
directly with its normal file-reading tools when a matching task comes
up. See `AGENTS.template.md` at the repository root for the exact block
content.

## If your Codex setup does support project-local skill discovery

Point it at `.agent-engineering/skills/` and `.agent-engineering/workflows/`
directly — both directories are plain `SKILL.md`-per-folder, identical in
shape to what Claude Code reads from `.claude/skills/`. There is nothing
Codex-specific to install beyond the `AGENTS.md` block; do not create a
second, Codex-flavored copy of any skill's content.

## Org-wide availability across repos (optional)

If a team wants these skills available to Codex across every repo without
running `install.sh` in each one, copy or symlink this repository's
`skills/` and `workflows/` directories into wherever the team's Codex
setup looks for user-level plugin skills (see Codex's own plugin
documentation for the exact path convention in your Codex version). This
is a manual, team-level choice outside `install.sh`'s scope — `install.sh`
only ever writes into the target Android project repo, never into a
user's global Codex configuration.
