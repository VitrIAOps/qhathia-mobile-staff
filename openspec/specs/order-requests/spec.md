# order-requests Specification

## Purpose

Defines how a merchant works purchase orders in the Solicitudes tab of the Qhathia staff app: browsing orders by status (Pendientes, Aceptados, Rechazados) against `GET /api/v1/orders`, searching them by customer name or order code through the backend `q` parameter, surfacing the app-wide pending-order count on the Solicitudes tab, the Inicio bell and the `Pendientes (N)` filter, reading each order's identity, total and live expiry countdown derived from `expiresAt`, opening the order detail as a dialog with per-item low-stock warnings, confirming or rejecting an order with its snackbar feedback and conflict handling (`STOCK_INSUFFICIENT`, `INVALID_STATE_TRANSITION`), and refreshing the list on demand with a visible last-update time.
## Requirements
### Requirement: Browse purchase orders by status

The system SHALL present purchase orders in the Solicitudes tab, grouped by three status filters backed by `GET /api/v1/orders`: **Pendientes** (`status=PENDING`), **Aceptados** (`status=CONFIRMED`) and **Rechazados** (`status=REJECTED,EXPIRED`). **Pendientes** MUST be selected when the screen opens. Only the Pendientes filter SHALL display a count; that count MUST be read from the application-wide pending-order count rather than from state private to this screen. Selecting a filter MUST re-query the backend rather than filter an already-loaded list.

#### Scenario: Pendientes is the default filter
- **WHEN** the merchant opens the Solicitudes tab
- **THEN** orders are requested with `status=PENDING` and the Pendientes filter is shown as selected

#### Scenario: Only Pendientes carries a count
- **WHEN** the order list is loaded and there are 2 pending orders
- **THEN** the Pendientes filter reads `Pendientes (2)` and the Aceptados and Rechazados filters show no count

#### Scenario: Pendientes count matches the tab badge
- **WHEN** the merchant opens the Solicitudes tab while the Solicitudes bottom-navigation badge reads 2
- **THEN** the Pendientes filter reads `Pendientes (2)`

#### Scenario: Rechazados includes expired orders
- **WHEN** the merchant selects the Rechazados filter
- **THEN** orders are requested with `status=REJECTED,EXPIRED` so manually rejected and TTL-expired orders appear together

#### Scenario: Changing filter re-queries the backend
- **WHEN** the merchant selects a different filter
- **THEN** a new `GET /api/v1/orders` request is issued with the corresponding `status` value

#### Scenario: Empty list per filter
- **WHEN** the active filter returns no orders
- **THEN** a centered message is shown, worded for that filter — `Sin solicitudes pendientes`, `Sin solicitudes aceptadas` or `Sin solicitudes rechazadas`

#### Scenario: Loading failure is recoverable
- **WHEN** the order list request fails
- **THEN** an error message with a retry action is shown instead of an empty list, and retrying re-issues the request with the active filter and search query

### Requirement: Surface the pending-order count across the app

The number of pending orders SHALL be an application-wide value, held by the order data layer and observable by any screen. It MUST be displayed as a count badge on the **Solicitudes** bottom-navigation tab whenever it is greater than zero, and MUST be the same value shown on the Inicio bell and in the `Pendientes (N)` filter label. The value MUST be refreshed when the authenticated shell is entered, whenever the order list is queried for pending orders, after an order is successfully confirmed or rejected, and when a `NEW_ORDER` or `ORDER_TAKEN` push arrives. When the count cannot be re-read after a successful confirm or reject, it MUST be lowered by one (never below zero) so it does not keep counting the order just handled. No screen may keep its own private copy of the count. Refreshes requested while one is already in flight SHALL share its answer, so Inicio and the shell entering together send one request; a refresh caused by a push, a confirm or a reject SHALL always use a request started after it, so the order that caused it is counted.

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

#### Scenario: Opening Inicio sends one count request
- **WHEN** the merchant enters the shell on Inicio
- **THEN** one `GET /api/v1/orders?status=PENDING&size=1` is sent, not two

#### Scenario: A push during a count request
- **WHEN** a count request is in flight and a `NEW_ORDER` push arrives
- **THEN** another count request is sent after it, and every badge includes the new order

