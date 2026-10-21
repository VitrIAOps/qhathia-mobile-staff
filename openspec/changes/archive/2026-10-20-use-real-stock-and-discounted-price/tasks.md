## 1. Transport and domain models

- [x] 1.1 `core:network` — add `val stock: Int? = null`, `val discountedPrice: Double? = null` and `val discountEndsAt: String? = null` to `ProductDto` (`dto/ProductDtos.kt`); update its KDoc, which still says the API "exposes no stock quantity yet" and that the stock is mocked
- [x] 1.2 `core:domain` — add `stock: Int? = null`, `discountedPrice: Double? = null` and `discountEndsAt: Instant? = null` (`java.time.Instant`) as the last properties of `Product`, documenting that `null` means "unknown stock" / "no discount in effect"
- [x] 1.3 `core:data` — map the three fields in `ProductDto.toDomain()` (`mapper/ProductMappers.kt`), parsing `discountEndsAt` with the existing `toInstantOrNull()` helper
- [x] 1.4 `core:data` — mapper tests in `ProductMappersTest`: stock and discount present; all three `null`; an unparsable `discountEndsAt` maps to `null` without failing
- [x] 1.5 `core:network` — deserialization test for a `GET /products` row that carries `stock`, `discountedPrice` and `discountEndsAt`, and one that omits them (older backend)
- [x] 1.6 `core:testing` — add `stock`, `discountedPrice` and `discountEndsAt` parameters (default `null`) to `TestFixtures.product(...)`

## 2. Real stock on the Inventory row

- [x] 2.1 `features:product-inventory` — add the `inventory_out_of_stock` string ("Sin existencias" / en "Out of stock")
- [x] 2.2 In `ProductRow`, render the stock from `product.stock`: nothing for `null`, `inventory_out_of_stock` for `0`, `inventory_stock_format` otherwise
- [x] 2.3 Delete `mockedStock` (`ProductRow.kt:246`), the now unused `kotlin.math.abs` import, and the "mocked" sentence from the `ProductRow` KDoc
- [x] 2.4 Grep the repo for `mockedStock` and "mocked stock" to confirm nothing else references it (code, previews, tests, docs)

## 3. Discounted price on the Inventory row

- [x] 3.1 Add row strings for the price content description (e.g. `inventory_price_discounted_cd`: "Precio original S/ %1$.2f, precio con descuento S/ %2$.2f", plus en)
- [x] 3.2 In `ProductRow`, when `product.discountedPrice != null` show `price` struck through (`onSurfaceVariant`, `TextDecoration.LineThrough`) followed by `discountedPrice` emphasized (bold, `onSurface`), styled like `QhathiaDiscountDialog`; otherwise keep today's plain price. Merge the price group's semantics into the new content description. No arithmetic on the client
- [x] 3.3 Check the row on a narrow screen (360dp) with a long name, two prices and a stock label: nothing overlaps and the stock stays right-aligned

## 4. Quiet reload after a discount save

- [x] 4.1 In `InventoryViewModel`, extract the "fetch with the active filters and apply the search" step of `loadProducts()` so it can run without setting `isLoading`
- [x] 4.2 After `saveProductDiscountUseCase` succeeds, run that quiet reload: keep the current rows while it is in flight, replace `loadedProducts` / `visibleProducts` on success, change nothing on failure (no error state, no extra snackbar), and keep the `knownDiscounts` entry just saved
- [x] 4.3 `InventoryViewModelTest`: a successful discount save triggers one more `GET /products` with the active category and availability, `isLoading` never becomes true during it, and the row's product then carries the new `discountedPrice` from the fake repository
- [x] 4.4 `InventoryViewModelTest`: when the quiet reload fails, the previous products stay in the state and `isError` stays false
- [x] 4.5 `InventoryViewModelTest`: the search text is preserved across the quiet reload
- [x] 4.6 Every products request cancels the one in flight, so a filter change during the quiet reload is not overwritten by it (test included)

## 5. Previews and verification

- [x] 5.1 Update the `InventoryContent` / `ProductRow` previews (light and dark) to cover: stock `n`, stock `0`, stock `null`, a discounted row and a non-discounted row
- [x] 5.2 Run `./gradlew :core:network:testDebugUnitTest :core:data:testDebugUnitTest :features:product-inventory:testDebugUnitTest`
- [x] 5.3 Run `./gradlew :features:product-inventory:lintDebug :app:assembleDebug`
- [x] 5.4 Confirm `features:product-detail` and `features:product-register` are untouched and their tests still pass (`./gradlew :features:product-detail:testDebugUnitTest :features:product-register:testDebugUnitTest`)
- [x] 5.5 Manual pass against production: the stock on each Inventory row equals the stock on that product's detail; a product with a discount shows its price struck through with the discounted price; applying a discount from the `⋮` menu updates the row without a spinner or losing the scroll position
- [x] 5.6 Update the `GET /api/v1/products` response example in `docs/android/api-contract.md` §8 to include `stock`, `discountedPrice` and `discountEndsAt`
