# Expected findings: compose/02-oversized-composable

## Major

**Major — oversized composable mixing four distinct sections with inline
business logic.** `CheckoutScreen` renders the cart list, promo-code
entry with validation, shipping selection, and a price summary — four
logically distinct sections — and computes promo validity, discount, and
totals inline inside the composable body instead of receiving them as
already-resolved UI state. Per `compose-development`'s oversized-composable
heuristic and `android-architecture`'s UI-model boundary: this mixes
layout with logic that belongs in a ViewModel/use case, and recomposes
the whole screen as one unbreakable unit.

**Fix:** extract `CartLineList`, `PromoCodeField`, `ShippingSelector`, and
`OrderSummaryCard` as their own stateless composables with narrow
parameters; move `promoValid`/`subtotalCents`/`discountCents`/`totalCents`
computation into the ViewModel's `UiState` (pre-formatted currency
strings, not raw cents) so the composables only render, never compute.

## Minor

- **Minor — currency formatting duplicated and done in the UI layer.**
  `NumberFormat.getCurrencyInstance` is instantiated twice, inline,
  inside composable bodies (recreated on every recomposition) and mixes
  presentation formatting with the UI tree. Should be resolved once in
  the ViewModel/UI-state mapping and passed down as a formatted `String`.
- **Minor — promo validation logic duplicated between UI gating and total
  calculation**, both recomputing `promoValid` inline; a single source of
  truth (computed once in `UiState`) removes the duplication risk.

## Suggestion

- Extracting each section also makes each one independently
  `@Preview`-able (see `compose-development`), which this monolithic
  version can't offer for, e.g., just the shipping selector's states.

## What a false positive looks like here

Flagging this fixture purely on line count without naming which
sections/logic should be extracted, or suggesting a generic "split this
up" without identifying the actual layering violation (business logic
inline in the composable), misses the substance of the finding per
`android-code-review`'s "usable finding" bar.
