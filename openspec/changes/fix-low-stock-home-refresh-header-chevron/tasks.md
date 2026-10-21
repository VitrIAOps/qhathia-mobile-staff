## 1. Out-of-stock warning in the order detail (BUG-017)

- [x] 1.1 `features:order-requests` — add `order_item_out_of_stock` ("Sin existencias" / en "Out of stock") next to `order_item_low_stock`
- [x] 1.2 Add `OutOfStockWarning` in `OrderDetailDialog.kt`, same layout as `LowStockWarning`, using `colorScheme.errorContainer` / `onErrorContainer` and `CoreUiR.drawable.ic_alert` (design Decision 1)
- [x] 1.3 Replace the stock condition in `OrderItemRow` with the `when` from Decision 1 (`null` → nothing, `<= 0` → out of stock, `<= LOW_STOCK_THRESHOLD` → low stock) and update its comment
- [x] 1.4 Add a preview item with `productStock = 0` (light and dark) next to the existing low-stock one
- [x] 1.5 `./gradlew :features:order-requests:testDebugUnitTest :features:order-requests:lintDebug`

## 2. Inicio refreshes on return (BUG-009)

- [x] 2.1 `features:home` — add `HomeUiEvent.ScreenStarted` with KDoc ("Inicio came back on screen")
- [x] 2.2 `HomeViewModel` — handle `ScreenStarted`: ignore the first one per ViewModel, bump `reloadTrigger` on every later one (Decision 2); update the class KDoc
- [x] 2.3 `HomeScreen` — add `LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.onEvent(HomeUiEvent.ScreenStarted) }`
- [x] 2.4 `HomeViewModelTest`: the first `ScreenStarted` issues no extra load; a second one re-requests summary, suggestions and the pending count for the selected period; the selected period is kept; the summary stays in the state while the reload is in flight and after a failed reload
- [x] 2.5 `./gradlew :features:home:testDebugUnitTest :features:home:lintDebug`

## 3. Root headers without a back chevron (BUG-011)

- [x] 3.1 `features:order-requests` — remove the `IconButton` chevron from `OrderRequestsHeader`, its comment, the unused imports and `order_requests_back_cd` (es/en)
- [x] 3.2 `features:product-inventory` — remove the `IconButton` chevron from `InventoryHeader`, the unused imports and `inventory_back_cd` (es/en)
- [x] 3.3 Grep for `order_requests_back_cd` and `inventory_back_cd` to confirm nothing else uses them
- [x] 3.4 `./gradlew :features:product-inventory:testDebugUnitTest :features:product-inventory:lintDebug`

## 4. Docs and verification

- [x] 4.1 Mark §4.2, §4.3 and §4.4 as done in `docs/handoff/android-pendientes.md`
- [x] 4.2 Full build: `./gradlew testDebugUnitTest lintDebug assembleDebug`
- [ ] 4.3 Manual — order detail: an order with one item at stock 0, one at 3 and one consult-mode item shows `Sin existencias`, `Quedan pocos disponibles` and nothing, respectively, in light and dark
- [ ] 4.4 Manual — Inicio: restock a product listed as `Sin existencias` from Inventario and switch back to Inicio; it leaves the list without a full-screen loader. Also check back from `Ver más` and from the background
- [ ] 4.5 Manual — Solicitudes and Inventario headers: no chevron, TalkBack announces no `Volver`, and the titles line up with Inicio's (compare with Figma)
