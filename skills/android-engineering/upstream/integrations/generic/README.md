# Generic SKILL.md-compatible agent integration

Any tool that discovers skills by walking a directory tree for
`SKILL.md` files (with `name` + `description` frontmatter, per the open
[Agent Skills](https://github.com/anthropics/skills) convention) can point
its discovery mechanism directly at:

```
<project>/.agent-engineering/skills/
<project>/.agent-engineering/workflows/
```

Both directories have the identical shape: one folder per skill/workflow,
each containing `SKILL.md` as the entry point and, for a few skills, a
`references/` subfolder with material the `SKILL.md` body links to on
demand. There is no tool-specific format conversion needed — this is the
single canonical copy every integration in this repository reads from.

## Cursor (documented recipe, not scripted)

Cursor's native convention is `.cursor/rules/*.mdc` (MDC frontmatter +
body), not `SKILL.md`. This repository does not generate `.mdc` files as
part of `install.sh` in v0.1 — it's unverified against a real Cursor
install and easy to get subtly wrong (e.g. the trigger-condition
frontmatter fields). The manual recipe, if your team wants Cursor
picking up this content:

1. After running `install.sh`, for each skill/workflow you want Cursor to
   see, create a thin stub at `.cursor/rules/<name>.mdc`:

   ```mdc
   ---
   description: <copy the skill's SKILL.md description field>
   alwaysApply: false
   ---
   See `.agent-engineering/skills/<name>/SKILL.md` (or
   `.agent-engineering/workflows/<name>/SKILL.md`) for the full guidance.
   ```

2. Keep the stub to a pointer, exactly like the `AGENTS.md`/`CLAUDE.md`
   blocks — never paste the skill's full body into the `.mdc` file, or
   you now have two copies to keep in sync.

If your team adopts this at scale, consider scripting it as a follow-up
contribution to `install.sh` (see `docs/authoring-skills.md`) rather than
maintaining it by hand per project.

## Any other SKILL.md-aware tool

Same principle: point the tool at `.agent-engineering/skills/` and
`.agent-engineering/workflows/`, or — if the tool requires its own
top-level directory convention (like Claude Code's `.claude/skills/`) —
write the thinnest possible adapter that either symlinks/copies those
directories into place or emits pointer stubs, never a rewritten copy of
the content itself.
