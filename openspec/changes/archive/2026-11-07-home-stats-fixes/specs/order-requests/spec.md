## MODIFIED Requirements

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
