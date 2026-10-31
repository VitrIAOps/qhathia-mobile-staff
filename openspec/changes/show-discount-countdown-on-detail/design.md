## Context

`ProductDetail.discountEndsAt: Instant?` is already mapped from `GET /api/v1/products/{id}`. `ProductDetailUiState` keeps a `ServerPricing(priceText, discountedPrice)` snapshot and only trusts it while `price == priceText && discount == loadedDiscount` (archived change `2026-10-31-use-server-discounted-price-in-detail`). The summary line is drawn by `QhathiaDiscountToggleRow` in `core:ui`, a shared component also used by product registration.

Only `features:product-detail` changes:

```
features:product-detail → core:domain (ProductDetail.discountEndsAt, already there)
                        → core:ui (theme only; QhathiaDiscountToggleRow untouched)
```

## Goals / Non-Goals

**Goals:**
- Tell the merchant how long the discount in force has left, as a days-and-hours countdown.
- Never show an end for something that is not in force or not saved.

**Non-Goals:**
- Inventory row, registration form, kiosk.
- Letting the merchant pick the end date (the app's discounts stay one week).

## Decisions

### 1. Same trust rule as the final price

`ServerPricing` gains `discountEndsAt: Instant?`. `ProductDetailUiState` exposes:

```kotlin
val discountEndsAt: Instant?
    get() = serverPricing
        ?.takeIf { discount != null && price == it.priceText && discount == loadedDiscount }
        ?.takeIf { it.discountedPrice != null }
        ?.discountEndsAt
```

Sharing the condition with `discountedPrice` keeps the line and the final price consistent: either both come from the server, or neither the server number nor its end date is shown. The match is extracted into one private `isPricingAsLoaded` property used by both getters.

*Alternative:* show "Termina en 7 días" for an unsaved discount, since the app always creates one-week discounts. Rejected: the date is only real once saved, and with the server clock skew found in QA the actual window can differ.

### 2. Pure formatter, ticking in the composable

`remainingDiscountTime(endsAt: Instant, now: Instant): DiscountTimeLeft?` in `model/` returns `null` once `now >= endsAt`, otherwise one of `DaysAndHours(days, hours)` (hours may be 0) / `Hours(hours)` / `UnderAnHour`. Pure Kotlin, unit-tested without Compose.

The composable `DiscountCountdown(endsAt)` holds `now` in `produceState`, updated every 60 s with `delay`, and renders the result with plurals. The ViewModel does not tick: the countdown is presentation of a fixed instant, and a ticking `StateFlow` would recompose the whole form every minute and complicate every ViewModel test with virtual time.

*Alternative:* inject a `Clock` and tick in the ViewModel. More testable end to end but heavier for a label; the formatter carries the logic that needs testing.

### 3. Rendered next to, not inside, `QhathiaDiscountToggleRow`

The line goes right after the toggle row inside the existing `Column` in `ProductDetailContent`, styled `bodySmall` / `onSurfaceVariant`. Adding a parameter to the shared `core:ui` component would touch registration for a field it never has.

### 4. Strings

`detail_discount_ends_in` = `Termina en %1$s`, `detail_discount_ends_in_days_hours` = `Termina en %1$s y %2$s`, `detail_discount_ends_soon` = `Termina en menos de 1 hora`, plurals `detail_discount_days` (`%d día` / `%d días`) and `detail_discount_hours` (`%d hora` / `%d horas`). English: `Ends in …`, `Ends in less than 1 hour`.

## Risks / Trade-offs

- **[Server clock skew]** QA found the server clock about 4 minutes behind. `discountEndsAt` is stored as sent by the phone, so the countdown is computed against the phone's clock and is accurate to the minute; the skew only affects *when* the server starts/stops applying the discount. Accepted; reported to backend.
- **[Minute-level staleness]** The line can lag up to a minute behind the real remaining time. Irrelevant at days/hours granularity.

## Migration Plan

Client-only, additive. Rollback is reverting the change.

## Open Questions

_None._
