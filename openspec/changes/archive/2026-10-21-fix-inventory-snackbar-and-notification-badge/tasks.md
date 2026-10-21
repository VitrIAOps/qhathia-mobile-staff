## 1. Inventory result consumed once (BUG-007)

- [x] 1.1 `features:product-inventory` — in `InventoryScreen`, add `val scope = rememberCoroutineScope()`; in `LaunchedEffect(registerResult)`, for a non-null result capture `registeredProductId` into a local `val`, call `onRegisterResultConsumed()`, then `scope.launch { … }` the branch body (refresh where it applies + snackbar). The `LaunchedEffect` itself no longer awaits any snackbar (design Decision 1)
- [x] 1.2 Change `showRegisteredSnackbar` to take the captured product id as a parameter instead of reading the composable parameter, so `Ver producto` works after the flag is cleared
- [x] 1.3 Apply 1.1 to all four branches: `SUCCESS`, `PLAN_LIMIT`, `DISCOUNT_FAILED`, `PRODUCT_UPDATED`
- [x] 1.4 Add a short comment at the effect saying why it consumes first and why the snackbar runs in `scope` (clearing the flag restarts the effect)
- [x] 1.5 `./gradlew :features:product-inventory:testDebugUnitTest :features:product-inventory:lintDebug`

## 2. Domain port and reconciliation use case

- [x] 2.1 `core:domain` — add `repository/OrderNotificationTray.kt` with `activeOrderIds(): Set<String>`, `cancel(orderId: String)` and `cancelUnmatched()`, KDoc'd as the port to the device's tray (no Android types)
- [x] 2.2 `core:domain` — add `usecase/ReconcileOrderNotificationsUseCase.kt` implementing design Decision 5 (`RECONCILE_PAGE_SIZE = 50`), returning `Unit` and never throwing
- [x] 2.3 `core:testing` — add `FakeOrderNotificationTray` (settable active ids, records cancelled ids and `cancelUnmatched` calls)
- [x] 2.4 `core:domain` tests for the use case with `FakeOrderRepository` + `FakeOrderNotificationTray`: empty tray only refreshes the count and does not list orders; ids not pending are cancelled and pending ones kept; zero pending cancels all; `totalElements` > page cancels nothing; a failed read cancels nothing; `cancelUnmatched` is always called; the list query is unfiltered `PENDING`, page 0, size 50
- [x] 2.5 Confirm `core:domain` keeps zero Android dependencies (`./gradlew :core:domain:test`)

## 3. Presenter: one notification per order, new event handling (BUG-006, BUG-008)

- [x] 3.1 `core:notifications` — extract the event decision into a pure function (e.g. `orderPushAction(event, orderId)` → `Post(kind)` / `Cancel(orderId)` / `Ignore`) so it is JVM-testable: `NEW_ORDER` → post new-order; `ORDER_EXPIRING` → post expiring; `ORDER_TAKEN` with id → cancel; `ORDER_TAKEN` without id → ignore; anything else → post generic
- [x] 3.2 Rename `showOrderNotification` to `handleOrderPush`, drive it from 3.1, and post with `notify(tag = orderId, ORDER_NOTIFICATION_ID, …)`; keep the rolling id with a null tag when `orderId` is null; keep the per-order `PendingIntent` request code
- [x] 3.3 Update `QhathiaMessagingService.onMessageReceived` to call `handleOrderPush`
- [x] 3.4 Strings (es + en): add `notif_order_expiring_title` ("Orden por vencer" / "Order about to expire") and `notif_order_expiring_body` ("Una orden pendiente está por vencer. Atiéndela pronto." / "A pending order is about to expire. Handle it soon."); remove `notif_order_taken_title`, `notif_order_confirmed_body`, `notif_order_rejected_body` and the now unused `STATUS_*` constants
- [x] 3.5 Implement `OrderNotificationTray` in `OrderNotificationPresenter`: read `NotificationManager.activeNotifications` filtered to the `orders` channel; tagged → order id; `cancelUnmatched()` cancels untagged ones by their own id (this clears notifications from the previous app version); wrap the reads in `try/catch` and return an empty set on failure
- [x] 3.6 Bind `OrderNotificationTray` to the presenter in `NotificationsModule`
- [x] 3.7 Add the JUnit 5 test source set to `core:notifications` (dependencies are already declared) and test the pure decision from 3.1 for every event, with and without `orderId`
- [x] 3.8 Update the KDoc of the presenter and the service: content still comes only from `event`/`status`, `ORDER_TAKEN` now cancels
- [x] 3.9 `./gradlew :core:notifications:testDebugUnitTest :core:notifications:lintDebug`

## 4. Trigger from the shell

- [x] 4.1 `app` — `MainShellViewModel`: inject `ReconcileOrderNotificationsUseCase`, add `onForegrounded()` that launches it, and remove the `init` refresh; update the class KDoc
- [x] 4.2 `app` — `MainShell`: add `LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.onForegrounded() }`
- [x] 4.3 `MainShellViewModelTest`: `onForegrounded()` runs the reconciliation (and so refreshes the count); creating the ViewModel alone no longer refreshes; adjust existing tests that relied on the `init` refresh
- [x] 4.4 `./gradlew :app:testDebugUnitTest :app:lintDebug`

## 5. Spec and docs

- [x] 5.1 Grep for `showOrderNotification`, `notif_order_taken_title`, `notif_order_confirmed_body`, `notif_order_rejected_body` to confirm nothing still references them
- [x] 5.2 Mark §2.3, §3.4 and §4.1 as done in `docs/handoff/android-pendientes.md` (including the `ORDER_EXPIRING` row of "Resumen de campos nuevos")

## 6. Verification

- [x] 6.1 Full build and tests: `./gradlew testDebugUnitTest lintDebug assembleDebug`
- [ ] 6.2 Manual — Inventory: for each of registered, discount failed, plan limit and product updated, leave Inventory while the snackbar is visible (open a product, switch tab) and come back: the snackbar does not reappear; `Ver producto` still scrolls to the new row
- [ ] 6.3 Manual — notifications on a device: a `NEW_ORDER` then `ORDER_EXPIRING` for the same order leaves one notification with the expiring text; confirming that order from another device removes it and the launcher badge drops
- [ ] 6.4 Manual — lost push: with a notification in the tray, turn off the network, confirm the order from another device, turn the network back on and open the app: the notification disappears
- [ ] 6.5 Manual — update path: install the previous build, receive a push, update to this build and open the app: the old notification is cleared
