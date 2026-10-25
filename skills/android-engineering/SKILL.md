---
name: android-engineering
description: Use for general Android/Kotlin app engineering — architecture, Compose UI, coroutines/Flow, Gradle build problems, unit/UI testing, security review, code review, and refactoring workflows. Vendor of siifii/android-agent-engineering (MIT). For VPN/proxy clients use the vpn-android skill first.
---

# Android engineering (vendored knowledge base)

Upstream: `siifii/android-agent-engineering` (MIT, see `upstream/LICENSE`). Copied 2026-10-24.
Everything lives under `upstream/`; this file is only the router so the whole library costs one
skill slot.

## How to use

Open exactly one sub-file, not the whole tree.

| Need | File |
|---|---|
| Layering, boundaries, state holders | `upstream/skills/android-architecture/SKILL.md` (+ `references/architecture-checklist.md`) |
| Compose state, recomposition, theming | `upstream/skills/compose-development/SKILL.md` |
| Coroutines, Flow, cancellation, dispatchers | `upstream/skills/kotlin-coroutines-flow/SKILL.md` |
| Gradle/AGP failures, build speed | `upstream/skills/gradle-build/SKILL.md` |
| Tests: unit, UI, fakes | `upstream/skills/android-testing/SKILL.md` |
| Security review (storage, crypto, exported components) | `upstream/skills/android-security/SKILL.md` (+ `references/threat-checklist.md`) |
| Reviewing a diff | `upstream/skills/android-code-review/SKILL.md` (+ `references/finding-examples.md`) |
| Feature → spec → plan → implement | `upstream/workflows/*/SKILL.md` |
| Design system / tokens / RTL | `upstream/skills/design-system/*` |

## Calibration examples (best part of this repo)

`upstream/evals/` holds before/after code pairs with `expected-findings.md` — state misuse,
oversized composables, coroutine leaks, dispatcher misuse, broken cancellation, DTO leaks,
repository violations. Use them to calibrate a review: read `before.kt`, predict the findings,
then compare with `expected-findings.md`.

## Rules when applying it here

- This is generic Android guidance. VPN-specific rules (VpnService, TUN, DNS, lifecycle, Play VPN
  policy) come from `skills/vpn-android/` and always win on conflicts.
- Persian/RTL UI: see `upstream/skills/design-system/rtl-localization/SKILL.md`.
- Do not copy upstream text into deliverables without keeping the MIT notice.
