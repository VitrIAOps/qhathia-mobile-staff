## ADDED Requirements

### Requirement: Browse purchase orders by status

The system SHALL present purchase orders in the Solicitudes tab, grouped by three status filters backed by `GET /api/v1/orders`: **Pendientes** (`status=PENDING`), **Aceptados** (`status=CONFIRMED`) and **Rechazados** (`status=REJECTED,EXPIRED`). **Pendientes** MUST be selected when the screen opens. Only the Pendientes filter SHALL display a count, taken from the `totalElements` of the pending query. Selecting a filter MUST re-query the backend rather than filter an already-loaded list.

#### Scenario: Pendientes is the default filter
- **WHEN** the merchant opens the Solicitudes tab
- **THEN** orders are requested with `status=PENDING` and the Pendientes filter is shown as selected

#### Scenario: Only Pendientes carries a count
- **WHEN** the order list is loaded and there are 2 pending orders
- **THEN** the Pendientes filter reads `Pendientes (2)` and the Aceptados and Rechazados filters show no count

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
