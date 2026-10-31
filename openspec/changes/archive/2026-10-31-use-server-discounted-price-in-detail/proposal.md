## Why

The Inventory list shows the discounted price the backend computes (`discountedPrice`), but the Product Detail screen still works it out on the phone from the form's price and the discount it reads from `/discounts`. Since Fase 25 the backend is the only authority on that number (cheapest discount wins, `startsAt <= now < endsAt`, `HALF_UP`, floor at 0, `null` when nothing is in force), and `GET /api/v1/products/{id}` already returns it. Computing it again on the client means the list and the detail can show different prices for the same product — for example a discount whose window has not started yet shows as rebated in the detail while the list and the kiosk charge the full price. Handoff `android-pendientes-v2.md`, "Nota 1", is the last required Staff item.

## What Changes

- `ProductDetailDto` reads `discountedPrice` and `discountEndsAt` from `GET /api/v1/products/{id}` (the KDoc that says discounts are "not consumed" is corrected). `ProductDetail` carries both.
- When the Product Detail form is opened, the final price shown next to the discount summary is the server's `discountedPrice`. It stays the server's number while the merchant has not touched the price or the discount.
- When the server says no discount is in force (`discountedPrice: null`) but a discount is shown on the form, the final price shown is the regular price: nothing is rebated right now.
- As soon as the merchant edits the price or the discount, the final price becomes the live preview computed on the phone (today's `Discount.applyTo`), because the server does not know the draft yet. The preview of the product ("Vista previa") follows the same rule, since it reads the same value.
- After a save that reached the server and then failed on the discount, the form no longer trusts the loaded server price and falls back to the live preview.
- No new endpoint, no new request, no UI layout change.

## Capabilities

### New Capabilities

_None._

### Modified Capabilities

- `product-detail`: "Discount shown on product detail" — the final price next to the summary comes from the server's `discountedPrice` while the form's price and discount are as loaded, and is the live client computation only once either is edited.

## Impact

- **`core:network`** — `ProductDtos.kt`: `ProductDetailDto` gains `discountedPrice: Double? = null` and `discountEndsAt: String? = null`; KDoc updated. Tests in `ProductDtosTest`.
- **`core:domain`** — `ProductDetail.kt`: gains `discountedPrice: Double? = null` and `discountEndsAt: Instant? = null`.
- **`core:data`** — `ProductMappers.kt`: `ProductDetailDto.toDomain()` maps both fields (reusing `toInstantOrNull()`). Tests in `ProductMappersTest`.
- **`core:testing`** — `TestFixtures.productDetail(...)` accepts the two new fields.
- **`features:product-detail`** — `ProductDetailUiState` (server snapshot + `discountedPrice` rule), `ProductDetailViewModel` (seed and invalidate the snapshot). Tests in `ProductDetailViewModelTest`.
- **Not affected:** `features:product-inventory` (already uses `Product.discountedPrice`), `features:product-register` (a new product has no server price yet), the `Aplicar descuento` dialog, the discount save/remove flow, the API.
