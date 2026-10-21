## Why

Two behaviours that merchants hit on every use are wrong, and both come from state that outlives the moment it belongs to (triage 2026-10-18, `docs/handoff/android-pendientes.md` §2.3, §4.1 and §3.4):

- **The Inventory snackbar comes back** (BUG-007). `InventoryScreen` clears the register-result flag only *after* `showSnackbar` returns, and `showSnackbar` suspends until the snackbar is dismissed. Navigating away cancels the effect, the flag is never cleared, and since it lives in the back-stack entry's `SavedStateHandle` the snackbar reappears every time the merchant comes back ("salió como 3 veces").
- **The launcher badge counts orders that are already handled** (BUG-006). Every push posts a notification — `ORDER_TAKEN` included — and none is ever cancelled. An order someone else confirmed stays in the tray as "Orden actualizada", and the launcher badge counts it. Pushes are best-effort, so even a correct add/remove pair would drift whenever a push is lost to Doze or no network.
- **The "order about to expire" push says "El estado de una orden cambió"** (BUG-008). The backend has been sending `ORDER_EXPIRING` 10 minutes before a pending order expires since 2026-10-18; the app does not know the event and falls into the generic text, which hides the one thing the push is for.

## What Changes

- **Inventory:** the register-result flag is consumed as soon as its effect starts, before any snackbar is awaited, in all four result branches (success, plan limit, discount failed, product updated). The `Ver producto` action keeps working because the registered product id is captured before the flag is cleared.
- **Notifications — `ORDER_TAKEN` cancels:** an `ORDER_TAKEN` push removes that order's notification from the tray instead of posting "Orden actualizada". **BREAKING (behaviour):** the merchant no longer gets a tray notification when an order is confirmed or rejected on another device.
- **Notifications — `ORDER_EXPIRING`:** a new event with its own title and body (es/en). It replaces the order's existing notification rather than adding a second one, because the order is still the same pending order.
- **Notifications — one per order:** each order has at most one notification in the tray, keyed by the order id, so an order's events replace each other instead of piling up.
- **Notifications — reconciled with the server:** every time the app comes to the foreground with a signed-in merchant, the tray is checked against the server's list of pending orders, and notifications for orders that are no longer pending are cancelled. The launcher badge (which Android derives from the notifications in the tray) therefore follows the same server count as the Solicitudes tab badge, and recovers from lost pushes the next time the app is opened.

## Capabilities

### New Capabilities

_None._

### Modified Capabilities

- `order-notifications`: `ORDER_TAKEN` stops posting a notification and cancels the order's notification instead; `ORDER_EXPIRING` gets its own notification; an order has at most one notification; the tray is reconciled with the server's pending orders when the app is foregrounded.
- `product-inventory`: the registration/update result snackbar is shown once per result and does not reappear when the merchant navigates away and back.

## Impact

- **`core:domain`** — new port `OrderNotificationTray` (which orders have a notification, cancel one, cancel the unknown ones) and a new `ReconcileOrderNotificationsUseCase` built on it and on `OrderRepository`. Pure Kotlin.
- **`core:notifications`** — `OrderNotificationPresenter` keys notifications by order id (tag), handles `ORDER_EXPIRING`, and cancels on `ORDER_TAKEN`; it implements `OrderNotificationTray` and binds it in `NotificationsModule`. New strings (es/en); the `ORDER_TAKEN` body strings become unused and are removed.
- **`core:testing`** — a `FakeOrderNotificationTray`.
- **`app`** — `MainShell` triggers the reconciliation on `ON_START`; `MainShellViewModel` calls the use case (it also refreshes the pending count, so the separate refresh in `init` goes).
- **`features:product-inventory`** — `InventoryScreen` effect ordering only.
- **No API changes.** Uses `GET /api/v1/orders?status=PENDING` (already consumed) and the `ORDER_EXPIRING` payload documented in `docs/android/api-contract.md` §25.
- **Not in scope:** clearing order notifications on logout, the in-app reaction to `ORDER_EXPIRING` (e.g. highlighting the order in Pendientes), and the low-stock-at-zero fix (§4.2), which is a separate change.
