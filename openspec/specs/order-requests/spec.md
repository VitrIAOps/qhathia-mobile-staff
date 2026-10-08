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

The number of pending orders SHALL be an application-wide value, held by the order data layer and observable by any screen. It MUST be displayed as a count badge on the **Solicitudes** bottom-navigation tab whenever it is greater than zero, and MUST be the same value shown on the Inicio bell and in the `Pendientes (N)` filter label. The value MUST be refreshed when the authenticated shell is entered, whenever the order list is queried for pending orders, and after an order is successfully confirmed or rejected. No screen may keep its own private copy of the count.

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

#### Scenario: Count is refreshed on entering the shell
- **WHEN** the merchant enters the authenticated shell
- **THEN** the pending count is refreshed regardless of which tab is selected

#### Scenario: Loading the pending list feeds the count
- **WHEN** the Solicitudes screen queries orders with `status=PENDING`
- **THEN** the shared pending count is updated from that query's `totalElements` without issuing an extra request

#### Scenario: Large counts are capped
- **WHEN** there are more than 99 pending orders
- **THEN** the badges read `99+`

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

Selecting **Ver detalle** on a card SHALL fetch `GET /api/v1/orders/{id}` and present the result as a dialog over the list, titled `Detalle de solicitud #<orderCode>`. The dialog MUST list every order item with its product name, unit price, quantity and line total, and MUST show the order total. An item whose `productStock` is low MUST display the `Quedan pocos disponibles` warning; an item with no stock information (consult-mode products) MUST NOT display it. Dismissing the dialog SHALL only close it — with no snackbar and no list refresh.

#### Scenario: Detail is fetched when opened
- **WHEN** the merchant taps `Ver detalle` on an order
- **THEN** `GET /api/v1/orders/{id}` is requested and the dialog shows the returned items and total

#### Scenario: Low stock warning per item
- **WHEN** an item's product stock is at or below the low-stock threshold
- **THEN** that item shows the `Quedan pocos disponibles` warning

#### Scenario: Consult-mode item shows no stock warning
- **WHEN** an item's product stock is unknown (consult mode)
- **THEN** no low-stock warning is shown for that item

#### Scenario: Detail load failure
- **WHEN** the detail request fails
- **THEN** an error message is shown for that order and the list underneath remains intact

#### Scenario: Dismissing the dialog changes nothing
- **WHEN** the merchant closes the dialog without confirming or rejecting
- **THEN** the dialog closes, no snackbar is shown and the list is not refreshed

### Requirement: Confirm an order

The system SHALL confirm an order via `POST /api/v1/orders/{id}/confirm`, from the detail dialog. On success it MUST close the dialog, show a green bottom snackbar reading **`Solicitud confirmada`**, and re-query the order list so the change is reflected.

#### Scenario: Successful confirmation
- **WHEN** the merchant taps `Confirmar pedido` and the request succeeds
- **THEN** the dialog closes, a green snackbar reading `Solicitud confirmada` is shown, and the order list is re-queried

#### Scenario: Confirmation blocked by insufficient stock
- **WHEN** the confirm request fails with `422 STOCK_INSUFFICIENT`
- **THEN** a message naming the affected product is shown, the order is not treated as confirmed, and the list is refreshed

#### Scenario: Order already handled by someone else
- **WHEN** the confirm request fails with `422 INVALID_STATE_TRANSITION`
- **THEN** a message stating that the order was already handled is shown and the list is refreshed

#### Scenario: Confirmation fails due to connectivity
- **WHEN** the confirm request fails with a network error
- **THEN** a retryable error message is shown and the order remains pending

### Requirement: Reject an order

The system SHALL reject an order via `POST /api/v1/orders/{id}/reject` — reachable both from the card's `Rechazar` action and from the detail dialog — without asking the merchant for a reason. On success it MUST close the dialog if it is open, show a red bottom snackbar reading **`Solicitud rechazada`**, and re-query the order list.

#### Scenario: Successful rejection from the dialog
- **WHEN** the merchant taps `Rechazar` in the detail dialog and the request succeeds
- **THEN** the dialog closes, a red snackbar reading `Solicitud rechazada` is shown, and the order list is re-queried

#### Scenario: Successful rejection from the card
- **WHEN** the merchant taps `Rechazar` directly on an order card and the request succeeds
- **THEN** a red snackbar reading `Solicitud rechazada` is shown and the order list is re-queried

#### Scenario: Rejection sends no reason
- **WHEN** an order is rejected
- **THEN** the request is sent without a rejection reason

#### Scenario: Rejecting an order that is no longer pending
- **WHEN** the reject request fails with `422 INVALID_STATE_TRANSITION`
- **THEN** a message stating that the order was already handled is shown and the list is refreshed

### Requirement: Refresh the order list on demand

The screen SHALL provide a manual refresh action in its header and SHALL display the time of the last successful load as `Última actualización a las <hora>`. Refreshing MUST re-query with the active status filter and search query.

#### Scenario: Manual refresh
- **WHEN** the merchant taps the refresh action
- **THEN** the order list is re-queried with the active filter and search query, and the last-update time is set to the time of the successful response

#### Scenario: Last update time is shown
- **WHEN** the order list has loaded successfully
- **THEN** the header shows `Última actualización a las` followed by the time of that load

#### Scenario: Failed refresh keeps the previous data
- **WHEN** a refresh fails
- **THEN** the previously loaded orders and the previous last-update time remain displayed alongside the error message

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

