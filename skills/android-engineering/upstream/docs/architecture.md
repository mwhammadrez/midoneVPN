# Architecture

## The split this repo is built on

`skills/` = engineering knowledge ("how we engineer"). `workflows/` =
agent execution process ("how an agent executes work"). Both are plain
[Agent Skills](https://github.com/anthropics/skills) `SKILL.md` files —
same frontmatter (`name`, `description`), same discovery mechanism, for
every consuming tool. A workflow's `SKILL.md` references the skills it
needs by path rather than restating their content (e.g.
`workflows/code-review` points at `skills/android-code-review` for the
finding taxonomy instead of redefining it) — this is what keeps the
"don't duplicate content across integrations" constraint honest even
*within* the repo, not just across Claude/Codex.

## Why workflows are also `SKILL.md`, not a separate format

A second format would mean either every consuming tool needs custom
handling for it, or workflows silently don't get discovered by anything
that only understands `SKILL.md`. Reusing the same convention for both
means one discovery mechanism does the whole job — see this repository's
own `implement`/`bug-fix`/etc. showing up as ordinary skills the moment
they're placed under `.claude/skills/`.

## Why the installer copies instead of symlinking

A symlink into a local clone doesn't survive `git clone` on another
machine or in CI — every consuming repo needs the content to actually be
present in its own checkout. `install.sh`/`update.sh` therefore always
copy, scoped to a single namespaced directory
(`.agent-engineering/`) plus the `.claude/skills/` placement and a marked
block in `AGENTS.md`/`CLAUDE.md`. Nothing else is ever touched — see the
"Never overwrites project-owned content" guarantee in
`docs/project-adoption.md`.

## The lock file

`.agent-engineering/INSTALLED` is a flat `key=value` file (no JSON, so no
`jq` dependency for something this simple):

```
VERSION=0.1.0
REF=v0.1.0
COMMIT=<sha>
INSTALLED_AT=<UTC timestamp>
SOURCE=local:/path/to/clone   (or a git URL, in remote mode)
```

`update.sh` reads `COMMIT` to decide whether there's anything to do, and
`REF`/`SOURCE` to know where to fetch from again.

## Why no git submodules

A submodule ties a consuming repo's history and checkout state to this
repo's own commit graph, and its update ergonomics (`git submodule
update --remote`, detached-HEAD surprises) are exactly the kind of
friction this repo is trying to avoid for a piece of shared, versioned,
markdown-only content. Copying a pinned, versioned snapshot gets the same
reproducibility (the lock file records exactly what commit was installed)
without coupling the two repos' git histories together.

## Optional-by-design: third-party skills and MCP

See `SECURITY.md` for the full reasoning. In short: generic process-skill
packs (Superpowers, Matt Pocock's skills) and any MCP server are
referenced, never vendored or required — every workflow that mentions one
is written to degrade gracefully to "ask/flag as open question" when it's
absent.
