# Claude Code integration

No adapter format needed — Claude Code discovers project-level skills
from `.claude/skills/<name>/SKILL.md` in the repository it's working in,
the same way it discovers any other skill (this is exactly the mechanism
that surfaced this repository's own design-process skills while it was
being built).

`install.sh` places a copy of every `skills/` and `workflows/` entry at
`.claude/skills/<name>/` in the consuming repository, so once installed,
Claude Code lists `android-architecture`, `implement`, `code-review`, etc.
alongside any other skill it already has access to — no configuration
step, no restart required beyond starting a new session in that project.

## What gets placed where

```
<project>/.claude/skills/
├── android-architecture/SKILL.md (+ references/)
├── compose-development/SKILL.md (+ references/)
├── kotlin-coroutines-flow/SKILL.md
├── android-testing/SKILL.md
├── android-security/SKILL.md (+ references/)
├── gradle-build/SKILL.md
├── kmp-development/SKILL.md
├── android-code-review/SKILL.md (+ references/)
├── requirements-grill/SKILL.md
├── feature-to-spec/SKILL.md
├── implementation-plan/SKILL.md
├── implement/SKILL.md
├── bug-fix/SKILL.md
├── refactor/SKILL.md
└── code-review/SKILL.md
```

A full copy also exists at `.agent-engineering/`, which is what
`update.sh` diffs against and what other tools (Codex, generic) read
from — `.claude/skills/` is a placement, not a second source of truth.

## Name collisions

If a consuming project already has its own `.claude/skills/<name>/`
directory using one of these names (most likely `android-testing` or
`android-security`, since those are generic enough names a project might
already have picked independently), `install.sh` will overwrite it after
a diff preview and confirmation — read that diff carefully before
confirming, and rename one side if they're meant to be different things.

## Optional: CLAUDE.md pointer

`.claude/skills/` placement alone is enough for discovery. A project can
additionally run the CLAUDE.md step in `install.sh` to add an explicit
pointer block — mainly useful as human-readable documentation of what's
installed, not required for Claude Code itself to find the skills.

## Updating

`update.sh` re-syncs `.claude/skills/<name>/` for any skill/workflow whose
content changed, in step with `.agent-engineering/`. It only touches
directories matching this repository's skill/workflow names — it never
touches an unrelated skill you've added to the same project.
