## Why

Two HIGH findings of the 2026-11-03 QA regression (`docs/handoff/android-regresion-2026-11-03.md` §3.2, §3.3) leave the merchant looking at stale lists:

- **F-80** — Solicitudes never re-queries on its own. A push only paints or cancels the tray notification, so new orders never appear. Coming back to the tab restores the old list (`saveState`/`restoreState`). The chip and the badge read a count that the list never received ("Pendientes (3)" with 0 cards). Tapping a notification while Solicitudes is already open does nothing. Orders already rejected elsewhere stay as pending until "Actualizar" is tapped.
- **F-79** — Inventario loads once. Coming back to the tab, or back from a detail with Back, shows the old list. A product created elsewhere is "0 productos" in the search, a deleted one is still listed, and there is no pull-to-refresh.

Two smaller ones are on the same screen:

- **F-90** — After a confirm whose follow-up reload fails (the network drops right after the `200`), the card stays in Pendientes with its actions. Tapping Rechazar on it then says "Otro vendedor ya atendió este pedido" about an order the merchant just confirmed.
- **F-91** — A network error while confirming or rejecting from the detail dialog goes to the screen's snackbar, which is drawn *under* the dialog's scrim and cannot be read.

The order side of the backend's Fases 38–39 is also unused: each item carries `listPriceSnapshot` (the list price before the discount the order was charged with), `productActive` (`false` when the product was deleted) and `currentProductName` (when it was renamed). Confirming an order with a deleted product answers `409 PRODUCT_NOT_SELLABLE`, which today shows "Algo salió mal". This is the Solicitudes part of F-83; the delete part went in `product-save-safety`.

## What Changes

- **"Orders changed" signal.** `OrderRepository` gains `ordersChanged: Flow<Unit>` and `notifyOrdersChanged()`. A `NEW_ORDER` or `ORDER_TAKEN` push fires it and refreshes the pending count right away (fail-open, with a time cap, inside the FCM callback). So do tapping a notification while the app is running and coming back to Solicitudes. `ORDER_EXPIRING` changes nothing in the list.
- **Solicitudes re-queries by itself.** It reloads quietly on the signal (debounced, so a burst of pushes is one request) and when the screen starts again (back to the tab, back from the background), skipping the first start. A quiet reload that fails keeps the list and shows no snackbar.
- **Settled orders leave at once (F-90).** After a successful confirm or reject, or an "already handled" / "not found" answer, the order is removed from Pendientes immediately, and a stale response cannot bring it back. If the count cannot be re-read after the `200`, it is lowered by one locally.
- **Errors inside the dialog (F-91).** A network error while confirming or rejecting is shown in red inside the dialog, above its buttons, instead of a snackbar behind it. The detail's load error gets a "Reintentar" button.
- **Inventario refresh (F-79).** Coming back to the tab, back from a pushed screen or back from the background reloads quietly, skipping the first start. Pull-to-refresh is added, and it works on the empty and error states too. A pull that fails says so.
- **Order items (Fases 38–39, F-83):**
  - an item charged below its list price shows "antes S/ 100,00" struck through next to its price;
  - an item whose product was deleted shows "Producto eliminado del inventario" instead of the stock warning, and "Confirmar pedido" is disabled (rejecting is still allowed);
  - an item whose product was renamed shows "Ahora: <nombre>";
  - confirming an order whose product is no longer sellable (`409 PRODUCT_NOT_SELLABLE`) keeps the dialog open with "No se puede confirmar: «<producto>» ya no está a la venta. Recházalo para avisar al cliente."

## Capabilities

### New Capabilities

_None._

### Modified Capabilities

- `order-requests`: "Surface the pending-order count across the app" (refreshed on a push; lowered by one if it cannot be re-read after a confirm or reject), "Open the order detail as a dialog" (deleted/renamed products, list price, retry on error), "Confirm an order" (immediate removal, inline network error, not sellable, disabled with a deleted product), "Reject an order" (immediate removal, inline network error), "Refresh the order list on demand" (reload on return and on the orders-changed signal, quiet failures).
- `order-notifications`: "Display a local notification for each order push" (a `NEW_ORDER`/`ORDER_TAKEN` push refreshes the count and the open list; tapping while Solicitudes is open re-queries).
- `product-inventory`: "Product list display" (reload on return, pull-to-refresh).

## Impact

- **`core:network`** — `OrderItemDto` (`listPriceSnapshot`, `productActive`, `currentProductName`), `OrderProblemDto.reason`.
- **`core:domain`** — `OrderItem` (`listPrice`, `productActive`, `currentProductName`), `ConfirmOrderError.ProductNotSellable`, `OrderRepository.ordersChanged` / `notifyOrdersChanged()`, new use cases `ObserveOrdersChangedUseCase`, `NotifyOrdersChangedUseCase`, `HandleOrderPushUseCase`.
- **`core:data`** — `OrderRepositoryImpl` (signal, `409 PRODUCT_NOT_SELLABLE`, local count decrement), `OrderMappers`.
- **`core:notifications`** — `QhathiaMessagingService` calls `HandleOrderPushUseCase`.
- **`core:testing`** — `FakeOrderRepository`, `TestFixtures`.
- **`app`** — `MainShellViewModel.onOrdersDeepLink()`, `MainShell` calls it on a notification tap.
- **`features:order-requests`** — ViewModel (signal, `ScreenStarted`, quiet reloads, settled ids, inline errors), Screen (`LifecycleEventEffect`), detail dialog (item states, list price, inline error, retry), strings.
- **`features:product-inventory`** — ViewModel (`ScreenStarted`, `PullToRefresh`), Screen, Content (`PullToRefreshBox`), strings.
- **Depends on** `product-save-safety` (this branch is cut from it, because both touch `InventoryViewModel`).
- **Not in this change:**
  - tapping the notification *group* (F-54) — `notifications-routing`;
  - `confirmedAt`/`rejectedAt`/`expiredAt` — no Staff screen shows when an order was resolved, so there is nothing to feed. Add them with a UI that needs them;
  - periodic polling — push, return-to-screen and the shell's reconcile are enough, per the handoff.
