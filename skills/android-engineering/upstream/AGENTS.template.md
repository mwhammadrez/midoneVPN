<!-- BEGIN android-agent-engineering -->
## Shared Android engineering skills and workflows

This project has `android-agent-engineering` installed at
`.agent-engineering/` (version: see `.agent-engineering/INSTALLED`).

Before doing Android engineering work in this repository, check whether
one of these applies and read the matching `SKILL.md` first:

- **Starting a new feature or an ambiguous request** →
  `.agent-engineering/workflows/requirements-grill/SKILL.md`, then
  `.agent-engineering/workflows/feature-to-spec/SKILL.md`
- **You have an approved spec** →
  `.agent-engineering/workflows/implementation-plan/SKILL.md`
- **Implementing new behavior** →
  `.agent-engineering/workflows/implement/SKILL.md`
- **Fixing a bug** → `.agent-engineering/workflows/bug-fix/SKILL.md`
- **Refactoring existing code** →
  `.agent-engineering/workflows/refactor/SKILL.md`
- **Reviewing a diff/PR** →
  `.agent-engineering/workflows/code-review/SKILL.md` (uses
  `.agent-engineering/skills/android-code-review/SKILL.md`)

Engineering-knowledge skills to consult by topic:
`android-architecture`, `compose-development`, `kotlin-coroutines-flow`,
`android-testing`, `android-security`, `gradle-build`, `kmp-development` —
each at `.agent-engineering/skills/<name>/SKILL.md`.

### Design System

If this project consumes a shared Android Design System — **state which
side of that boundary this repository is on below this block**: an
*application* consuming it, or the Design System repository itself. The
default assumption if unstated is "application."

- **Building UI in an application** → read
  `.agent-engineering/skills/design-system/design-system-consumer/SKILL.md`
  first, before writing any custom UI.
- **Deciding whether a component belongs in the app or the shared Design
  System** →
  `.agent-engineering/skills/design-system/design-system-boundary/SKILL.md`
- **Creating or changing a component inside the Design System repository
  itself** →
  `.agent-engineering/workflows/design-system-component/SKILL.md`, which
  sequences
  `.agent-engineering/skills/design-system/design-system-component-authoring/SKILL.md`,
  `design-tokens`, `compose-theming`, `design-system-accessibility`, and
  `rtl-localization` (all under `.agent-engineering/skills/design-system/`).
- **Reviewing a Design System PR** →
  `.agent-engineering/skills/design-system/design-system-review/SKILL.md`
  (applies `android-code-review`'s taxonomy plus Design-System-specific
  axes).

These skills encode defaults, not unquestionable rules. Where this
project's own instructions below explicitly document an exception, this
project's instructions win.

Do not edit anything under `.agent-engineering/` by hand — it is managed
by `install.sh`/`update.sh` from the central repository. Project-specific
architecture, module descriptions, build commands, and product context
belong below this block, not inside `.agent-engineering/`.
<!-- END android-agent-engineering -->

<!--
Everything below this line is owned by this project, not by
android-agent-engineering. Add your project-specific architecture notes,
module map, build/test commands, app-specific constraints, and product
context here. install.sh/update.sh will never modify anything below the
END marker above.
-->
