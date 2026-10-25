# Changelog

All notable changes to this repository are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/).

Versioning applies to the repository's *content contract* — the skills,
workflows, templates, and the install/update mechanism — not to any
consuming Android app's own version.

- **MAJOR** — a skill/workflow's guidance changes in a way that contradicts
  prior guidance, or the install/update directory contract changes.
- **MINOR** — a new skill, new workflow, or materially expanded coverage of
  an existing one.
- **PATCH** — wording fixes, corrected examples, non-behavioral doc changes.

## [Unreleased]

## [0.2.1] - 2026-08-24

### Changed

- Relicensed from an internal/proprietary notice to MIT for public
  release.
- Genericized the Design System example branding (`ElmTheme`/`ElmButton`,
  the `elm-android-design-system` repo name) to `AcmeTheme`/`AcmeButton`/
  `acme-design-system`, so the public repo doesn't carry any specific
  organization's internal project naming.
- `SECURITY.md`'s reporting section now points at GitHub issues/security
  advisories instead of an internal team contact, reflecting public
  distribution.

## [0.2.0] - 2026-08-24

### Added

- A Design System skills domain under `skills/design-system/`: eight new
  skills — `design-system-consumer`, `design-system-boundary`,
  `design-system-component-authoring`, `design-tokens`,
  `compose-theming`, `design-system-accessibility`, `rtl-localization`,
  `design-system-review` — covering how to consume a shared Android
  Design System from an application and how to build one correctly. The
  actual Design System implementation remains a separate repository
  (`acme-design-system`); this repository contains only
  engineering knowledge and workflow guidance about it.
- A new workflow, `workflows/design-system-component`, sequencing the new
  skills together with the existing `requirements-grill`, `implement`,
  and `code-review` workflows for building a component end to end.
- A new template, `templates/component-specification.md`.
- `docs/design-system.md`.
- Design System sections in `AGENTS.template.md`, `CLAUDE.template.md`,
  and `README.md`.

### Fixed

- `install.sh`/`update.sh`'s `.claude/skills/` placement now discovers
  skill/workflow directories recursively (any `SKILL.md` under `skills/`
  or `workflows/`, at any nesting depth) instead of a one-level glob —
  required to support `skills/design-system/`'s grouping folder, and
  backward compatible with the existing flat layout.
- `scripts/lib.sh`'s lock-file value reader no longer returns a leading
  space (was silently breaking `update.sh`'s up-to-date check), and its
  diff-summary output now prints clean relative paths for added/removed
  files instead of the raw `diff --brief` text.

## [0.1.0] - 2026-08-24

### Added

- Initial release. Eight engineering-knowledge skills: `android-architecture`,
  `compose-development`, `kotlin-coroutines-flow`, `android-testing`,
  `android-security`, `gradle-build`, `kmp-development`, `android-code-review`.
- Seven execution workflows: `requirements-grill`, `feature-to-spec`,
  `implementation-plan`, `implement`, `bug-fix`, `refactor`, `code-review`.
- Five spec/report templates.
- `install.sh` / `update.sh` for copying a pinned version into a consuming
  Android repository, with a namespaced `.agent-engineering/` directory,
  a version lock file, and confirm-before-overwrite semantics.
- Claude Code, Codex, and generic SKILL.md-convention integration docs.
- A ten-fixture Android mistake eval suite with a manual
  agent-with-skill-vs-without scoring protocol.
- A minimal `examples/sample-android-project` multi-module skeleton.
- `SECURITY.md` and a CI frontmatter/secret-pattern validation script.
