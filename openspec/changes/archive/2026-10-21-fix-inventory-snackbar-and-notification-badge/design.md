## Context

Three independent defects from the 2026-10-18 triage, grouped because two of them live in the same file (`OrderNotificationPresenter`) and all three are small:

**Inventory snackbar (BUG-007).** `InventoryScreen` receives `registerResult` / `registeredProductId` from the back-stack entry's `SavedStateHandle` and handles them in `LaunchedEffect(registerResult)`. Each of the four branches awaits `snackbarHostState.showSnackbar(...)` and only then calls `onRegisterResultConsumed()`. `showSnackbar` suspends until the snackbar is dismissed; navigating away cancels the effect with a `CancellationException`, so the flag is never cleared and the effect fires again on return. `InventoryUiEffect` (the ViewModel's one-shot `SharedFlow`) is not involved.

**Tray notifications (BUG-006, BUG-008).** `QhathiaMessagingService.onMessageReceived` passes every data push to `OrderNotificationPresenter.showOrderNotification`, which always posts, with id `orderId.hashCode()` and no tag. `contentFor` knows `NEW_ORDER` and `ORDER_TAKEN`; `ORDER_EXPIRING` falls into the generic branch. Nothing ever cancels a notification except the user tapping it (`setAutoCancel(true)`).

**What the launcher badge is.** Since Android 8 the launcher badge is derived from the app's active notifications (a dot, or on some launchers a count of them, optionally weighted by `setNumber`). There is no platform API to set an arbitrary badge number. So "derive the badge from the server" can only mean: make the set of order notifications in the tray follow the server's pending orders.

**What already exists.** `OrderRepository` (a `@Singleton` in `core:data`) owns the app-wide `pendingOrderCount`, fed by any unfiltered `PENDING` query. `MainShellViewModel` refreshes it once in `init`; the shell is composed only when the merchant is signed in and unlocked. `MainActivity` already observes the process lifecycle for the biometric relock, but that observer sits above the auth gate.

## Goals / Non-Goals

**Goals:**
- A register/update result is handled once, whatever the merchant does while its snackbar is visible.
- One tray notification per order; `ORDER_TAKEN` removes it; `ORDER_EXPIRING` replaces it with its own text.
- The tray, and therefore the launcher badge, converges on the server's pending orders every time the app is opened, so a lost push cannot leave a permanent ghost.
- Keep `core:domain` free of Android types and keep the reconciliation logic unit-testable on the JVM.

**Non-Goals:**
- Reconciling while the app stays in the background (no periodic work, no `WorkManager`). A missed `ORDER_TAKEN` is corrected the next time the app is opened.
- Clearing order notifications on logout.
- Any in-app reaction to `ORDER_EXPIRING` (highlighting the order, a countdown).
- Changing `showSnackbar` semantics for `InventoryUiEffect.ShowError` / `ShowMessage`.

## Decisions

### 1. Consume the result before awaiting the snackbar

In each branch of `LaunchedEffect(registerResult)`: capture `registeredProductId` into a local `val`, call `onRegisterResultConsumed()`, call `viewModel.refresh()` where applicable, then await the snackbar. `showRegisteredSnackbar` takes the captured id as a parameter instead of reading the composable parameter.

Clearing the flag recomposes with `registerResult == null`, which restarts `LaunchedEffect(registerResult)` and **cancels the running effect** — including the snackbar we are about to show. To avoid that, the effect is keyed on something that does not change when the flag is cleared: the branch body is moved into a coroutine launched in a scope that outlives the key change (`rememberCoroutineScope()`), and the `LaunchedEffect` only dispatches. Alternative considered: `try { … } finally { onRegisterResultConsumed() }` — rejected because `finally` runs on cancellation too, so it fixes the reappearance, but it still couples consuming to the snackbar's lifetime, and the key change would still cancel the snackbar when the flag is cleared normally. The handoff suggests "consume first"; this decision is how to do that without the effect cancelling itself.

The `rememberCoroutineScope()` scope is cancelled when `InventoryScreen` leaves composition, which dismisses a visible snackbar on navigation — the expected behaviour — and by then the flag is already cleared.

### 2. Notifications keyed by tag = orderId

Post with `notify(tag = orderId, id = ORDER_NOTIFICATION_ID)` (a constant) instead of `notify(orderId.hashCode(), …)`. The tag is the order id itself, so the tray can be read back as "which orders have a notification" without a hash table, and there is no hash collision between two orders. A push without `orderId` keeps the rolling id and a `null` tag.