### Requirement: Search orders by customer name or order code

The system SHALL let the merchant search orders from a search box, sending the text as the `q` query parameter of `GET /api/v1/orders` so that customer-name matches and order-code prefix matches are resolved by the backend across the whole result set. Input MUST be debounced, and results MUST always correspond to the latest query typed. The search MUST be combined with the active status filter.

#### Scenario: Search is delegated to the backend
- **WHEN** the merchant types `mari` in the search box
- **THEN** orders are requested with `q=mari` and the active `status` filter

#### Scenario: Typing does not issue one request per keystroke
- **WHEN** the merchant types several characters in quick succession
- **THEN** the requests are debounced and only the latest query produces the displayed result

#### Scenario: Clearing the search restores the filtered list
- **WHEN** the merchant clears the search box
- **THEN** orders are requested for the active status filter with no `q` parameter

#### Scenario: No matches
- **WHEN** a search returns no orders
- **THEN** the centered empty message for the active filter is shown

### Requirement: Show each order's remaining time before expiry

Each order card SHALL display, in its top-right corner, the time remaining until `expiresAt`, counting down while the screen is visible. When the remaining time is **3 minutes or less**, the countdown MUST switch to its emphasized red treatment. When the remaining time reaches zero, the order's actions MUST be disabled. The countdown is a display of the server-provided `expiresAt` — the app SHALL NOT compute expiry from a hardcoded TTL, and SHALL NOT remove an order from the list on its own.

#### Scenario: Countdown rendered from expiresAt
- **WHEN** a pending order expires in 20 minutes
- **THEN** its card shows `Expira en 20 minutos` in the top-right corner

#### Scenario: Countdown turns red near expiry
- **WHEN** an order has 3 minutes or less remaining
- **THEN** the countdown is rendered with the red emphasized treatment

#### Scenario: Countdown ticks down over time
- **WHEN** the screen stays open while time passes
- **THEN** the remaining time on each card decreases without a new request

#### Scenario: Actions disabled once the countdown reaches zero
- **WHEN** an order's remaining time reaches zero
- **THEN** its confirm and reject actions are disabled and the order stays visible until the next refresh

### Requirement: Show order identity and totals in each card

Each order card SHALL display the customer name, the order code as `Código: <orderCode>`, and the order total. When the customer name is absent, the card title MUST fall back to `#<orderCode>`. When `orderCode` is absent (orders created before the backend added it), the card MUST still render a usable identifier instead of an empty or `null` label.

#### Scenario: Card shows customer, code and total
- **WHEN** an order has a customer name, an order code and a total
- **THEN** the card shows the customer name as the title, `Código:` with the order code, and the formatted total

#### Scenario: Missing customer name falls back to the code
- **WHEN** an order has no customer name
- **THEN** the card title is rendered as `#<orderCode>`

#### Scenario: Missing order code does not render null
- **WHEN** an order has no `orderCode`
- **THEN** a fallback identifier is shown and no `null` text appears in the card

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

### Requirement: Search clear action

The order search box SHALL show a trailing clear (`x`) button whenever it contains text. Tapping it SHALL remove all the entered text, which restores the list for the active status filter. The button SHALL NOT be shown while the search box is empty.

#### Scenario: Clear button appears with text

- **WHEN** the merchant has typed text in the search box
- **THEN** a clear button is shown at the end of the search box

#### Scenario: Clearing the text

- **WHEN** the merchant taps the clear button
- **THEN** the search box becomes empty, the clear button disappears and the orders are re-queried with the active status filter and no search text

### Requirement: Show each item's product photo in the order detail

Each item row of the order detail dialog SHALL display the product photo given by `items[].productImageUrl` of `GET /api/v1/orders/{id}` in its thumbnail slot. When `productImageUrl` is `null` or blank, or when the image cannot be loaded, the row MUST display the neutral placeholder instead of an empty slot. The photo MUST come from the detail response alone — the app SHALL NOT issue any per-item product request to obtain it. The photo is the product's current primary image, not a snapshot taken when the order was created, so the app SHALL NOT cache or persist the URL beyond the detail it arrived with.

