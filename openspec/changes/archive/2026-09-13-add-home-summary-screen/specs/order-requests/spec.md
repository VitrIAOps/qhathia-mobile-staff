## ADDED Requirements

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

## MODIFIED Requirements

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
