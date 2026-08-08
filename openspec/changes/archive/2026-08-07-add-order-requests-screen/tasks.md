## 1. Create the `features:order-requests` module

- [x] 1.1 Create the module directories: `features/order-requests/src/main/java/com/qhathia/mobile/staff/feature/orderrequests/{model,navigation,ui}`, `features/order-requests/src/main/res/values`, and `features/order-requests/src/test/java/com/qhathia/mobile/staff/feature/orderrequests/ui`
- [x] 1.2 Write `features/order-requests/build.gradle.kts` from the Feature module template (`android.library` + `kotlin.compose` + `ksp` + `hilt`, namespace `com.qhathia.mobile.staff.feature.orderrequests`, compileSdk 36 / minSdk 26, `buildFeatures.compose`, `unitTests.all { it.useJUnitPlatform() }`), depending on `:core:domain`, `:core:ui`, `:core:common` plus Compose BOM, material3, material-icons-extended, lifecycle, navigation-compose, kotlinx-immutable, Hilt (`ksp(libs.hilt.compiler)` + `ksp(libs.kotlin.metadata.jvm)`) and the JUnit5/MockK/Turbine test set — mirror `features/product-inventory/build.gradle.kts`
- [x] 1.3 Register the module in `settings.gradle.kts` with `include(":features:order-requests")`
- [x] 1.4 Create the minimal `features/order-requests/src/main/AndroidManifest.xml` (`<manifest />`)
- [x] 1.5 Verify the module resolves: `./gradlew :features:order-requests:compileDebugKotlin`

## 2. Domain layer — `core:domain`

- [x] 2.1 Add `model/OrderStatus.kt` — `PENDING`, `CONFIRMED`, `REJECTED`, `CANCELLED`, `EXPIRED`
- [x] 2.2 Add `model/Order.kt` — order summary (`id`, `status`, `customerName`, `customerPhone`, `orderCode`, `itemCount`, `total`, `expiresAt`, `rejectionReason`, `createdAt`) plus `OrderDetail` with `items: List<OrderItem>`; `expiresAt`/`createdAt` as `kotlinx.datetime`-free `Instant`-equivalent (`java.time.Instant`, available on minSdk 26)
- [x] 2.3 Add `model/OrderItem.kt` — `id`, `productId`, `productName`, `quantity`, `unitPrice`, `lineTotal`, `productStock: Int?`, `productAvailability`
- [x] 2.4 Add `model/OrderPage.kt` — `orders: List<Order>` + `totalElements` (feeds the Pendientes badge)
- [x] 2.5 Add `model/ConfirmOrderError.kt` and `model/RejectOrderError.kt` sealed hierarchies with their `*Exception` wrappers, following the `RegisterProductError` precedent: `StockInsufficient(productName, available, requested)` (confirm only), `AlreadyHandled`, `NotFound`, `Network`, `Unknown`
- [x] 2.6 Add `repository/OrderRepository.kt` — `getOrders(statuses, query, page, size): Result<OrderPage>`, `getOrderDetail(id): Result<OrderDetail>`, `confirmOrder(id): Result<OrderDetail>`, `rejectOrder(id): Result<OrderDetail>`
- [x] 2.7 Add `usecase/GetOrdersUseCase.kt`, `usecase/GetOrderDetailUseCase.kt`, `usecase/ConfirmOrderUseCase.kt`, `usecase/RejectOrderUseCase.kt`

## 3. Network layer — `core:network`

