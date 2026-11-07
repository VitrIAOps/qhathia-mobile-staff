## 1. Network (`core:network`)

- [x] 1.1 `OrderItemDto`: add `listPriceSnapshot: Double? = null`, `productActive: Boolean = true`, `currentProductName: String? = null`, with KDoc (Fases 38–39)
- [x] 1.2 `OrderProblemDto`: add `reason: String? = null` (`PRODUCT_NOT_SELLABLE`)
- [x] 1.3 `OrderApiServiceTest` (or a DTO test): the new item fields decode when present and default when absent

## 2. Domain (`core:domain`)

- [x] 2.1 `OrderItem`: `listPrice: Double?`, `productActive: Boolean`, `currentProductName: String?`, with KDoc
- [x] 2.2 `ConfirmOrderError.ProductNotSellable(productName: String?, reason: String?)`
- [x] 2.3 `OrderRepository`: `ordersChanged: Flow<Unit>` and `notifyOrdersChanged()`, with KDoc (a signal to re-query, not a cache)
- [x] 2.4 New use cases: `ObserveOrdersChangedUseCase`, `NotifyOrdersChangedUseCase`, `HandleOrderPushUseCase` (design decision 1)
- [x] 2.5 `HandleOrderPushUseCaseTest`: `NEW_ORDER` and `ORDER_TAKEN` notify and refresh the count; `ORDER_EXPIRING`, unknown and null do nothing; a failing count refresh does not throw

## 3. Data (`core:data`)

- [x] 3.1 `OrderRepositoryImpl`: the shared-flow signal; `409` + `PRODUCT_NOT_SELLABLE` → `ProductNotSellable(problem.productName, problem.reason)`; after a confirm or reject `200`, a failed count refresh lowers the count by one (not below zero)
- [x] 3.2 `OrderMappers`: map the three item fields
- [x] 3.3 Tests: the mapping; the 409 mapping; count decrement when the refresh fails (and floor at zero); `notifyOrdersChanged` emits on `ordersChanged`

## 4. Testing fixtures (`core:testing`)

- [x] 4.1 `FakeOrderRepository`: `ordersChanged` / `notifyOrdersChanged()` with `notifyOrdersChangedCount`; a way to hold or fail `getOrders` per call if the ViewModel tests need it
- [x] 4.2 `TestFixtures.orderItem(...)` with the new fields; fix other constructions

## 5. Push (`core:notifications`)

- [x] 5.1 `QhathiaMessagingService`: inject `HandleOrderPushUseCase`; after the presenter, `runBlocking { withTimeoutOrNull(PUSH_SYNC_TIMEOUT_MS) { … } }` (design decision 2); update the class KDoc

## 6. Shell (`app`)

- [x] 6.1 `MainShellViewModel.onOrdersDeepLink()` calls `NotifyOrdersChangedUseCase`; `MainShell` calls it in the pending-destination effect after `switchTab(ORDER_REQUESTS_ROUTE)`
- [x] 6.2 `MainShellViewModelTest`: `onOrdersDeepLink` notifies

## 7. Solicitudes (`features:order-requests`)

- [x] 7.1 `OrderRequestsUiEvent.ScreenStarted`; `OrderRequestsScreen` sends it from `LifecycleEventEffect(ON_START)`
- [x] 7.2 ViewModel: `reload(quiet)` with `suppressNextLoadError`; the debounced `ordersChanged` observer; `ScreenStarted` with `hasStarted` (design decision 3)
- [x] 7.3 ViewModel: `settledOrderIds`, `dropSettled` on success / `AlreadyHandled` / `NotFound` of both actions, and the stale-page filter in `applyOutcome` (design decision 4)
- [x] 7.4 UiState: `SubmitError` and `OrderDetailUiState.submitError`; ViewModel network branches and `ProductNotSellable` set it instead of a snackbar; `startMutation` clears it (design decision 5)
- [x] 7.5 Detail dialog: inline `submitError` above the actions and in `OrderRejectForm`; `DialogErrorState(onRetry)`; `OrderItemRow` list price, deleted-product warning and "Ahora:" line; `OrderDetailActions(confirmEnabled)` (design decision 6)
- [x] 7.6 Strings in `values` / `values-en`: `order_item_list_price` ("antes %1$s"), `order_item_product_deleted`, `order_item_current_name` ("Ahora: %1$s"), `order_error_not_sellable`, `order_retry` (if missing)
- [x] 7.7 Tests: the signal reloads the pending list; a burst reloads once; the first `ScreenStarted` is ignored and the second reloads; a quiet failure emits no snackbar; confirm `200` with a failing reload removes the card without an error; a stale page after confirm does not bring the card back; `AlreadyHandled` drops it; network error on confirm keeps the dialog with `submitError` and no snackbar (same for reject); `ProductNotSellable` keeps the dialog with the product name; retrying the detail requests it again

## 8. Inventario (`features:product-inventory`)

- [x] 8.1 Events `ScreenStarted`, `PullToRefresh`; `InventoryUiState.isRefreshing`
- [x] 8.2 ViewModel per design decision 7 (`hasStarted`, `userInitiated` errors, `isRefreshing` cleared on both outcomes)
- [x] 8.3 `InventoryScreen`: `LifecycleEventEffect(ON_START)`; `InventoryContent`: `PullToRefreshBox` around the list area, scrollable empty and error states, FAB outside
- [x] 8.4 Tests: two `ScreenStarted` → one extra `getProducts` and rows kept; pull sets and clears `isRefreshing`; a failed pull shows the error and keeps the rows; a failed quiet start-reload says nothing

## 9. Verification

- [x] 9.1 Run, one at a time: `:core:network:testDebugUnitTest`, `:core:domain:test`, `:core:data:testDebugUnitTest`, `:core:notifications:testDebugUnitTest`, `:app:testDebugUnitTest`, `:features:order-requests:testDebugUnitTest`, `:features:product-inventory:testDebugUnitTest`, then `testDebugUnitTest`
- [x] 9.2 Run `./gradlew lintDebug` and `./gradlew assembleDebug`

## 10. Manual — QA

- [ ] 10.1 Manual — S:SOL-061 / X:XPE-021: Solicitudes open, `sim-tablet.sh` creates 3 orders → in ≤ 3 s the list, "Pendientes (N)" and the badge grow by 3 without "Actualizar"; nginx shows one `GET /orders?status=PENDING…` per burst
- [ ] 10.2 Manual — X:EXP-001: phone locked, new order, tap the notification → the list has it; same with Solicitudes already open
- [ ] 10.3 Manual — Reject an order by curl while Solicitudes is open → its card disappears when `ORDER_TAKEN` arrives
- [ ] 10.4 Manual — S:RED-012: Ver detalle → Confirmar pedido → airplane mode in < 1 s → "Solicitud confirmada", the card leaves, chip and badge drop, no load-error snackbar; with network, "Actualizar" does not bring it back
- [ ] 10.5 Manual — S:EXP-008: detail open, airplane mode, Confirmar pedido → the error reads in red inside the dialog; X:EXP-013: without network, Ver detalle shows the error with "Reintentar"
- [ ] 10.6 Manual — S:EXP-004 / X:EXP-012: create a product by curl, Inicio → Inventario → it is listed; pull down on "0 productos" → new `GET /products`; delete one by curl and come back from its detail with Back → it is gone; confirm a sale in Solicitudes and return → the row's stock changed
- [ ] 10.7 Manual — X:XCA-032: an order with a product deleted by `?force=true` → the item says "Producto eliminado del inventario" and "Confirmar pedido" is disabled; a discounted item shows "antes S/ …"
