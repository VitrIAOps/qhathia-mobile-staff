## Context

Prices are `Double` in the domain (products, orders, discounts) and `BigDecimal` in the dashboards and the subscription. They are written in four ways:

- `%1$.2f` in string resources, which `String.format` renders with the app locale: a comma decimal in Spanish, a point in English, never grouped (`product-inventory`, `order-requests`, `core:ui` discount and preview strings). `discount_original_price_format` has no "S/".
- `BigDecimal.toSolesLabel()` (`core:ui` `SalesFormatters.kt`): `Locale.US` integer, "S/ 2,849".
- `ProductDetailViewModel.formatPrice`: `Double.toString()` for non-whole prices, scientific from 10^7, which `parsePrice` rejects.
- The price placeholder "S/ 0.00", already with a point.

`parsePrice` accepts both a point and a comma, so the input side is fine.

## Goals / Non-Goals

**Goals:**
- One rule for money, in every screen and both languages.
- The price field always opens with something `parsePrice` accepts.

**Non-Goals:**
- Changing how the merchant types a price (both separators stay accepted).
- Decimals on dashboard totals: they stay in whole soles, by design.
- A currency other than PEN.

## Decisions

1. **Fixed separators, not a locale.** `MoneyFormat.kt` builds its `DecimalFormat`s with explicit `DecimalFormatSymbols` (decimal '.', grouping ','), `HALF_UP` rounding: `#,##0.00` for prices, `#,##0` for amounts.
   - *Why not `Locale("es", "PE")`?* The JVM (tests) and Android's ICU can disagree on a locale's symbols, and the app's English must write money the same way. Peru writes "S/ 1,234.50"; the placeholder already does.
   - API: `Double.toPenPrice()` and `BigDecimal.toPenPrice()` ("S/ 12,345,678.50"), `BigDecimal.toPenAmount()` ("S/ 2,849"). `toSolesLabel()` stays as a thin alias of `toPenAmount()` so the dashboards and `home-stats-fixes` keep compiling unchanged.

2. **Strings take the formatted text.** Every `%1$.2f` money placeholder becomes `%1$s` and the caller passes `price.toPenPrice()`; the "S/ " moves into the formatter, so the strings keep only their words ("Precio original: %1$s"). `discount_original_price_format` becomes `%1$s` too, and the struck price gains its "S/". `preview_price_format` (non-translatable) goes: the preview calls `toPenPrice()` directly.

3. **`formatPriceInput` in `core:common`.** Next to `parsePrice`, so the pair is tested together: `BigDecimal.valueOf(price).setScale(2, HALF_UP)`; whole → `"149"`, otherwise `toPlainString()` (`"189.90"`, `"12345678.50"`). Detalle's `seededFrom` uses it for both the field and the server-pricing key, so the discounted price still matches the untouched text.
   - *Why `BigDecimal.valueOf`?* It goes through `Double.toString`'s shortest representation, so 189.9 becomes 189.90, not 189.899999….

## Risks / Trade-offs

- [A string missed by the search] → the search covered `%1$.2f`, `%.2f` and `toSolesLabel` in every module; a missed one keeps today's look, it does not break.
- [Product Detail's server-pricing key changes form (189.9 → "189.90")] → both sides use `formatPriceInput`, so the comparison still holds; a test covers the discounted price staying shown.
- [Merge with other branches] → `core:ui` strings changed in `profile-fixes` on other lines; `toSolesLabel` keeps its name for `home-stats-fixes`.

## Migration Plan

Ships with the app, no data migration. Rollback is reverting the change.

## Open Questions

_None._
