<!-- BEGIN android-agent-engineering -->
## Shared Android engineering skills and workflows

This project has `android-agent-engineering` installed. Its skills and
workflows are copied to `.claude/skills/` by `install.sh`, so Claude Code
discovers and can invoke them the same way it discovers any other
project-level skill — no action needed beyond installing.

A full copy of the same content also lives at `.agent-engineering/` for
other tools (Codex, generic SKILL.md-aware agents) and for reference; see
`.agent-engineering/INSTALLED` for the installed version.

Prefer these workflows for Android engineering work in this repository:
`requirements-grill` → `feature-to-spec` → `implementation-plan` →
`implement` / `bug-fix` / `refactor` → `code-review`. Consult the
`android-architecture`, `compose-development`, `kotlin-coroutines-flow`,
`android-testing`, `android-security`, `gradle-build`, `kmp-development`,
and `android-code-review` skills by topic as they become relevant — they
encode defaults, not unquestionable rules; a documented project-specific
exception below this block wins.

**Design System:** state below this block whether this repository is an
*application* consuming a shared Android Design System, or the Design
System repository itself (default: application, if unstated). If it
consumes one, prefer `design-system-consumer` before writing custom UI
and `design-system-boundary` before deciding a component should be
reusable. If this *is* the Design System repository, use the
`design-system-component` workflow, which sequences
`design-system-component-authoring`, `design-tokens`, `compose-theming`,
`design-system-accessibility`, `rtl-localization`, and
`design-system-review`.

Do not edit `.claude/skills/<android-agent-engineering skill names>` or
`.agent-engineering/` by hand — both are managed by `install.sh`/
`update.sh`. Project-specific architecture, module descriptions, build
commands, and product context belong below this block.
<!-- END android-agent-engineering -->

<!--
Everything below this line is owned by this project, not by
android-agent-engineering. Add your project-specific architecture notes,
module map, build/test commands, app-specific constraints, and product
context here. install.sh/update.sh will never modify anything below the
END marker above.
-->
