## MODIFIED Requirements

### Requirement: Display a local notification for each order push

The system SHALL handle the backend's data-only, PII-free push payload (`{ event, orderId, storeId, status }`). The notification content MUST be derived solely from the `event` and `status` fields — no personally identifiable information and no order-detail fetch. Each order SHALL have at most one notification in the tray, keyed by its `orderId`, so a later event for the same order replaces or removes the earlier one instead of adding another. A `NEW_ORDER` event SHALL post a "new purchase order" notification. An `ORDER_EXPIRING` event SHALL post an "order about to expire" notification with its own title and body, replacing that order's existing notification; it SHALL NOT be treated as a change of status. An `ORDER_TAKEN` event SHALL NOT post a notification: it SHALL cancel that order's notification, because the order no longer needs attention. Any other event SHALL post the generic "order updates" notification. Tapping a notification SHALL open the app on the Solicitudes section.

#### Scenario: New order push posts a notification
- **WHEN** a push with `event = NEW_ORDER` is received
- **THEN** a local notification announcing a new purchase order is posted to the system tray

#### Scenario: Order-expiring push posts its own notification
- **WHEN** a push with `event = ORDER_EXPIRING` and `status = PENDING` is received
- **THEN** a local notification saying a pending order is about to expire is posted, with text distinct from the generic "order updated" text

#### Scenario: Order-expiring push replaces the order's new-order notification
- **WHEN** the tray holds the `NEW_ORDER` notification of an order and an `ORDER_EXPIRING` push for the same `orderId` is received
- **THEN** the tray holds exactly one notification for that order, the "about to expire" one

#### Scenario: Order-taken push cancels the order's notification
- **WHEN** the tray holds a notification for an order and a push with `event = ORDER_TAKEN` for the same `orderId` is received
- **THEN** that order's notification is removed from the tray and no new notification is posted

#### Scenario: Order-taken push for an order with no notification
- **WHEN** a push with `event = ORDER_TAKEN` is received for an order that has no notification in the tray
- **THEN** nothing is posted and no error occurs

#### Scenario: Notification contains no PII
- **WHEN** any order push is rendered as a notification
- **THEN** the notification text is generic and contains no customer name, product, or amount

#### Scenario: Tapping opens the Solicitudes section
- **WHEN** the merchant taps an order notification
- **THEN** the app opens on the Solicitudes section with the Pendientes tab selected

#### Scenario: Tapping while the app is already running
- **WHEN** the merchant taps an order notification while the app is in the background
- **THEN** the running instance is reused and switched to Solicitudes, without restarting the app

#### Scenario: Tapping while signed out
- **WHEN** the merchant taps an order notification with no authenticated session
- **THEN** the login screen is shown, and Solicitudes is opened once the merchant signs in

## ADDED Requirements

### Requirement: Reconcile order notifications with the server's pending orders

Pushes are best-effort, so the tray SHALL NOT be the source of truth for which orders need attention. Every time the app comes to the foreground with a signed-in, unlocked merchant, the system SHALL read the store's pending orders from the server (`GET /api/v1/orders?status=PENDING`) and SHALL cancel every order notification whose order is not among them. Notifications that cannot be matched to an order (no order id) SHALL be cancelled as well. The read SHALL also refresh the app-wide pending-order count. When the tray holds no order notifications, the system SHALL only refresh the pending-order count. If the server has more pending orders than one page returns, the system SHALL NOT cancel notifications it cannot rule out. If the read fails, the tray SHALL be left as it is. Because Android derives the launcher badge from the notifications in the tray, the launcher badge SHALL as a result follow the same server data as the Solicitudes tab badge.

#### Scenario: An order handled elsewhere while the push was lost
- **WHEN** the tray holds a notification for an order that the server no longer lists as pending, and the merchant opens the app
- **THEN** that notification is cancelled and the launcher badge no longer counts it

#### Scenario: Notifications of still-pending orders are kept
- **WHEN** the tray holds notifications for orders that the server still lists as pending, and the merchant opens the app
- **THEN** those notifications stay in the tray

#### Scenario: No pending orders left
- **WHEN** the server reports no pending orders and the tray holds order notifications
- **THEN** every order notification is cancelled

#### Scenario: Notification with no order id
- **WHEN** the tray holds an order notification that carries no order id, and the merchant opens the app
- **THEN** that notification is cancelled

#### Scenario: Empty tray
- **WHEN** the tray holds no order notifications and the merchant opens the app
- **THEN** only the pending-order count is refreshed

#### Scenario: More pending orders than one page
- **WHEN** the server's pending total is larger than the page that was read
- **THEN** notifications whose order is absent from that page are kept

#### Scenario: Server unreachable
- **WHEN** the pending-orders read fails (network or HTTP error)
- **THEN** no notification that carries an order id is cancelled

#### Scenario: Locked or signed out
- **WHEN** the app comes to the foreground while signed out or behind the fingerprint lock
- **THEN** no reconciliation runs until the merchant reaches the app shell
