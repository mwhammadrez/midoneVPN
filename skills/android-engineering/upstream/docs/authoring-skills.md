# Authoring a new skill or workflow

## Format

Every skill/workflow is one directory with `SKILL.md` as the entry point:

```yaml
---
name: your-skill-name          # must match the directory name (checked by CI)
description: Use when ...      # what triggers this, written for an agent deciding whether to load it
metadata:
  version: "0.1.0"
---

# Title

Body content.
```

Add a `references/` subfolder only once the main body would otherwise
grow past what's needed for routine use — see `android-architecture`,
`android-security`, and `android-code-review` for the pattern: the main
`SKILL.md` covers the common case, `references/*.md` covers the deep
checklist, linked from the body, loaded only when actually needed.

## Skill vs. workflow

- **Skill** (`skills/`) — durable, technology-scoped knowledge. Triggers
  on *topic* ("Compose state", "coroutine cancellation").
- **Workflow** (`workflows/`) — ordered process with gates. Triggers on
  *verb/intent* ("implement a feature", "fix a bug", "review this diff").
  References the skills it needs by path; never restates their content.

If you're not sure which one you're writing, ask: "is this a fact about
Android engineering, or a sequence of steps an agent should follow?" —
that's the split.

## Writing a good `description`

This is the field every agent/tool uses to decide whether to load the
skill at all — see how specifically `android-performance` (a real
third-party skill found on this machine during this repo's design)
scopes its trigger conditions. Name concrete trigger phrases and the
topic boundary, not a vague summary. Compare:

- Weak: `description: Compose best practices`
- Strong (this repo's actual `compose-development` skill):
  `description: Use when writing or reviewing Jetpack Compose UI - state hoisting, ViewModel ownership of state, immutable UI state, recomposition and stability, remember/rememberSaveable, derivedStateOf, side-effect APIs (...), lifecycle-aware Flow collection, navigation, accessibility, previews, Compose performance, and Compose UI testing.`

## Don't duplicate

Before writing new guidance, check whether an existing skill already
covers it (`grep -r` across `skills/*/SKILL.md` for the topic). If a new
skill needs to touch on something another skill owns, reference it by
name/path rather than restating — this is the same discipline the
workflows already follow toward the skills.

## Validate before committing

```bash
./scripts/validate-skills.sh
```

Checks frontmatter completeness and that `name:` matches the directory,
and runs the best-effort secret-pattern scan (see `SECURITY.md`). CI runs
this on every PR via `.github/workflows/validate.yml`.

## Versioning a change

See `CHANGELOG.md`'s MAJOR/MINOR/PATCH rules. A skill/workflow content
change that contradicts prior guidance is MAJOR; new coverage is MINOR;
wording fixes are PATCH. Bump `VERSION` and add a `CHANGELOG.md` entry as
part of the same change.
