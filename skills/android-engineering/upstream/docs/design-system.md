# Design System

## The three-layer picture

```
android-agent-engineering
        ↓
defines HOW developers/agents should work
(engineering skills + design-system skills, both knowledge/process only)

acme-design-system  (separate repository, not part of this one)
        ↓
contains the actual reusable Android UI implementation

Android applications
        ↓
consume acme-design-system
        +
follow android-agent-engineering skills
```

This repository never contains the actual Design System implementation —
no components, no token values, no theme code. `skills/design-system/`
is engineering knowledge about *how* to consume and build a Design
System; the real one lives in its own repository (referred to here as
`acme-design-system`), which does not exist yet and whose module
architecture is intentionally not invented by this repository — see
`workflows/design-system-component`'s Figma/release steps for where that
repository's own decisions plug in.

## Consuming the Design System (from an application)

Read `skills/design-system/design-system-consumer/SKILL.md`. The one
rule that matters: search the Design System before writing custom UI.
Everything else in that skill — never forking components, using tokens
instead of raw values, using the documented API instead of reaching
around it — follows from taking that rule seriously.

## Contributing a component (inside the Design System repository)

Follow `workflows/design-system-component/SKILL.md` end to end. It's a
sequence over existing pieces (`requirements-grill`, `implement`,
`code-review`) plus the Design-System-specific steps in between —
boundary evaluation, API/token/variant design, accessibility, RTL,
documentation via `templates/component-specification.md`.

## Component lifecycle

A component's life, per the workflow above: request → search existing →
boundary evaluation → requirements → design spec → API/token/variant
design → accessibility → RTL → implementation (TDD, via the existing
`implement` workflow) → previews → tests → design review (human) →
engineering review (`workflows/code-review` +
`skills/design-system/design-system-review`) → documentation → release
(the Design System repository's own versioning, out of this repo's
scope).

## Design System boundaries

The recurring question — does this belong in the app or the shared
system — is `skills/design-system/design-system-boundary/SKILL.md`'s
whole job. It deliberately isn't a mechanical rule ("used twice = Design
System"); it weighs reusability, domain knowledge, API stability, and
several other factors together, and defaults to keeping something
application-specific when genuinely uncertain.

## Tokens

`skills/design-system/design-tokens/SKILL.md` defines the
primitive → semantic → component layering; application code (and most
Design System component code) should only ever touch semantic/component
tokens, never raw primitives directly. `skills/design-system/compose-theming/SKILL.md`
covers how those tokens actually flow through `MaterialTheme`/
`CompositionLocal` in Compose.

## Accessibility

`skills/design-system/design-system-accessibility/SKILL.md` — the rule
that matters: accessible behavior is the component's *default*, not
something an application repairs per use. Builds on
`skills/compose-development`'s general Compose accessibility section
rather than restating it.

## RTL and localization

`skills/design-system/rtl-localization/SKILL.md` — Arabic and RTL are
first-class requirements, covering start/end vs. left/right, directional
icon mirroring, Arabic typography, mixed-direction text, numbers/dates/
currencies, pluralization, and text-expansion tolerance.

## Review process

`skills/design-system/design-system-review/SKILL.md` applies
`skills/android-code-review`'s existing Critical/Major/Minor/Suggestion/
Positive taxonomy and general checklist, adding only the axes specific to
a Design System PR: API misuse-resistance, genuine reusability, token
hygiene, accessibility/localization compliance, per-component state
coverage, and documentation accuracy.

## Which side of the boundary is this repository?

Neither `design-system-consumer` nor `design-system-boundary` can tell,
from the skill text alone, whether the repository they're installed into
is an application or the Design System repository itself. State this
explicitly in the consuming repository's own `AGENTS.md`/`CLAUDE.md`,
below the managed block — see `AGENTS.template.md`'s Design System
section, added by this repository's install/update mechanism the same
way every other project-specific note is kept separate from the managed
content (see `docs/project-adoption.md`).

## Future work (not built in this version)

Deliberately deferred, in rough order of likely value once
`acme-design-system` exists to build against:

- **Figma MCP/integration** — automated design-to-code context so a
  component's spec can be generated from (or checked against) its Figma
  source. `templates/component-specification.md`'s Figma field is a
  manual placeholder today.
- **Screenshot / visual regression testing** — Compose screenshot testing
  for catching unintended visual drift across the token/theme layers.
- **Design-token generation and Figma token synchronization** — keeping
  primitive/semantic token values in code in sync with their design
  source automatically instead of by hand.
- **Component catalog generation** — an app that renders every component/
  variant/state for browsing, generated from the components themselves
  rather than hand-maintained.
- **Documentation generation** — generating (or at least validating)
  `templates/component-specification.md` content from the component's
  actual API and previews.
- **Accessibility automation** — automated checks (contrast, missing
  content descriptions, touch target size) as a CI gate on the Design
  System repository, complementing `design-system-review`'s manual
  checklist.
- **Design System analytics / adoption tracking** — visibility into which
  components/tokens are actually used across consuming applications, to
  inform deprecation and boundary decisions (`design-system-boundary`)
  with real usage data instead of judgment alone.
- **Design-system-specific eval fixtures** — extending `evals/` (see its
  README) with fixtures like token hardcoding, an inaccessible custom
  component, or a broken-RTL layout, once the real Design System
  repository exists to build realistic fixtures against.

None of these are assumed by any skill or workflow in this version —
every one degrades gracefully to "do it manually" when absent, the same
principle this repository already applies to optional MCP servers (see
`SECURITY.md`).
