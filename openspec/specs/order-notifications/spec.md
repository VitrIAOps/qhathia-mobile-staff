# order-notifications Specification

## Purpose

Defines how the Qhathia staff app receives and surfaces order push notifications: registering the device's FCM token with the backend while a merchant is authenticated (after login, on authenticated app start, and on token rotation), handling the backend's data-only, PII-free payload to post a local tray notification for new and taken orders, requesting the `POST_NOTIFICATIONS` runtime permission on Android 13+, and unregistering the token on logout so the device stops receiving pushes.
## Requirements
### Requirement: Register the device for order push notifications

While a merchant is authenticated, the system SHALL register the device's FCM token with the backend via `POST /api/v1/notifications/tokens` (body `{ token, platform: "ANDROID", deviceLabel? }`, JWT-scoped) so the store's devices receive order pushes. Registration MUST run after a successful login, on app start when a session already exists, and whenever Firebase rotates the token. Registration MUST be skipped when there is no session, so a rotation while signed out sends nothing. Registration failures MUST be fail-open — they SHALL NOT block login, app start, or any user action.

When a registration fails while signed in, the system SHALL try again when the network comes back (or after a short wait), up to 10 attempts, and SHALL stop trying when the session ends.

#### Scenario: Token registered after login
- **WHEN** a merchant logs in successfully
- **THEN** the current FCM token is sent to `POST /api/v1/notifications/tokens` with the authenticated session

#### Scenario: Token registered on authenticated app start
- **WHEN** the app starts and a valid session is already present
- **THEN** the current FCM token is registered with the backend

#### Scenario: Token re-registered on rotation
- **WHEN** Firebase issues a new token via `onNewToken` and a session exists
- **THEN** the new token is registered with the backend

#### Scenario: Rotation while signed out
- **WHEN** Firebase issues a new token and there is no session (for example after clearing the app's data)
- **THEN** no registration request is sent

#### Scenario: Registration retried when the network returns
- **WHEN** the app starts signed in without network and the network comes back
- **THEN** the token is registered within 30 seconds, without any action from the merchant

#### Scenario: Registration failure does not block the user
- **WHEN** the token registration request fails (network error, non-2xx, or no permission)
- **THEN** the failure is swallowed and login / app start proceed normally

#### Scenario: No registration without a session
- **WHEN** there is no authenticated session
- **THEN** no token registration request is made

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

### Requirement: Request notification permission on supported devices

On Android 13 (API 33) and above, the system SHALL request the `POST_NOTIFICATIONS` runtime permission when the merchant first reaches the authenticated app shell. If the permission is denied, token registration and push handling SHALL still proceed; only the visible tray notification is suppressed by the OS.

#### Scenario: Permission requested on API 33+
- **WHEN** an authenticated merchant reaches the app shell on Android 13+ and notification permission has not been decided
- **THEN** the system prompts for the `POST_NOTIFICATIONS` permission

#### Scenario: Denied permission does not break registration
- **WHEN** the merchant denies the notification permission
- **THEN** the FCM token is still registered and no error is surfaced to the user

### Requirement: Stop order notifications on logout

When a merchant logs out, the system SHALL unregister the device's FCM token via `DELETE /api/v1/notifications/tokens` before the session is cleared, and SHALL delete the local Firebase token so the device stops receiving order pushes. Unregistration MUST be fail-open — a failed request SHALL NOT prevent logout from completing.

#### Scenario: Token unregistered before session cleared
- **WHEN** the merchant logs out
- **THEN** `DELETE /api/v1/notifications/tokens` is called with the still-valid session, and only then is the session cleared

#### Scenario: Local Firebase token deleted on logout
- **WHEN** the merchant logs out
- **THEN** the local Firebase token is deleted so the device no longer receives pushes

#### Scenario: Logout succeeds despite unregister failure
- **WHEN** the unregister request fails (e.g. offline)
- **THEN** the session is still cleared and the merchant is returned to the login screen

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

