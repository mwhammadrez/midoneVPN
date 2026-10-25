# Adopting this in one Android project

## What you get after `./install.sh /path/to/project`

- `.agent-engineering/{skills,workflows,templates}/` — the canonical copy.
- `.claude/skills/` — Claude Code discovery placement.
- A marked block in `AGENTS.md` (and optionally `CLAUDE.md`) pointing at
  it — see `docs/architecture.md` for exactly what's tracked.

## What stays yours

This repo intentionally ships with no project-specific content. Your
project's own `AGENTS.md`/`CLAUDE.md`, **below** the marked block, is
where you keep:

- Architecture decisions specific to this app, including any deliberate,
  documented exception to a skill's default (a skill encodes a default,
  not an unquestionable rule — see each skill's own framing).
- Module map, build/test commands, local dev setup.
- App-specific constraints: `minSdk`, target markets, compliance regime.
- Product/domain context.

`install.sh`/`update.sh` never write below the `<!-- END
android-agent-engineering -->` marker — that boundary is what makes
"never overwrites project-specific context without confirmation" a
guarantee rather than a hope.

## Recording a deliberate exception

Use `templates/architecture-decision.md` for anything worth remembering
beyond a single feature — e.g. "this module intentionally keeps a
`Context` reference in what would otherwise be `domain`, because X." Its
"Relationship to android-agent-engineering skills" section is exactly for
this: name which skill's default you're deviating from and why, so a
future review (human or agent) trusts the exception instead of flagging
it as a violation every time.

## Keeping up to date

```bash
cd android-agent-engineering   # pull latest first
./update.sh /path/to/project
```

Shows what changed per subdirectory before applying anything, and updates
`.agent-engineering/INSTALLED`. Pin to a specific version with `--ref` if
your team wants to control exactly when a change lands rather than always
taking the latest tag.

## If a name collides with a skill you already have

See `integrations/claude-code/README.md`'s "Name collisions" section —
`install.sh` will show a diff and ask before overwriting; read it before
confirming.