- [x] 3.1 Add `dto/OrderDtos.kt` — `@Serializable` `OrderSummaryDto`, `OrderPageDto` (`content`, `totalElements`, `totalPages`, `size`, `number`), `OrderDetailDto`, `OrderItemDto` matching §9 of `docs/android/api-contract.md`; nullable `orderCode`, `expiresAt`, `productStock`, `productAvailability`, `rejectionReason`
- [x] 3.2 Add `dto/OrderProblemDtos.kt` — the `STOCK_INSUFFICIENT` ProblemDetail shape (`errorCode`, `productId`, `productName`, `available`, `requested`), tolerant of unknown keys
- [x] 3.3 Add `service/OrderApiService.kt` on the authenticated client: `getOrders(status, q, page, size)` sending `status` as a comma-joined list and omitting null params (follow `ProductApiService`'s optional-parameter style), `getOrderDetail(id)`, `confirmOrder(id)`, `rejectOrder(id)` (no body)
- [x] 3.4 In `confirmOrder`, read the error response body on `422` and surface the parsed `STOCK_INSUFFICIENT` / `INVALID_STATE_TRANSITION` `errorCode` alongside the status, so the repository can distinguish the two
- [x] 3.5 Provide `OrderApiService` from `NetworkModule` using the authenticated `HttpClient` qualifier

## 4. Data layer — `core:data`

- [x] 4.1 Add `mapper/OrderMappers.kt` — DTO → domain for summary, detail and item; parse `expiresAt`/`createdAt` to `Instant`, map unknown/absent `status` defensively, keep `orderCode` nullable
- [x] 4.2 Add `repository/OrderRepositoryImpl.kt` implementing `OrderRepository`, mapping `NetworkResult` → `Result` at the boundary
- [x] 4.3 Map confirm failures to `ConfirmOrderError`: `422 STOCK_INSUFFICIENT` → `StockInsufficient(productName, available, requested)`, `422 INVALID_STATE_TRANSITION` → `AlreadyHandled`, `404` → `NotFound`, network → `Network`, else `Unknown`; map reject failures to the equivalent `RejectOrderError`
- [x] 4.4 Bind `OrderRepositoryImpl` to `OrderRepository` in `DataModule` (`@Binds @Singleton`)

## 5. Test doubles — `core:testing`

- [x] 5.1 Add `FakeOrderRepository` with settable results per method and recorded call arguments (statuses, query, page) so ViewModel tests can assert what was requested
- [x] 5.2 Extend `TestFixtures` with `order(...)`, `orderDetail(...)` and `orderItem(...)` builders, including an expiring-soon order and an order with null `orderCode` / null `customerName`

## 6. MVI contract — `features:order-requests`

- [x] 6.1 Add `model/OrderRequestsFilter.kt` — `PENDING`, `CONFIRMED`, `REJECTED` tabs, each carrying the backend status list it maps to (`REJECTED` → `REJECTED,EXPIRED`)
- [x] 6.2 Add `model/OrderRequestsUiState.kt` — `orders`, `selectedFilter`, `pendingCount`, `searchQuery`, `isLoading`, `isError`, `lastUpdatedAt`, and a nullable `detail: OrderDetailUiState?` (`order`, `isLoading`, `isError`, `isSubmitting`)
- [x] 6.3 Add `model/OrderRequestsUiEvent.kt` — `Load`, `Retry`, `Refresh`, `SelectFilter`, `Search`, `OpenDetail`, `DismissDetail`, `ConfirmOrder`, `RejectOrder`
- [x] 6.4 Add `model/OrderRequestsUiEffect.kt` — `ShowConfirmedSnackbar`, `ShowRejectedSnackbar`, `ShowError(@StringRes)` with the severity needed to colour the snackbar green vs red
- [x] 6.5 Add `res/values/strings.xml` with every literal: `Solicitudes de compra`, `Última actualización a las %1$s`, `Buscar por nombre o código`, `Pendientes (%1$d)`, `Aceptados`, `Rechazados`, `Código: %1$s`, `Total: %1$s`, `Expira en %1$s`, `Rechazar`, `Ver detalle`, `Detalle de solicitud #%1$s`, `Total del pedido:`, `Confirmar pedido`, `Quedan pocos disponibles`, `Solicitud confirmada`, `Solicitud rechazada`, the three empty-state messages, and the stock/conflict error messages

## 7. ViewModel — `features:order-requests`

- [x] 7.1 Add `ui/OrderRequestsViewModel.kt` (`@HiltViewModel`) exposing `uiState: StateFlow` + `uiEffect: SharedFlow`, loading `PENDING` on init
- [x] 7.2 Implement server-side loading: every load sends the active filter's status list and the current query; success sets `orders` and `lastUpdatedAt`, failure sets `isError` while keeping previously loaded orders and the previous `lastUpdatedAt`
- [x] 7.3 Implement the pending badge: alongside each load, issue a `size=1` `PENDING` query and store its `totalElements` as `pendingCount`, so the badge stays accurate on the other tabs
- [x] 7.4 Implement debounced search (~350 ms) over a query `MutableStateFlow` with `flatMapLatest`, so no stale response can overwrite a newer one; clearing the box reloads without `q`
- [x] 7.5 Implement `OpenDetail` → fetch detail into `detail`, with its own loading/error state that leaves the list intact; `DismissDetail` → clear `detail` with no snackbar and no refresh
- [x] 7.6 Implement `ConfirmOrder`: on success close the dialog, emit `ShowConfirmedSnackbar`, reload the list; on `StockInsufficient` / `AlreadyHandled` / `NotFound` close the dialog, emit the matching `ShowError` and reload; on `Network` keep the dialog open and emit a retryable error
- [x] 7.7 Implement `RejectOrder` from both the card and the dialog (no reason sent): on success emit `ShowRejectedSnackbar` and reload; apply the same conflict handling as confirm
- [x] 7.8 Guard double submissions with `isSubmitting` so a second tap cannot fire a second confirm/reject

## 8. UI — `features:order-requests`

- [x] 8.1 Add `ui/OrderRequestsScreen.kt` — stateful composable collecting state with `collectAsStateWithLifecycle()`, hosting the `SnackbarHost` and mapping `UiEffect` to green/red snackbars
- [x] 8.2 Add `ui/OrderRequestsContent.kt` — stateless composable: gradient header (title, `Última actualización a las …`, refresh icon), search box, filter chips row, list / loading / error / empty states
- [x] 8.3 Add `ui/OrderRequestsHeader.kt` and `ui/OrderFilterChips.kt` — the chips render `Pendientes (n)` with a count and the other two without, per the design
- [x] 8.4 Add `ui/OrderCard.kt` — title (customer name, or `#<orderCode>` when absent), `Código:` line with the id-prefix fallback when `orderCode` is null, total, countdown in the top-right, and the `Rechazar` / `Ver detalle` actions
- [x] 8.5 Add `ui/ExpiryCountdown.kt` — formats remaining time from `expiresAt` against a `now` value, switching to the red pill treatment at ≤ 3 minutes and disabling actions at zero
- [x] 8.6 Drive `now` from a single one-second `LaunchedEffect` ticker at the list level (not one per card), and set stable `key`s on `items()` so only the countdown label subtree recomposes
- [x] 8.7 Add `ui/OrderDetailDialog.kt` — title `Detalle de solicitud #<code>`, item rows (placeholder thumbnail, name, unit price, quantity, line total), per-item `Quedan pocos disponibles` warning when `productStock` is non-null and at or below the threshold, order total block, `Rechazar` / `Confirmar pedido` actions, plus loading/error/submitting states
- [x] 8.8 Add `ui/OrderEmptyState.kt` — centered message worded per active filter (`Sin solicitudes pendientes` / `aceptadas` / `rechazadas`)
- [x] 8.9 Add `@Preview` composables (light + dark) for `OrderRequestsContent`, `OrderCard` and `OrderDetailDialog`, including the expiring-soon and low-stock variants
- [x] 8.10 Add `navigation/OrderRequestsNavigation.kt` — `ORDER_REQUESTS_ROUTE` constant and `NavGraphBuilder.orderRequestsGraph()`

## 9. App wiring — `:app`

- [x] 9.1 Add `implementation(project(":features:order-requests"))` to `app/build.gradle.kts`
- [x] 9.2 In `MainShell.kt`, replace the `REQUESTS_ROUTE` `PlaceholderScreen` composable with `orderRequestsGraph()` and point the Solicitudes bottom-nav destination at `ORDER_REQUESTS_ROUTE`
- [x] 9.3 Remove the now-unused `REQUESTS_ROUTE` constant and confirm the bottom bar still highlights the Solicitudes tab via the `hierarchy` check
- [x] 9.4 Confirm no new manifest permissions or components are needed (this feature only performs HTTP calls, already covered by `INTERNET`)

## 10. Tests

- [x] 10.1 `core:data` — `OrderMappersTest`: summary/detail/item mapping, null `orderCode`, null `productStock`, `EXPIRED` status, timestamp parsing
- [x] 10.2 `core:data` — `OrderRepositoryImplTest`: success mapping plus each error mapping (`STOCK_INSUFFICIENT` with its detail fields, `INVALID_STATE_TRANSITION`, `404`, network)
- [x] 10.3 `core:network` — `OrderApiServiceTest` with a Ktor `MockEngine`: comma-joined `status`, `q`/`page`/`size` params omitted when null, and `422` body parsing on confirm
- [x] 10.4 `features:order-requests` — `OrderRequestsViewModelTest` covering: `PENDING` requested on init; filter change re-queries with the right status list (`REJECTED,EXPIRED` for the Rechazados tab); debounced search sends `q` and the latest query wins; pending count comes from `totalElements`; detail fetch populates the dialog; dismiss changes nothing; confirm → snackbar + reload; reject → snackbar + reload; `StockInsufficient` and `AlreadyHandled` → error + reload; failed refresh keeps previous orders and `lastUpdatedAt`
- [x] 10.5 Run `./gradlew test` and fix failures

## 11. Definition of Done

- [x] 11.1 `./gradlew lint` passes with no new warnings in the touched modules
- [x] 11.2 `./gradlew assembleDebug` succeeds
- [x] 11.3 Screen composables have working light and dark `@Preview`s
- [ ] 11.4 Manual pass on device: default Pendientes tab with count, search by name and by code, countdown turning red near expiry, detail dialog with low-stock warning, confirm → green snackbar + refreshed list, reject → red snackbar + refreshed list, dismiss → no snackbar and no refresh, and the empty state on each tab