#### Scenario: Item with a product photo
- **WHEN** the detail response carries an item whose `productImageUrl` is a valid image URL
- **THEN** that item's thumbnail shows the product photo, cropped to fill the slot

#### Scenario: Item whose product has no images
- **WHEN** the detail response carries an item whose `productImageUrl` is `null`
- **THEN** that item's thumbnail shows the neutral placeholder

#### Scenario: Blank image URL
- **WHEN** the detail response carries an item whose `productImageUrl` is an empty or blank string
- **THEN** that item's thumbnail shows the neutral placeholder and no image request is attempted

#### Scenario: Image fails to load
- **WHEN** an item has a `productImageUrl` but the image cannot be downloaded or decoded
- **THEN** that item's thumbnail shows the neutral placeholder, and the rest of the dialog — items, total and actions — remains usable

#### Scenario: Photo is not required to act on the order
- **WHEN** an item's photo is still loading
- **THEN** the item's name, price, quantity and line total are already shown and the confirm and reject actions are available

#### Scenario: No per-item request for the photo
- **WHEN** the merchant opens the detail of an order with several items
- **THEN** only `GET /api/v1/orders/{id}` is issued against the backend API, and no product endpoint is called per item

#### Scenario: Response without the field
- **WHEN** the detail response omits `productImageUrl` for its items
- **THEN** the dialog renders normally with the neutral placeholder on every item

#### Scenario: Reopening shows the current photo
- **WHEN** the merchant changes a product's primary photo and then reopens the detail of an order containing that product
- **THEN** the item shows the photo returned by the new detail response

### Requirement: Solicitudes header has no back control

Solicitudes is a root bottom-navigation destination, so its header SHALL NOT show a back chevron or any control that suggests going back. The header SHALL NOT expose a back action to accessibility services. The title SHALL start at the header's horizontal padding.

#### Scenario: No back chevron in the header
- **WHEN** the Solicitudes tab is shown
- **THEN** the header shows the title, the last-update time and the refresh action, and no back chevron

#### Scenario: TalkBack announces no back action
- **WHEN** a screen-reader user explores the Solicitudes header
- **THEN** no element is announced as `Volver`

### Requirement: Actions only on actionable orders in the detail dialog

The order detail dialog SHALL show the `Rechazar` and `Confirmar pedido` actions only when the order is actionable: its status is `PENDING` and it has not reached its `expiresAt`. While a confirm or reject request for that order is in flight, the actions SHALL remain visible but disabled, with progress shown on the confirm button. For any order that is not actionable the dialog SHALL NOT show either action, and SHALL instead show one line stating the outcome:

- `CONFIRMED` → "Esta solicitud ya fue confirmada"
- `REJECTED` → "Esta solicitud ya fue rechazada"
- `CANCELLED` → "Esta solicitud fue cancelada"
- `EXPIRED`, or `PENDING` past its `expiresAt` → "Esta solicitud venció"

The `Confirmar pedido` label SHALL never be clipped: if it does not fit on one line, the button SHALL grow to show it in full.

#### Scenario: Pending order
- **WHEN** the merchant opens the detail of a pending order that has not expired
- **THEN** `Rechazar` and `Confirmar pedido` are shown and enabled

#### Scenario: Confirmed order
- **WHEN** the merchant opens the detail of a confirmed order
- **THEN** no action buttons are shown and the dialog reads "Esta solicitud ya fue confirmada"

#### Scenario: Rejected order
- **WHEN** the merchant opens the detail of a rejected order
- **THEN** no action buttons are shown and the dialog reads "Esta solicitud ya fue rechazada"

#### Scenario: Pending order past its expiry
- **WHEN** the merchant opens the detail of an order still marked `PENDING` whose `expiresAt` has passed
- **THEN** no action buttons are shown and the dialog reads "Esta solicitud venció"

#### Scenario: Request in flight
- **WHEN** the merchant has tapped `Confirmar pedido` and the request has not answered yet
- **THEN** both actions are visible and disabled, and the confirm button shows progress

#### Scenario: Long confirm label
- **WHEN** the confirm label does not fit on one line (narrow screen or large font)
- **THEN** the button grows and the whole label is visible

