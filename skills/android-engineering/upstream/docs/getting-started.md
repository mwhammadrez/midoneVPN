# Getting started

## Install into an Android project

```bash
git clone https://github.com/siifii/android-agent-engineering
cd android-agent-engineering
./install.sh /path/to/your-android-project
```

You'll be asked to confirm each thing it writes:

1. `.agent-engineering/{skills,workflows,templates}/`: the canonical
   copy, diffed against whatever's already there.
2. `.claude/skills/`: a copy of every skill/workflow, for Claude Code's
   native discovery.
3. `AGENTS.md`: a marked block pointing Codex/generic agents at
   `.agent-engineering/`. Created from `AGENTS.template.md` if missing,
   appended if the file exists with no block yet, replaced in place if
   the block already exists.
4. `CLAUDE.md`: optional, same idea, mainly useful as a human-readable
   pointer since `.claude/skills/` already makes things discoverable.

Nothing outside those four locations is touched. Re-run with `--yes` to
skip prompts in a script/CI context (still shows the diff summary first).

## Try it with Claude Code

Open the project you just installed into. Ask Claude Code to do something
Android-shaped, like "add a screen that...". It should pick up
`requirements-grill`/`feature-to-spec` on its own; the skill descriptions
are written to trigger on exactly this kind of request (see
`docs/authoring-skills.md` for why that matters).

## Try it with Codex

Same project. Codex reads `AGENTS.md` at session start and will see the
marked block pointing it at `.agent-engineering/workflows/` and
`.agent-engineering/skills/`. Ask it to read the relevant `SKILL.md`
before Android-shaped work if it doesn't do so on its own.

## Upgrading later

```bash
cd android-agent-engineering   # your original clone, pull latest first
./update.sh /path/to/your-android-project
```

Shows a per-file diff summary against what's currently installed (via
`.agent-engineering/INSTALLED`), asks to confirm before applying, and
updates the lock file. See `docs/architecture.md` for what the lock file
tracks and why nothing is silently overwritten.

## Pinning a version

```bash
./install.sh /path/to/project --ref v0.1.0
```

`REF`/`COMMIT`/`VERSION` are recorded in
`.agent-engineering/INSTALLED` after install; `update.sh` reads that file
to know what "currently installed" means.
