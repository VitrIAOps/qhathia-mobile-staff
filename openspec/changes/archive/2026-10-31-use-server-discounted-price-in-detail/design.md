## Context

Two sources describe a product's discount on the detail screen today:

- `GET /api/v1/products/{id}/discounts` → `GetProductDiscountUseCase` → `loadedDiscount` / `discount` on the form. This is what the toggle, the summary (`Descuento de 20%`) and the `Aplicar descuento` dialog edit, and what the save flow replaces.
- `GET /api/v1/products/{id}` → `ProductDetail`. Since Fase 25 (`api-contract.md` §25) it also returns `discountedPrice` and `discountEndsAt`, computed by the backend with the single rule set (cheapest wins, no chaining, `startsAt <= now < endsAt`, `HALF_UP` to 2 decimals, floor 0, `null` when nothing lowers the price). `ProductDetailDto` drops both fields.

`ProductDetailUiState.discountedPrice` is a getter: `discount?.applyTo(parsePrice(price))`. It feeds the final price on the summary line (`ProductDetailContent.kt:263`) and the preview (`productPreviewMapper.kt:15`). `Discount.applyTo` already rounds `HALF_UP` and floors at 0, but it ignores the window and does not know about other discounts, so on load it can disagree with the list (which reads `Product.discountedPrice`) and with the kiosk.

Modules involved — all existing; no new module:

```
features:product-detail → core:domain ← core:data → core:network
                                    core:testing (test fixtures)
```

## Goals / Non-Goals

**Goals:**
- On opening a product, the detail shows the same discounted price as the list and the kiosk: the server's.
- Keep the live preview while editing, since the server cannot price a draft.
- Never mix the two: a number shown is either the server's for the loaded values or the client's for the edited ones.

**Non-Goals:**
- Showing `discountEndsAt` ("termina en X"). It is mapped so the model is complete, but no UI uses it in this change.
- Changing how the current discount is chosen from `/discounts`, the dialog, or the replace/remove flow.
- Product registration: a new product has no server price yet.
- Reading `discounts[]` from the product payload.

## Decisions

### 1. Keep a snapshot of what the server priced, and use it only while the form matches it

`ProductDetailUiState` gains one nullable field, a small private-to-module value:

```kotlin
/** The price and discounted price the server returned for the product as loaded. */
data class ServerPricing(val priceText: String, val discountedPrice: Double?)
val serverPricing: ServerPricing? = null
```

`seededFrom(product)` sets it with `priceText = formatPrice(product.price)` (the exact text the price field is seeded with) and `discountedPrice = product.discountedPrice`.

`discountedPrice` becomes:

```kotlin
val discountedPrice: Double?
    get() {
        val applied = discount ?: return null              // no discount on the form → no final price
        val server = serverPricing
        if (server != null && price == server.priceText && discount == loadedDiscount) {
            return server.discountedPrice ?: parsePrice(price)   // null = nothing in force → regular price
        }
        return parsePrice(price)?.let { applied.applyTo(it) }
    }
```

The match compares the price **text**, not the parsed number, so the rule is trivially "the field is exactly as seeded". Typing `35.9` over `35.90` counts as an edit and shows the client number — which is equal anyway.

*Alternatives considered:*
- **Overwrite the getter with a stored `discountedPrice` field updated on every event.** More state to keep consistent across `onPriceChanged`, `onDiscountApplied`, `onDiscountToggled`, retries and save; a derived getter cannot go stale.
- **A `isPricingTouched` flag set on first edit.** Simpler, but never resets when the merchant types the original value back, and needs every editing path to remember to set it. Comparing against the snapshot covers all paths at once.
- **Only use the server number, never the client one.** Breaks the live preview the spec requires while editing.

### 2. `discountedPrice == null` from the server with a discount on the form → regular price

If `/discounts` returns a current discount (active, not ended) but the server says nothing is in force — a window that has not started, or a discount that lowers nothing — the summary is still shown (the merchant does have that discount configured) and the final price is the regular price. That is what the list and the kiosk show, and what a shopper pays. Falling back to `applyTo` would re-create the very divergence this change removes.

### 3. Drop the snapshot once a save has reached the server

After `updateProductUseCase` succeeds, the server's numbers may have changed (new price, discount partly replaced). On the path where the product saved but the discount did not and the merchant stays on the form, `serverPricing` is set to `null`, so the form shows the client computation until the screen is reopened. The successful path navigates away, so it does not matter there. A photo-failure path also stays on the form after the product update: it clears the snapshot the same way.

*Alternative:* re-read `GET /products/{id}` after a partial failure to refresh the snapshot. One more request on an error path for a number the merchant is about to change again; not worth it.

### 4. `loadDiscount()` (retry) does not touch the snapshot

The snapshot comes from the product request, which already succeeded. Once the retry fills `loadedDiscount` and `discount`, the match condition holds and the server number is shown.

### 5. Data layer: map, do not interpret

`ProductDetailDto` gains `discountedPrice: Double? = null` and `discountEndsAt: String? = null`; `ProductDetail` gains `discountedPrice: Double? = null` and `discountEndsAt: Instant? = null` (defaults keep existing constructors compiling). The mapper reuses `toInstantOrNull()` exactly as `ProductDto.toDomain()` does. No rule from §25 is replicated anywhere.

## Risks / Trade-offs

- **[The summary and the server number can describe different discounts]** When a product holds several discounts (created outside this app), `/discounts` picks one for the summary while the server prices with the cheapest. → The final price is still the true one; saving replaces all discounts with the form's, which re-aligns them. Accepted.
- **[A summary with no reduction looks odd]** Decision 2 can show `Descuento de 20%` next to the regular price. → It is the truthful state (nothing is rebated now) and only happens for discounts not created by this app. Documented here; revisit only if QA reports it.
- **[Price text formatting drift]** If `formatPrice` changes, the snapshot and the seeded text change together, since both come from the same call. No risk as long as both use it.

## Migration Plan

Client-only and additive. Older backends without the fields deserialize to `null` (`ignoreUnknownKeys` + defaults); in that case the detail would show the regular price for a loaded discount. Production has had the fields since 2026-10-18, so no fallback is kept. Rollback is reverting the change.

## Open Questions

_None._
