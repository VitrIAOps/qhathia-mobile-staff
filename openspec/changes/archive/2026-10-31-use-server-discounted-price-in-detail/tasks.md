## 1. Network (`core:network`)

- [x] 1.1 In `ProductDtos.kt`, add `discountedPrice: Double? = null` and `discountEndsAt: String? = null` to `ProductDetailDto`, and rewrite its KDoc: the server's discounted price is read; `discounts[]` is still not consumed
- [x] 1.2 In `ProductDtosTest`, cover a detail payload with both fields and one without them (both decode to `null`)

## 2. Domain (`core:domain`)

- [x] 2.1 In `ProductDetail.kt`, add `discountedPrice: Double? = null` and `discountEndsAt: Instant? = null`, with KDoc stating `null` means no discount in force (backend rule, §25)

## 3. Data (`core:data`)

- [x] 3.1 In `ProductMappers.kt`, map `discountedPrice` and `discountEndsAt.toInstantOrNull()` in `ProductDetailDto.toDomain()`
- [x] 3.2 In `ProductMappersTest`, cover the detail mapping with a discount (price, instant) and without one (both `null`), plus an unparseable `discountEndsAt` → `null`

## 4. Testing fixtures (`core:testing`)

- [x] 4.1 In `TestFixtures.productDetail(...)`, add `discountedPrice: Double? = null` and `discountEndsAt: Instant? = null` parameters and pass them through

## 5. Product detail (`features:product-detail`)

- [x] 5.1 In `ProductDetailUiState`, add `ServerPricing(priceText, discountedPrice)` and the `serverPricing: ServerPricing? = null` field; document it in the class KDoc
- [x] 5.2 Rewrite the `discountedPrice` getter per design decision 1: `null` without a form discount; the server value (or the parsed regular price when it is `null`) while `price == serverPricing.priceText && discount == loadedDiscount`; otherwise `applyTo` on the parsed price
- [x] 5.3 In `ProductDetailViewModel.seededFrom`, set `serverPricing` from `formatPrice(product.price)` and `product.discountedPrice`
- [x] 5.4 In `save()`, set `serverPricing = null` on both paths that stay on the form after `updateProductUseCase` succeeded (photo failure, discount failure)
- [x] 5.5 Confirm `loadDiscount()` leaves `serverPricing` untouched, and that `productPreviewMapper` and `ProductDetailContent` need no change (they read `discountedPrice`)

## 6. Tests (`features:product-detail`)

- [x] 6.1 Load with a discount and a server `discountedPrice` that differs from `applyTo` (e.g. server 30.90, client would give another value) → `discountedPrice` equals the server value
- [x] 6.2 Load with a current discount and server `discountedPrice = null` → `discountedPrice` equals the regular price
- [x] 6.3 Edit the price → client computation; type the loaded price back → server value again
- [x] 6.4 Apply a different discount → client computation; toggle off → `null`
- [x] 6.5 Discount load fails, retry succeeds → server value
- [x] 6.6 Save where the product update succeeds and the discount save fails → client computation (snapshot cleared); same for the photo-failure path
- [x] 6.7 Preview opened right after load uses the server value as the shown price
- [x] 6.8 Re-check the existing discount tests in `ProductDetailViewModelTest` that assert `discountedPrice`; update fixtures so they keep asserting what they meant

## 7. Verification

- [x] 7.1 Run `./gradlew :core:network:testDebugUnitTest :core:data:testDebugUnitTest :features:product-detail:testDebugUnitTest` and `./gradlew lintDebug`; all pass
- [x] 7.2 Run `./gradlew assembleDebug`

## 8. Manual — QA

- [ ] 8.1 Manual — Put a discount on a product from staff; the final price in the detail equals the price in the Inventory row and on the kiosk grid
- [ ] 8.2 Manual — Edit the price on the detail: the final price updates live; cancel and reopen: it shows the server's number again
