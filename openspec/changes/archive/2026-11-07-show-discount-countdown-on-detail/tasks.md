## 1. State (`features:product-detail`)

- [x] 1.1 Add `discountEndsAt: Instant?` to `ServerPricing` and fill it in `ProductDetailViewModel.seededFrom` from `product.discountEndsAt`
- [x] 1.2 Extract the "price and discount as loaded" check into a private `isPricingAsLoaded` used by `discountedPrice`, and add the derived `discountEndsAt` getter (design decision 1)

## 2. Formatter

- [x] 2.1 Add `DiscountTimeLeft` (`DaysAndHours`, `Hours`, `UnderAnHour`) and the pure `remainingDiscountTime(endsAt, now)` in `model/`, returning `null` once the end has passed; whole units rounded down

## 3. UI

- [x] 3.1 Add the strings and plurals (es + en) from design decision 4
- [x] 3.2 Add `DiscountCountdown(endsAt)` that ticks `now` every 60 s with `produceState` and renders the formatter result in `bodySmall` / `onSurfaceVariant`; nothing when the formatter returns `null`
- [x] 3.3 In `ProductDetailContent`, render it right after `QhathiaDiscountToggleRow` when `uiState.discountEndsAt != null`; add a preview with a countdown

## 4. Tests

- [x] 4.1 Formatter: 6 d 4 h 20 min → `DaysAndHours(6, 4)`; 1 d 30 min → `DaysAndHours(1, 0)`; 5 h 59 min → `Hours(5)`; 40 min → `UnderAnHour`; past or equal end → `null`
- [x] 4.2 ViewModel: loaded discount in force with an end → `discountEndsAt` set; price edited or discount changed → `null`; server `discountedPrice = null` → `null`; no discount → `null`

## 5. Verification

- [x] 5.1 Run `./gradlew :features:product-detail:testDebugUnitTest lintDebug assembleDebug`; all pass

## 6. Manual — QA

- [ ] 6.1 Manual — Open a product with a discount in force: the line under the summary counts down in days and hours and matches the discount's end
- [ ] 6.2 Manual — Edit the price or the discount: the line disappears; cancel and reopen: it is back
