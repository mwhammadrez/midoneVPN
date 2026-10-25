# android-agent-engineering

A shared, reusable foundation of Android engineering knowledge and
AI-agent execution workflows, meant to be installed into many Android app
repositories and kept up to date centrally.

## What this is

A small library of:

- **Skills** (`skills/`) hold durable Android engineering knowledge: Clean
  Architecture, Compose, coroutines/Flow, testing, security, Gradle,
  Kotlin Multiplatform, and a code-review taxonomy. These describe **how
  we engineer**.
- **Workflows** (`workflows/`) are ordered, gated processes an agent
  follows to turn a request into reviewed, tested, merged code: grilling
  requirements, writing a spec, planning implementation, TDD
  implementation, bug fixing, refactoring, and independent code review.
  These describe **how an agent executes work**.
- **Templates**, an **eval suite** of common Android mistakes, and an
  **installer** that copies a pinned version of all of the above into a
  consuming Android repository without touching that repository's own
  project-specific context.

Every skill and workflow is a plain [Agent Skills](https://github.com/anthropics/skills)-convention
`SKILL.md` file, the same format Claude Code, Codex, and any other
SKILL.md-aware tool already know how to discover. There is one canonical
copy of each; per-agent "integration" is just a placement convention, not
a rewrite (see `docs/architecture.md`).

## What this is not

- **Not autonomous vibe coding.** Nothing here lets an agent design,
  implement, and merge code unattended. The workflow below has two
  mandatory human approval gates before any code is written, and a human
  review gate after the AI review, before merge.
- **Not a framework.** There is no runtime, no plugin system to learn, no
  new DSL. It's markdown plus two shell scripts.
- **Not a source of project-specific truth.** This repo intentionally
  contains no build commands, module maps, or product context for any
  specific app. That stays in each consuming repo's own
  `AGENTS.md`/`CLAUDE.md` (see `AGENTS.template.md`).
- **Not a substitute for a senior engineer's judgment.** Skills encode
  defaults and things to check, not unquestionable rules; a documented,
  reasoned exception in a project's own `AGENTS.md` always wins.

## The intended workflow

```text
Request
  ↓
Requirements Grill        (workflows/requirements-grill)
  ↓
Specification              (workflows/feature-to-spec)
  ↓
Human approval  ───────────────────────────── gate 1
  ↓
Implementation Plan        (workflows/implementation-plan)
  ↓
Human approval  ───────────────────────────── gate 2
  ↓
TDD / Implementation       (workflows/implement, workflows/bug-fix, workflows/refactor)
  ↓
Automated verification     (tests, lint, build — run by the agent, checked by the human)
  ↓
AI Code Review              (workflows/code-review + skills/android-code-review)
  ↓
Human Code Review  ────────────────────────── gate 3
  ↓
PR
```

Humans retain ownership of architecture and of what gets merged, at every
gate. An agent following these workflows should stop and ask rather than
invent product behavior, silently reinterpret a requirement, or weaken a
test to make it pass.

## Repository layout

```text
skills/          Engineering knowledge (8 skills) + skills/design-system/ (8 more, see below)
workflows/        Execution processes (8 workflows, see below)
templates/        Output formats the workflows fill in
integrations/     Thin per-agent placement docs (Claude Code, Codex, generic)
evals/            10 fixtures demonstrating common Android mistakes + scoring protocol
examples/         A minimal sample Android project used for docs and install smoke-testing
docs/             Getting started, architecture, and adoption guides
install.sh        Install a pinned version into another Android repo
update.sh         Upgrade an already-installed copy, with a confirm-and-diff step
```

See `docs/architecture.md` for the full rationale and `docs/getting-started.md`
to install this into a project in a few minutes.

## Skills (8)

| Skill | Covers |
|---|---|
| `android-architecture` | Clean Architecture layering, dependency direction, DI, modularization |
| `compose-development` | State hoisting, stability, side effects, lifecycle-aware collection, a11y, perf |
| `kotlin-coroutines-flow` | Structured concurrency, dispatchers, cancellation, `StateFlow`/`SharedFlow` |
| `android-testing` | TDD, unit/Compose/coroutine testing, test doubles, testing pyramid |
| `android-security` | Keystore, secure storage, logs/screenshots, deep links, WebView, exported components |
| `gradle-build` | Version catalogs, convention plugins, AGP/Kotlin upgrades, build performance |
| `kmp-development` | KMP module boundaries, `expect`/`actual`, shared-code testing |
| `android-code-review` | The Critical/Major/Minor/Suggestion/Positive review taxonomy |

## Workflows (8)

`requirements-grill` → `feature-to-spec` → `implementation-plan` →
`implement` (or `bug-fix` / `refactor`) → `code-review`, plus
`design-system-component` for building inside a Design System repository
(see below).

## Design System skills (8)

```
Engineering Skills
    ↓
How we build Android software

Design System Skills
    ↓
How we build and consume consistent UI

Design System Repository (acme-design-system, separate)
    ↓
The actual reusable Android UI implementation
```

This repository never contains the actual Design System implementation.
It only carries the engineering knowledge for consuming it correctly
(from an application) and building it correctly (inside the Design
System repository itself). Both live under `skills/design-system/`:

| Skill | For | Covers |
|---|---|---|
| `design-system-consumer` | Applications | Search-first discipline, never forking/copying components, tokens over raw values |
| `design-system-boundary` | Either | Does this UI belong in the app or the shared Design System |
| `design-system-component-authoring` | Design System repo | API design, avoiding excessive Booleans, slots, controlled components |
| `design-tokens` | Either | Primitive → semantic → component token layering |
| `compose-theming` | Design System repo | `MaterialTheme`/`CompositionLocal` mechanics, light/dark, the theme entry point |
| `design-system-accessibility` | Design System repo | Accessible behavior as the component default, not a per-use repair |
| `rtl-localization` | Either | Arabic/RTL/localization as first-class, not an afterthought |
| `design-system-review` | Design System repo | PR review, reusing `android-code-review`'s taxonomy plus Design-System-specific axes |

See `docs/design-system.md` for the full picture, including the
`design-system-component` workflow that sequences these together with
the existing `implement`/`code-review` workflows.

## Installing into an Android project

```bash
git clone https://github.com/siifii/android-agent-engineering
cd android-agent-engineering
./install.sh /path/to/your-android-project
```

This copies the current pinned version into
`your-android-project/.agent-engineering/`, wires up `.claude/skills/` for
Claude Code, and adds a marked block to that project's `AGENTS.md` (and
optionally `CLAUDE.md`) pointing at it. Nothing outside those locations is
touched. Re-run `update.sh` from the same clone later to see what changed
and confirm before upgrading. Full details: `docs/getting-started.md`.

## What each consuming repository still owns

This repository deliberately contains no proprietary or project-specific
information. Each Android app repository keeps, in its own
`AGENTS.md`/`CLAUDE.md` and docs:

- Its own architecture decisions and any deliberate, documented exceptions
  to a skill's default guidance.
- Module descriptions, build/test commands, and local dev setup.
- App-specific constraints (min SDK, target markets, compliance regime).
- Product and domain context.

## Versioning

Semantic Versioning (see `CHANGELOG.md`). Consuming projects pin a version
via the ref recorded in their own `.agent-engineering/INSTALLED` lock
file, written by `install.sh`.

## Security

See `SECURITY.md`. Short version: no secrets ever go into this repo,
`install.sh`/`update.sh` never execute fetched code, and any third-party
skill/plugin/MCP server is treated as a supply-chain dependency that gets
reviewed and pinned before use.

## Contributing / authoring a new skill or workflow

See `docs/authoring-skills.md`.

## License

MIT, see `LICENSE`. Use it, fork it, adapt it for your own team.
