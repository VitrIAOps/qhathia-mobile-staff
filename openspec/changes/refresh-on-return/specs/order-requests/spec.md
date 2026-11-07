## MODIFIED Requirements

### Requirement: Surface the pending-order count across the app

The number of pending orders SHALL be an application-wide value, held by the order data layer and observable by any screen. It MUST be displayed as a count badge on the **Solicitudes** bottom-navigation tab whenever it is greater than zero, and MUST be the same value shown on the Inicio bell and in the `Pendientes (N)` filter label. The value MUST be refreshed when the authenticated shell is entered, whenever the order list is queried for pending orders, after an order is successfully confirmed or rejected, and when a `NEW_ORDER` or `ORDER_TAKEN` push arrives. When the count cannot be re-read after a successful confirm or reject, it MUST be lowered by one (never below zero) so it does not keep counting the order just handled. No screen may keep its own private copy of the count.

#### Scenario: Badge on the Solicitudes tab
- **WHEN** there are 2 pending orders
- **THEN** the Solicitudes bottom-navigation tab shows a count badge reading `2`

#### Scenario: No badge when nothing is pending
- **WHEN** there are no pending orders
- **THEN** the Solicitudes tab shows no badge and renders exactly as the other tabs do

#### Scenario: Every consumer shows the same number
- **WHEN** the pending count is 2
- **THEN** the Solicitudes tab badge, the Inicio bell badge and the `Pendientes (2)` filter label all read 2

#### Scenario: Confirming an order updates the badge
- **WHEN** the merchant confirms one of 2 pending orders
- **THEN** the pending count is refreshed and every badge reads `1`

#### Scenario: Rejecting an order updates the badge
- **WHEN** the merchant rejects one of 2 pending orders
- **THEN** the pending count is refreshed and every badge reads `1`

#### Scenario: Count cannot be re-read after confirming
- **WHEN** the merchant confirms one of 2 pending orders, the confirm answers `200`, and re-reading the count fails
- **THEN** every badge reads `1`

#### Scenario: A push refreshes the count
- **WHEN** a `NEW_ORDER` push arrives while the app process is alive
- **THEN** the pending count is re-read and every badge shows the new value, whichever tab is open

#### Scenario: Count is refreshed on entering the shell
- **WHEN** the merchant enters the authenticated shell
- **THEN** the pending count is refreshed regardless of which tab is selected

#### Scenario: Loading the pending list feeds the count
- **WHEN** the Solicitudes screen queries orders with `status=PENDING`
- **THEN** the shared pending count is updated from that query's `totalElements` without issuing an extra request

#### Scenario: Large counts are capped
- **WHEN** there are more than 99 pending orders
- **THEN** the badges read `99+`

### Requirement: Open the order detail as a dialog

Selecting **Ver detalle** on a card SHALL fetch `GET /api/v1/orders/{id}` and present the result as a dialog over the list, titled `Detalle de solicitud #<orderCode>`. The dialog MUST list every order item with its product name, unit price, quantity and line total, and MUST show the order total. An item whose `productStock` is `0` MUST display an out-of-stock warning reading `Sin existencias`, styled with the theme's error colours; it MUST NOT display the low-stock warning, because a stock-out is not "a few left". An item whose `productStock` is above `0` and at or below the low-stock threshold (5) MUST display the `Quedan pocos disponibles` warning. An item with no stock information (consult-mode products) MUST NOT display either warning. Dismissing the dialog SHALL only close it — with no snackbar and no list refresh.

The unit price shown is the one the order was charged (`unitPriceSnapshot`). When the item's `listPriceSnapshot` is higher, the list price SHALL also be shown, struck through, as `antes S/ <precio>`. An item whose product was deleted (`productActive: false`) SHALL show `Producto eliminado del inventario` in the error colours, instead of any stock warning. An item whose product was renamed after the order (`currentProductName` present) SHALL show `Ahora: <nombre>` below the name the order was placed with. When the detail fails to load, the error SHALL offer `Reintentar`, which requests it again.

