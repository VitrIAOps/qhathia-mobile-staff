## MODIFIED Requirements

### Requirement: Display a local notification for each order push

The system SHALL handle the backend's data-only, PII-free push payload (`{ event, orderId, storeId, status }`). The notification content MUST be derived solely from the `event` and `status` fields — no personally identifiable information and no order-detail fetch. Each order SHALL have at most one notification in the tray, keyed by its `orderId`, so a later event for the same order replaces or removes the earlier one instead of adding another. A `NEW_ORDER` event SHALL post a "new purchase order" notification. An `ORDER_EXPIRING` event SHALL post an "order about to expire" notification with its own title and body, replacing that order's existing notification; it SHALL NOT be treated as a change of status. An `ORDER_TAKEN` event SHALL NOT post a notification: it SHALL cancel that order's notification, because the order no longer needs attention. Any other event SHALL post the generic "order updates" notification. Tapping a notification SHALL open the app on the Solicitudes section.

All order notifications SHALL belong to one group with an app-owned summary that reads "N órdenes por atender" ("N orders to handle"), N being the number of order notifications in the tray. Tapping the summary SHALL open the app on the Solicitudes section, as tapping a single notification does. The summary SHALL follow the tray: it is updated when an order notification is posted or removed, it SHALL stay while at least one order notification remains, and it SHALL be removed with the last one. Updating the summary SHALL NOT sound or vibrate; only the order notification does. The summary SHALL NOT count as an order notification anywhere else, such as the reconcile with the server's pending orders.

Besides the tray, a `NEW_ORDER` or `ORDER_TAKEN` push SHALL tell the app that orders changed: the pending count is re-read and an open Solicitudes list re-queries. This SHALL be fail-open and time-capped, so a push that cannot refresh anything never breaks the notification. Tapping a notification while the app is running SHALL do the same, also when Solicitudes is already the open tab.

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

#### Scenario: A push refreshes the app's orders
- **WHEN** a `NEW_ORDER` or `ORDER_TAKEN` push is received while the app process is alive
- **THEN** the pending count is re-read and an open Solicitudes list re-queries

#### Scenario: An expiring push does not refresh the list
- **WHEN** an `ORDER_EXPIRING` push is received
- **THEN** the list is not re-queried

#### Scenario: A push without network
- **WHEN** a `NEW_ORDER` push is received and the count cannot be read
- **THEN** the notification is still posted and nothing fails

#### Scenario: Two orders are grouped under our summary
- **WHEN** two `NEW_ORDER` pushes for different orders are received
- **THEN** the tray shows one group with the summary "2 órdenes por atender" over the two order notifications

#### Scenario: Tapping the group opens Solicitudes
- **WHEN** the merchant taps the group summary while the app is in the background on another section
- **THEN** the app opens on the Solicitudes section with the Pendientes tab selected, and the list shows the new orders

#### Scenario: Tapping the group while Solicitudes is open
- **WHEN** the merchant taps the group summary while Solicitudes is already the open tab
- **THEN** the list re-queries and shows the new orders

#### Scenario: The summary follows the tray
- **WHEN** the tray holds three order notifications and an `ORDER_TAKEN` push removes one of them
- **THEN** the summary reads "2 órdenes por atender" and the other two notifications stay

#### Scenario: The last order leaves
- **WHEN** the tray holds one order notification under the summary and it is removed
- **THEN** the summary is removed too

#### Scenario: The reconcile ignores the summary
- **WHEN** the app reconciles the tray with the server's pending orders while the summary is shown
- **THEN** the summary is not treated as an order, and only order notifications are cancelled

#### Scenario: Notification contains no PII
- **WHEN** any order push is rendered as a notification
- **THEN** the notification text is generic and contains no customer name, product, or amount

#### Scenario: Tapping opens the Solicitudes section
- **WHEN** the merchant taps an order notification
- **THEN** the app opens on the Solicitudes section with the Pendientes tab selected

#### Scenario: Tapping while the app is already running
- **WHEN** the merchant taps an order notification while the app is in the background
- **THEN** the running instance is reused and switched to Solicitudes, without restarting the app

#### Scenario: Tapping while Solicitudes is open
- **WHEN** the merchant taps an order notification while Solicitudes is already the open tab
- **THEN** the list re-queries and shows the new order

#### Scenario: Tapping while signed out
- **WHEN** the merchant taps an order notification with no authenticated session
- **THEN** the login screen is shown, and Solicitudes is opened once the merchant signs in
