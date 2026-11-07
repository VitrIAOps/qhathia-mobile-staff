## Why

The 2026-11-03 QA regression (`docs/handoff/android-regresion-2026-11-03.md` §3.3, F-82) finds money written three ways:

- Detalle seeds the price field with `Double.toString()`: "QA Precio grande" (12345678.5) shows "1.23456785E7", which `parsePrice` rejects, so the field opens invalid and the discount switch is disabled (S:DET-015).
- `%1$.2f` follows the app language: Spanish writes "S/ 12345678,50" in Inventario and "S/ 123,44" in the discount summary, while the price field shows "189.90" with a point (S:EXP-011). No screen groups thousands.
- "Aplicar descuento" shows the struck original price without "S/".
- The dashboards use their own `Locale.US` integer formatter ("S/ 2,849").

## What Changes

- **One way to write money.** A shared formatter in `core:ui`: prices with two decimals, a point as decimal separator and commas for thousands ("S/ 12,345,678.50", "S/ 189.90"), in Spanish and English alike. Dashboard totals keep whole soles with the same separators ("S/ 2,849").
- **Every price string uses it.** Inventario (price and discounted price, also their accessible text), Solicitudes (card, detail, unit and list prices, line totals), the discount dialog and toggle row (with "S/" on the struck price), and the preview. The `%1$.2f` strings become `%1$s`.
- **The price field never shows scientific notation.** Detalle seeds the field from a plain decimal: "149" for a whole price, "189.90", "12345678.50". The field then opens valid and the discount stays usable.
- **Mi suscripción** shows the plan price with its cents ("S/ 49.90"), since it is a price, not a dashboard total.

## Capabilities

### New Capabilities

- `money-format`: how the app writes amounts of money (prices, totals) and seeds the price field.

### Modified Capabilities

_None._ The screens' own requirements already say "S/ <price>"; this change fixes how the number is written, which the new capability states once.

## Impact

- **`core:ui`** — new `format/MoneyFormat.kt` (`Double.toPenPrice()`, `BigDecimal.toPenPrice()`, `BigDecimal.toPenAmount()`); `toSolesLabel` delegates to `toPenAmount`; discount and preview strings and their callers.
- **`core:common`** — `formatPriceInput(price)` next to `parsePrice`.
- **`features:product-detail`** — `seededFrom` uses `formatPriceInput`; the private `formatPrice` goes.
- **`features:product-inventory`**, **`features:order-requests`** — strings to `%1$s`, callers pass the formatted price.
- **`features:profile`** — the subscription price uses `toPenPrice()`.
- **Branch:** cut from `feature/refresh-on-return` (changes 0–3), which holds the latest Detalle, Inventario and Solicitudes. No other branch touches these strings.
