## Context

`ProductRow.kt` → `ProductPrice` renders a discounted row as a `FlowRow` of two texts:

| Text | Style today |
|---|---|
| Original `price` (left) | `bodySmall`, `onSurfaceVariant`, `TextDecoration.LineThrough`, theme weight (Normal) |
| `discountedPrice` (right) | `bodyMedium`, `onSurface`, `FontWeight.Bold` |

Typography comes from `core:ui` → `Type.kt`. Urbanist ships as a single variable font (`res/font/urbanist_variable.ttf`, `wght` axis 100–900), and `UrbanistFamily` registers one `Font` entry per weight, each pinned to its axis value: Normal, Medium, SemiBold, Bold. When a composable asks for a weight the family does not register, Compose picks the closest registered one, so `FontWeight.Light` today renders as Normal.

Modules involved (both existing, no new module, no new dependency edge):

```
features:product-inventory → core:ui
```

## Goals / Non-Goals

**Goals:**

- The discounted price uses the same style as a plain price (`bodyMedium`, `onSurface`, no weight override).
- The struck-through original price on a discounted Inventory row renders in Light (300).

**Non-Goals:**

- Changing the order of the two prices or the plain (no discount) price.
- Changing the screen-reader text (`inventory_price_discounted_cd`).
- Applying the light weight to the discount dialog (`QhathiaDiscountDialog`) or to Product Detail / Registration.
- Adding a new named text style to the typography scale.

## Decisions

### 1. Register Light in `UrbanistFamily`

Add `urbanist(FontWeight.Light)` to `UrbanistFamily`. It reuses the same variable font with `FontVariation.weight(300)`, exactly like the existing entries.

- Without it, the row change would compile and do nothing visible (fallback to Normal).
- Registering a weight does not change any existing style: every style in `QhathiaTypography` names its own weight explicitly.

*Alternative considered:* a separate static Light font file. Rejected: the variable font already covers the weight.

### 2. Light (300), not Thin (100)

The struck price is already small (`bodySmall`) and muted (`onSurfaceVariant`). At 100 the digits and the strike line become hard to read on low-brightness screens and in dark mode. 300 gives a visible step down from the regular weight while staying legible. Agreed with the product owner.

### 3. Weight override on the existing style, set at the call site

The struck text keeps `MaterialTheme.typography.bodySmall` and adds `fontWeight = FontWeight.Light`. Typography still comes from the theme; only the weight is overridden, the same call-site pattern the row already used for the bold discounted price.

*Alternative considered:* a new `TextStyle` token in `core:ui` (e.g. "struck price"). Rejected for now: there is a single use, and the existing pattern on the same composable is a call-site weight override. If the discount dialog later adopts the same look, a shared token can be extracted then.

### 4. The discounted price drops its bold override

The discounted price takes the place of the regular price, so it uses exactly the plain price's style: `bodyMedium`, `onSurface`, and no `fontWeight` override. The strike-through and the lighter weight on the original price are enough to show there is a discount. Requested by the product owner.

## Risks / Trade-offs

- **[Risk] Lower contrast for the struck price.** → It stays `bodySmall` in `onSurfaceVariant` and remains a secondary, reference value; the discounted price is the one that must read as the price. Check both themes in the previews.
- **[Trade-off] Light is now available app-wide.** → Any screen can use it from now on. That is intended: it is part of the same typeface, and using it remains a deliberate per-call choice.