#### Scenario: Detail is fetched when opened
- **WHEN** the merchant taps `Ver detalle` on an order
- **THEN** `GET /api/v1/orders/{id}` is requested and the dialog shows the returned items and total

#### Scenario: Low stock warning per item
- **WHEN** an item's product stock is between 1 and the low-stock threshold, inclusive
- **THEN** that item shows the `Quedan pocos disponibles` warning

#### Scenario: Sold-out item shows the out-of-stock warning
- **WHEN** an item's product stock is `0`
- **THEN** that item shows `Sin existencias` in the error colours and does not show `Quedan pocos disponibles`

#### Scenario: Stock above the threshold shows no warning
- **WHEN** an item's product stock is above the low-stock threshold
- **THEN** no stock warning is shown for that item

#### Scenario: Consult-mode item shows no stock warning
- **WHEN** an item's product stock is unknown (consult mode)
- **THEN** no stock warning is shown for that item

#### Scenario: Discounted item shows its list price
- **WHEN** an item was charged S/ 80.00 and its `listPriceSnapshot` is S/ 100.00
- **THEN** the item shows S/ 80.00 and, struck through, `antes S/ 100.00`

#### Scenario: Item at list price
- **WHEN** an item's `listPriceSnapshot` equals its unit price, or is absent
- **THEN** no list price is shown

#### Scenario: Deleted product
- **WHEN** an item has `productActive: false`
- **THEN** it shows `Producto eliminado del inventario` and no stock warning

#### Scenario: Renamed product
- **WHEN** an item was ordered as "Taladro" and has `currentProductName` "Taladro Pro"
- **THEN** it shows "Taladro" and, below it, `Ahora: Taladro Pro`

#### Scenario: Detail load failure
- **WHEN** the detail request fails
- **THEN** an error message with `Reintentar` is shown for that order, and the list underneath remains intact

#### Scenario: Retrying the detail
- **WHEN** the merchant taps `Reintentar` on a failed detail
- **THEN** `GET /api/v1/orders/{id}` is requested again and the dialog shows the result

#### Scenario: Dismissing the dialog changes nothing
- **WHEN** the merchant closes the dialog without confirming or rejecting
- **THEN** the dialog closes, no snackbar is shown and the list is not refreshed

### Requirement: Confirm an order

The system SHALL confirm an order via `POST /api/v1/orders/{id}/confirm`, from the detail dialog. On success it MUST close the dialog, remove the order from the Pendientes list at once, show a green bottom snackbar reading **`Solicitud confirmada`**, and re-query the order list quietly so the change is reflected. A list response that still shows that order as pending SHALL NOT bring it back. When the order already left `PENDING` elsewhere (`422 INVALID_STATE_TRANSITION`) or no longer exists (`404`), it SHALL also be removed at once.

`Confirmar pedido` SHALL be disabled while any item's product was deleted; rejecting stays available. When the confirm answers `409 PRODUCT_NOT_SELLABLE`, the dialog SHALL stay open and show, inside it, `No se puede confirmar: «<producto>» ya no está a la venta. Recházalo para avisar al cliente.` When the confirm fails for lack of connection, the dialog SHALL stay open and show the error inside it, in red above its buttons, not in a snackbar behind it.

#### Scenario: Successful confirmation
- **WHEN** the merchant taps `Confirmar pedido` and the request succeeds
- **THEN** the dialog closes, the order leaves Pendientes, a green snackbar reading `Solicitud confirmada` is shown, and the order list is re-queried

#### Scenario: Confirmed while the network drops
- **WHEN** the confirm answers `200` and the following list query fails
- **THEN** the order is not in Pendientes, no load-error snackbar is shown, and a later refresh does not bring it back

#### Scenario: Confirmation blocked by insufficient stock
- **WHEN** the confirm request fails with `422 STOCK_INSUFFICIENT`
- **THEN** a message naming the affected product is shown, the order is not treated as confirmed, and the list is refreshed

#### Scenario: Order already handled by someone else
- **WHEN** the confirm request fails with `422 INVALID_STATE_TRANSITION`
- **THEN** a message stating that the order was already handled is shown, the order leaves Pendientes and the list is refreshed

