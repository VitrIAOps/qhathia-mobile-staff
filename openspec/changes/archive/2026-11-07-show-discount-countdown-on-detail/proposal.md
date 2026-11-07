## Why

A discount created from the app lasts one week, but nothing in the app says when it ends: the merchant only finds out when the struck-through price disappears. `GET /api/v1/products/{id}` already returns `discountEndsAt` (Fase 25) and the detail screen already maps it into `ProductDetail` (change `use-server-discounted-price-in-detail`); it is just not shown. Handoff `android-pendientes-v2.md`, Apéndice A, lists the "termina en X" as the pending optional use of that field.

## What Changes

- On Product Detail, under the `Descuento de X%` summary line, a secondary line counts down to the end of the discount in force: `Termina en 6 días y 4 horas`, `Termina en 1 día`, `Termina en 5 horas`, `Termina en menos de 1 hora`.
- The countdown refreshes every minute while the screen is open, and disappears once the end has passed.
- It is shown only for the discount the server says is in force, with the same rule as the final price: while the form's price and discount are the ones loaded. It is hidden when the merchant edits the price or the discount (an unsaved discount has no end yet), when the server reports no discount in force (`discountedPrice: null`), and when `discountEndsAt` is missing.
- The Inventory row does not change.

## Capabilities

### New Capabilities

_None._

### Modified Capabilities

- `product-detail`: new requirement "Discount end countdown on product detail".

## Impact

- **`features:product-detail`** — `ProductDetailUiState`: `ServerPricing` also carries `discountEndsAt`, and a derived `discountEndsAt` follows the same "untouched" rule as `discountedPrice`. `ProductDetailViewModel.seededFrom` fills it. New pure formatter for the remaining time, a small countdown composable under the toggle row in `ProductDetailContent`, and `strings.xml` (es + en). Unit tests for the formatter and the ViewModel rule.
- **Not affected:** `core:*` (the field is already mapped), `core:ui`'s `QhathiaDiscountToggleRow`, `features:product-inventory`, `features:product-register`, the API.
