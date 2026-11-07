## Context

**Solicitudes.** `OrderRequestsViewModel` has a single load pipeline: `combine(searchQuery, selectedFilter, reloadTrigger)` → `flatMapLatest` → `applyOutcome`. `reloadTrigger` only moves on `Load`, `Retry`, `Refresh` and after a confirm or reject.
- The screen has no lifecycle hook. `MainShell.switchTab` saves and restores each tab, so the ViewModel and its old list survive a tab switch.
- `QhathiaMessagingService.onMessageReceived` only asks `OrderNotificationPresenter` to paint or cancel a notification.
- The shell's `ON_START` reconcile refreshes the shared pending count, but the screen never sees that page: hence "Pendientes (3)" with 0 cards.
- A notification tap while Solicitudes is open goes through `switchTab`, which `launchSingleTop` turns into a no-op. The Activity only gets `onNewIntent`, with no `ON_START`.

**F-90.** After a `200`, the ViewModel only calls `reload()`. If that reload fails, `applyOutcome` keeps the old `orders`, the confirmed order among them. **F-91.** A network error from the dialog goes to `ShowError` → the screen's `SnackbarHost`, which lives in the Activity window under the `Dialog`'s own window and scrim.

**Inventario (F-79).** `InventoryViewModel` loads in `init` and on explicit events; `refresh()` only runs when coming back with a result. There is a quiet fetch already (`fetchProducts(quiet = true)` keeps the rows and stays silent on failure). The content is a plain `Box`, with no pull-to-refresh. Inicio already solves "reload on return" with `LifecycleEventEffect(ON_START)` and a `hasStarted` flag that skips the first start.

**Order items (Fases 38–39).** `OrderItemDto` lacks `listPriceSnapshot`, `productActive` and `currentProductName`, all optional on the wire. `OrderProblemDto` lacks `reason`. `toConfirmError` maps `409 PRODUCT_NOT_SELLABLE` to `Unknown`.

Modules (no new module; graph unchanged):

```
core:notifications ──► core:domain ◄── core:data ──► core:network
app (MainShellViewModel) ─┘  ▲   (signal, 409 mapping, count fallback)   (item fields, reason)
features:order-requests ─────┤
features:product-inventory ──┘
core:testing: FakeOrderRepository, TestFixtures
```

## Goals / Non-Goals

**Goals:**
- What the merchant sees in Solicitudes follows the server within seconds of a push, and on every return to the screen.
- An order the merchant settled never reappears as actionable.
- Errors from the dialog are readable where the merchant is looking.
- Inventario is current on every return, and on demand.
- Order items say when their product was discounted, renamed or deleted, and a deleted product cannot be confirmed.

**Non-Goals:**
- Periodic polling, a global order cache, or building cards from the push payload (it carries no data, by design).
- The notification-group tap (F-54).
- `confirmedAt`/`rejectedAt`/`expiredAt`: no screen shows them.
- Changing `saveState`/`restoreState` on tab switches: it is what keeps each tab's filter, search and scroll.

## Decisions

### 1. A signal, not a cache: `ordersChanged` on `OrderRepository`

```kotlin
val ordersChanged: Flow<Unit>
fun notifyOrdersChanged()
```

`OrderRepositoryImpl` backs it with `MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = DROP_OLDEST)`. Each screen keeps its own query (filter + search) and the signal only says "ask again".

- *Why not hand the reconcile page to the screen:* that page is `PENDING` with `size=50` and no search, so it would be wrong for any other filter or query.
- Use cases (domain rule: one per action): `ObserveOrdersChangedUseCase`, `NotifyOrdersChangedUseCase`, `HandleOrderPushUseCase(event)`.
  - `NEW_ORDER` and `ORDER_TAKEN` → `notifyOrdersChanged()`, then `runCatching { refreshPendingOrderCount() }`.
  - `ORDER_EXPIRING` and anything else → nothing.
- *Why the count refresh lives in the use case:* the badge must move even when Solicitudes is not composed (another tab, or the app in the background with the process alive).

### 2. FCM callback: presenter first, then a time-capped sync

```kotlin
override fun onMessageReceived(message: RemoteMessage) {
    val data = message.data
    presenter.handleOrderPush(event = data[KEY_EVENT], orderId = data[KEY_ORDER_ID])
    runBlocking { withTimeoutOrNull(PUSH_SYNC_TIMEOUT_MS) { handleOrderPush(data[KEY_EVENT]) } }
}
```