#### Scenario: Order with a deleted product
- **WHEN** the detail of an order shows an item with `productActive: false`
- **THEN** `Confirmar pedido` is disabled and `Rechazar` is enabled

#### Scenario: Product no longer sellable
- **WHEN** the confirm request fails with `409 PRODUCT_NOT_SELLABLE` for "Taladro"
- **THEN** the dialog stays open and shows `No se puede confirmar: «Taladro» ya no está a la venta. Recházalo para avisar al cliente.`

#### Scenario: Confirmation fails due to connectivity
- **WHEN** the confirm request fails with a network error
- **THEN** the dialog stays open, the error is shown in red inside it, no snackbar is shown, and the order remains pending

### Requirement: Reject an order

The system SHALL reject an order via `POST /api/v1/orders/{id}/reject` — reachable both from the card's `Rechazar` action and from the detail dialog — with the motive the merchant picks in the reject form. On success it MUST close the dialog if it is open, remove the order from the Pendientes list at once, show a red bottom snackbar reading **`Solicitud rechazada`**, and re-query the order list quietly. When the order already left `PENDING` (`422 INVALID_STATE_TRANSITION`) or no longer exists (`404`), it SHALL also be removed at once. When the reject fails for lack of connection, the form SHALL stay open and show the error inside it, in red above its buttons.

#### Scenario: Successful rejection from the dialog
- **WHEN** the merchant rejects from the detail dialog and the request succeeds
- **THEN** the dialog closes, the order leaves Pendientes, a red snackbar reading `Solicitud rechazada` is shown, and the order list is re-queried

#### Scenario: Successful rejection from the card
- **WHEN** the merchant rejects directly from an order card and the request succeeds
- **THEN** the order leaves Pendientes, a red snackbar reading `Solicitud rechazada` is shown and the order list is re-queried

#### Scenario: Rejecting an order that is no longer pending
- **WHEN** the reject request fails with `422 INVALID_STATE_TRANSITION`
- **THEN** a message stating that the order was already handled is shown, the order leaves Pendientes and the list is refreshed

#### Scenario: Rejection fails due to connectivity
- **WHEN** the reject request fails with a network error
- **THEN** the reject form stays open with the error shown in red inside it, and no snackbar is shown

### Requirement: Refresh the order list on demand

The screen SHALL provide a manual refresh action in its header and SHALL display the time of the last successful load as `Última actualización a las <hora>`. Refreshing MUST re-query with the active status filter and search query.

The list SHALL also re-query by itself, quietly, with the active filter and search query:
- when the screen starts again after its first start (back to the tab, back from the background, after unlocking);
- when orders changed elsewhere (a `NEW_ORDER` or `ORDER_TAKEN` push, or a notification tap while the app is running). A burst of such changes SHALL cause a single query.

A quiet re-query that fails SHALL keep the list on screen and show no snackbar.

#### Scenario: Manual refresh
- **WHEN** the merchant taps the refresh action
- **THEN** the order list is re-queried with the active filter and search query, and the last-update time is set to the time of the successful response

#### Scenario: Last update time is shown
- **WHEN** the order list has loaded successfully
- **THEN** the header shows `Última actualización a las` followed by the time of that load

#### Scenario: Failed refresh keeps the previous data
- **WHEN** a refresh fails
- **THEN** the previously loaded orders and the previous last-update time remain displayed alongside the error message

#### Scenario: New orders while the screen is open
- **WHEN** Solicitudes is open and three `NEW_ORDER` pushes arrive within a second
- **THEN** the list is re-queried once and shows the three new orders without tapping refresh

#### Scenario: Back to the tab
- **WHEN** the merchant leaves Solicitudes for another tab and comes back
- **THEN** the list is re-queried

#### Scenario: First start does not query twice
- **WHEN** Solicitudes opens for the first time
- **THEN** the list is queried once

#### Scenario: Quiet re-query fails
- **WHEN** a re-query triggered by a push or by coming back fails
- **THEN** the orders on screen stay and no error snackbar is shown