The `PendingIntent` request code stays derived from the order id (it must still differ per order so `FLAG_UPDATE_CURRENT` does not rewrite other notifications' extras).

### 3. Event handling in the presenter

```
NEW_ORDER      → post (tag = orderId)
ORDER_EXPIRING → post (tag = orderId)   // same tag ⇒ replaces NEW_ORDER
ORDER_TAKEN    → cancel(tag = orderId, ORDER_NOTIFICATION_ID)   // no-op if absent
else           → post generic (tag = orderId)
```

`ORDER_TAKEN` with a null `orderId` does nothing. The method is renamed `handleOrderPush` to reflect that it does not always show something; `QhathiaMessagingService` is updated. The `notif_order_taken_title`, `notif_order_confirmed_body` and `notif_order_rejected_body` strings become unused and are removed (es and en); `notif_order_updated_body` stays for the generic branch.

New strings: `notif_order_expiring_title` ("Orden por vencer" / "Order about to expire") and `notif_order_expiring_body` ("Una orden pendiente está por vencer. Atiéndela pronto." / "A pending order is about to expire. Handle it soon."). The text does not state "10 minutos": the lead time is a backend variable (`ORDERS_EXPIRING_WARNING_MINUTES`).

### 4. A domain port for the tray, implemented in `core:notifications`

```kotlin
// core:domain — repository/OrderNotificationTray.kt
interface OrderNotificationTray {
    /** Order ids that currently have a notification in the tray. */
    fun activeOrderIds(): Set<String>
    fun cancel(orderId: String)
    /** Cancels order notifications that carry no order id (no tag). */
    fun cancelUnmatched()
}
```

`OrderNotificationPresenter` implements it, reading `NotificationManager.activeNotifications` (API 23+, below our min SDK 26) filtered to the `orders` channel. A tagged entry is an order id; an untagged one is "unmatched" and is cancelled by its own id. The channel filter also catches notifications posted by the previous app version (hash id, no tag), so they are cleared on the first open after the update. `NotificationsModule` binds it next to `PushTokenProvider`; both bindings point at the same `@Singleton` presenter.

This follows the existing `PushTokenProvider` pattern: the port is in `core:domain`, the Android implementation in `core:notifications`, the module graph is unchanged (`core:notifications → core:domain` already exists).

### 5. `ReconcileOrderNotificationsUseCase` in `core:domain`

```
tray.cancelUnmatched()
active = tray.activeOrderIds()
if active is empty → repository.refreshPendingOrderCount(); return
page = repository.getOrders([PENDING], query = null, page = 0, size = RECONCILE_PAGE_SIZE /* 50 */)
on failure → return (leave the tray)
if page.totalElements > page.orders.size → return   // cannot rule anything out
pending = page.orders.map { it.id }.toSet()
for id in active - pending → tray.cancel(id)
```

Unmatched notifications are cancelled up front and without a server read: with no order id they can never be matched, so keeping them only feeds the badge.

The unfiltered `PENDING` query already publishes `pendingOrderCount` inside `OrderRepositoryImpl`, so no second request is needed when the tray is not empty. When the total exceeds the page, unknown ids are kept: cancelling a notification for an order that is in fact still pending would hide work, which is worse than a ghost that the next open will clear. 50 is far above a store's realistic pending backlog. No new repository method is needed.

### 6. Trigger: `ON_START` in the shell, not in `MainViewModel`

`MainShell` adds `LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.onForegrounded() }`. Because the shell is only composed for a signed-in, unlocked merchant, the "locked or signed out" scenario holds by construction, and the effect also fires when the shell first enters composition (the lifecycle replays `ON_START` to a new observer of a started owner) — which covers first entry and unlock. `MainShellViewModel.onForegrounded()` launches the use case; the `init` refresh is removed because `ON_START` now covers it.

`MainViewModel`'s process-lifecycle observer was considered and rejected: it fires while locked or signed out, so it would have to re-implement the auth gate.

## Risks / Trade-offs

- **Ghost survives until the next open** → accepted (Non-Goal). The common case — the `ORDER_TAKEN` push does arrive — is fixed immediately by the cancel.
- **Behaviour change: no tray notice when another device confirms or rejects** → intended; the handoff calls it "nothing to do". The Solicitudes list still shows the result.
- **Launcher variance** → some launchers show a dot, some a count; both are driven by active notifications, so both become correct. No vendor badge SDK is added.
- **`activeNotifications` can throw on some OEM builds when the service is dying** → the tray implementation catches and returns an empty list, so reconciliation degrades to a count refresh.
- **Effect restructure in `InventoryScreen`** → the cancellation subtlety in Decision 1 is the one thing a reviewer must check; the feature modules have no Compose UI test setup, and adding one is out of scope, so it is covered by a manual pass (navigate away and back mid-snackbar, for all four result kinds) plus a code comment at the effect.
- **Notifications from the old version after update** → handled by the channel filter in Decision 4; they are cancelled on the first open.