- This follows the existing `onNewToken` pattern (`runBlocking` on FCM's worker thread).
- The notification is posted first, so a slow network can never delay or lose it.
- The cap (5 s) keeps the callback well within FCM's processing window. The use case never throws (`runCatching`).

### 3. Solicitudes: one quiet reload path for every trigger

`reload(quiet: Boolean = false)` bumps `reloadTrigger` and sets `suppressNextLoadError = quiet`. `applyOutcome` emits `ShowError(order_error_load)` only when that flag is false, then clears it. It still marks `isError`, so an empty list shows its `ErrorState` with Retry.
- **Signal:** `observeOrdersChangedUseCase().debounce(300 ms).onEach { reload(quiet = true) }` in `init`. A burst of three pushes becomes one query. The existing `flatMapLatest` already cancels a query that a newer one supersedes.
- **Lifecycle:** new `OrderRequestsUiEvent.ScreenStarted`, sent from `LifecycleEventEffect(ON_START)` in `OrderRequestsScreen`. The ViewModel reloads quietly from the second start on (`hasStarted`), the same as Inicio, so the first start does not double the `init` load.
- **Notification tap while running:** `MainShell`'s pending-destination effect calls `viewModel.onOrdersDeepLink()` after `switchTab`. `MainShellViewModel.onOrdersDeepLink()` calls `NotifyOrdersChangedUseCase`. On a cold start Solicitudes is created fresh, so the extra signal costs at most one debounced query.

### 4. Settled orders leave at once and stay gone (F-90)

The ViewModel keeps `settledOrderIds: MutableSet<String>`. `dropSettled(id)` adds the id and, when the filter is `PENDING`, removes the order from `state.orders`. Called on:
- confirm or reject `200`, after closing the dialog and before the snackbar, followed by `reload(quiet = true)`;
- `AlreadyHandled` and `NotFound` from either action, before the existing `reload()`.

In `applyOutcome`, a successful page drops any `PENDING` order whose id is in `settledOrderIds`: such a page was read before the mutation landed. Ids that the page no longer contains are removed from the set, so it does not grow.

Data side: after a `200`, `OrderRepositoryImpl.confirmOrder` / `rejectOrder` do `refreshPendingOrderCount().onFailure { pendingCount.update { (it - 1).coerceAtLeast(0) } }`.

- *Alternative:* show the card as "Confirmada" inside Pendientes. Rejected: the tab is `PENDING` only (handoff "No hacer").

### 5. Inline dialog errors (F-91)

`OrderDetailUiState.submitError: SubmitError?`, where `data class SubmitError(@StringRes val messageRes: Int, val arg: String? = null)`. It holds a resource id and an argument, never a built string, as everywhere in this module.
- `startMutation` clears it.
- The network branches of confirm and reject set `submitError = SubmitError(order_error_network)` instead of emitting `ShowError`, keeping the dialog open.
- `ProductNotSellable` sets `SubmitError(order_error_not_sellable, productName)` and keeps the dialog open: the merchant's next step is to reject from there.

`OrderDetailBody` (above `OrderDetailActions`) and `OrderRejectForm` (above its confirm button) render it in `colorScheme.error`, centered, as a polite live region. The detail's load error (`DialogErrorState`) gets `onRetry` → `OpenDetail(orderId)` and a `TextButton(order_retry)`.

### 6. Order item states (Fases 38–39, F-83)

- **DTO:** `listPriceSnapshot: Double? = null`, `productActive: Boolean = true`, `currentProductName: String? = null`.
- **Domain:** `OrderItem.listPrice: Double?`, `productActive: Boolean`, `currentProductName: String?`. The mapper passes them through.
- **`OrderItemRow`:**
  - `listPrice` above `unitPrice` → a struck-through `antes S/ x` next to the unit price, in `onSurfaceVariant` with `TextDecoration.LineThrough`;
  - `!productActive` → `DeletedProductWarning()` with the same error styling as `OutOfStockWarning`, taking the place of the stock `when`;
  - `currentProductName` → `bodySmall` `Ahora: …` under the name.
- **Confirm button:** `OrderDetailActions(confirmEnabled = detail.items.all { it.productActive })`.
- **Error mapping:** `OrderProblemDto.reason: String? = null`. `toConfirmError` maps `409` + `PRODUCT_NOT_SELLABLE` → `ConfirmOrderError.ProductNotSellable(productName, reason)`, with the screen copy in decision 5. It is not "already handled": the order is still `PENDING`, so it is not dropped from the list.

### 7. Inventario: start and pull

- Events `ScreenStarted` and `PullToRefresh`; state `isRefreshing`.
- `ScreenStarted` reloads categories and quietly fetches products from the second start on, unless a load is already showing.
- `PullToRefresh` sets `isRefreshing` and calls `fetchProducts(quiet = true, userInitiated = true)`.
- `fetchProducts` clears `isRefreshing` on both outcomes. On failure it emits `ShowError(inventory_error)` when `userInitiated`, even though the fetch is quiet.
- `InventoryScreen` adds `LifecycleEventEffect(ON_START) { onEvent(ScreenStarted) }`.
- `InventoryContent` wraps the list `Box`'s content in `PullToRefreshBox`, from the Material3 BOM already on the catalog. `EmptyState` and `ErrorState` get `verticalScroll(rememberScrollState())` so the gesture works with nothing to scroll. The FAB stays outside.
- A sale confirmed in Solicitudes is picked up on the return to the tab; no cross-feature invalidation is needed.

## Risks / Trade-offs

- [`runBlocking` in the FCM callback] → Same as the existing `onNewToken`. The callback runs on FCM's background thread, and the 5 s cap plus fail-open keep it bounded.
- [Extra queries] → One per burst of pushes (debounced) and one per return to the screen. The handoff measured pushes at ≤ 3 s; there is no polling.
- [`settledOrderIds` hides a server state] → It only hides an order that the server reports as `PENDING` after this device saw it leave `PENDING`. That only happens with a page read before the mutation. The id is forgotten once a page stops returning it.
- [Two error surfaces] → Errors of the dialog's own actions go inline; list-level messages (success snackbars, already handled, stock) stay in the snackbar, since the dialog is closed for those.
- [Pull-to-refresh with a nested `LazyColumn`] → `PullToRefreshBox` uses nested scroll, so the existing list scrolls as before; only the non-scrolling states needed `verticalScroll`.

## Migration Plan

Single merge, no flag, no persisted data. Rollback is reverting the commit. The new DTO fields are optional, so older backends decode as before.

## Open Questions

_None._
