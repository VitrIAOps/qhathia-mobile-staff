## 1. Registration hands back the product id (`:features:product-register`)

- [x] 1.1 Change `ProductRegisterUiEffect.NavigateBackWithSuccess` and `NavigateBackWithDiscountFailed` from `data object` to `data class …(val productId: String)` and update their KDoc
- [x] 1.2 In `ProductRegisterViewModel.submit()`, emit both effects with the id returned by `RegisterProductUseCase`
- [x] 1.3 Change `onRegistered` and `onDiscountFailed` to `(String) -> Unit` in `ProductRegisterScreen` and `productRegisterGraph`, forwarding `effect.productId`; update the KDoc in `ProductRegisterNavigation.kt`
- [x] 1.4 Update `ProductRegisterViewModelTest` assertions on `NavigateBackWithSuccess` and `NavigateBackWithDiscountFailed` to expect the id returned by `FakeProductRepository.registerResult`

## 2. Inventory result channel (`:features:product-inventory`, `:app`)

- [x] 2.1 Add `INVENTORY_REGISTERED_PRODUCT_ID_KEY` to `InventoryNavigation.kt` next to the existing result constants, with KDoc
- [x] 2.2 In `inventoryGraph`, read the id from the entry's `SavedStateHandle`, pass it to `InventoryScreen` as `registeredProductId`, and clear both keys in `onRegisterResultConsumed`
- [x] 2.3 In `MainShell.kt`, make `onRegistered` and `onDiscountFailed` take the product id and set `INVENTORY_REGISTERED_PRODUCT_ID_KEY` before `INVENTORY_REGISTER_RESULT_KEY`, then pop

## 3. Inventory MVI contract and ViewModel (`:features:product-inventory`)

- [x] 3.1 Add `InventoryUiEvent.ViewRegisteredProductClicked(productId: String)` with KDoc
- [x] 3.2 Add `InventoryUiEffect.ScrollToProduct(index: Int)` with KDoc
- [x] 3.3 Add strings `inventory_register_view_product` ("Ver producto") and `inventory_register_view_product_not_found` ("No se encontró el producto en la lista") to `strings.xml`
- [x] 3.4 Implement `revealProduct(productId)` in `InventoryViewModel` for the already-loaded cases: defer via a private `pendingRevealProductId` while `isLoading`; emit `ScrollToProduct(index)` when visible; clear the search and emit when hidden only by search
- [x] 3.5 Extend `revealProduct` for the not-loaded cases: with a category or availability filter active, reset both filters and the search, set `pendingRevealProductId` and call `loadProducts()`; with no filter active, emit `ShowMessage(inventory_register_view_product_not_found)`
- [x] 3.6 In `loadProducts()`, take and clear the pending id after a successful load and resolve it through `revealProduct`; drop it on failure
- [x] 3.7 Handle `ViewRegisteredProductClicked` in `onEvent`

## 4. Inventory UI (`:features:product-inventory`)

- [x] 4.1 Add a `listState: LazyListState = rememberLazyListState()` parameter to `InventoryContent`, pass it through `ProductListCard` to `LazyColumn(state = …)`
- [x] 4.2 In `InventoryScreen`, create the `LazyListState`, pass it to `InventoryContent`, and add the `registeredProductId: String?` parameter
- [x] 4.3 Add a local suspend helper that shows a registration snackbar with `actionLabel = inventory_register_view_product` (omitted when the id is null) and `duration = SnackbarDuration.Long`, and sends `ViewRegisteredProductClicked` on `SnackbarResult.ActionPerformed`
- [x] 4.4 Use the helper in the `INVENTORY_REGISTER_RESULT_SUCCESS` and `INVENTORY_REGISTER_RESULT_DISCOUNT_FAILED` branches, consuming the result afterwards; leave the plan-limit and product-updated branches as they are
- [x] 4.5 Handle `InventoryUiEffect.ScrollToProduct` in the effect collector: in a launched coroutine, wait with `snapshotFlow { listState.layoutInfo.totalItemsCount }.first { it > index }` and then `animateScrollToItem(index)`
- [x] 4.6 Update the `InventoryScreen` KDoc to describe the action and the scroll

## 5. Tests (`:features:product-inventory`)

- [x] 5.1 `InventoryViewModelTest`: product in the visible list → `ScrollToProduct` with its index, no extra products request
- [x] 5.2 Product hidden by search → search query cleared, `visibleProducts` restored, `ScrollToProduct` with its index in the loaded list, no extra products request
- [x] 5.3 Product excluded by a category filter → `selectedCategoryId`, `selectedAvailability` and `searchQuery` reset, products re-queried without filters, then `ScrollToProduct` with its index
- [x] 5.4 Product excluded by an availability filter → same outcome as 5.3
- [x] 5.5 No filter active and product not in the loaded list → `ShowMessage(inventory_register_view_product_not_found)`, no scroll effect, no extra products request
- [x] 5.6 Filters cleared but the product is still absent from the unfiltered list → not-found message, exactly one extra products request
- [x] 5.7 Event received while a load is in flight → `ScrollToProduct` emitted only after the load succeeds
- [x] 5.8 A pending reveal whose load fails → no `ScrollToProduct`, only the load error, and a later successful load does not scroll

## 6. Verification

- [x] 6.1 Run `./gradlew :features:product-inventory:testDebugUnitTest :features:product-register:testDebugUnitTest`
- [x] 6.2 Run `./gradlew lint` and `./gradlew assembleDebug`
- [x] 6.3 Confirm the `InventoryContent` light/dark previews still render with the new default parameter
- [x] 6.4 Manual check on device: register a product with a long list → snackbar shows `Ver producto` → tapping scrolls to the row; repeat with an active search, with a category filter that excludes it (filters reset to `Todas`), with a discount that fails, and letting the snackbar time out
