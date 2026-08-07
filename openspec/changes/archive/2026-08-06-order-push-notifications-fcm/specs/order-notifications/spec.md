## ADDED Requirements

### Requirement: Register the device for order push notifications

While a merchant is authenticated, the system SHALL register the device's FCM token with the backend via `POST /api/v1/notifications/tokens` (body `{ token, platform: "ANDROID", deviceLabel? }`, JWT-scoped) so the store's devices receive order pushes. Registration MUST run after a successful login, on app start when a session already exists, and whenever Firebase rotates the token. Registration failures MUST be fail-open — they SHALL NOT block login, app start, or any user action.

#### Scenario: Token registered after login
- **WHEN** a merchant logs in successfully
- **THEN** the current FCM token is sent to `POST /api/v1/notifications/tokens` with the authenticated session

#### Scenario: Token registered on authenticated app start
- **WHEN** the app starts and a valid session is already present
- **THEN** the current FCM token is registered with the backend

#### Scenario: Token re-registered on rotation
- **WHEN** Firebase issues a new token via `onNewToken` and a session exists
- **THEN** the new token is registered with the backend

#### Scenario: Registration failure does not block the user
- **WHEN** the token registration request fails (network error, non-2xx, or no permission)
- **THEN** the failure is swallowed and login / app start proceed normally

#### Scenario: No registration without a session
- **WHEN** there is no authenticated session
- **THEN** no token registration request is made

### Requirement: Display a local notification for each order push

The system SHALL handle the backend's data-only, PII-free push payload (`{ event, orderId, storeId, status }`) and post a local notification for each received message. The notification content MUST be derived solely from the `event` and `status` fields — no personally identifiable information and no order-detail fetch. A `NEW_ORDER` event SHALL produce a "new purchase order" notification; an `ORDER_TAKEN` event SHALL produce an "order updated" notification reflecting `CONFIRMED` or `REJECTED`. Tapping the notification SHALL open the app.

#### Scenario: New order push posts a notification
- **WHEN** a push with `event = NEW_ORDER` is received
- **THEN** a local notification announcing a new purchase order is posted to the system tray

#### Scenario: Order-taken push posts a notification
- **WHEN** a push with `event = ORDER_TAKEN` and `status = CONFIRMED` or `REJECTED` is received
- **THEN** a local notification indicating the order was updated is posted

#### Scenario: Notification contains no PII
- **WHEN** any order push is rendered as a notification
- **THEN** the notification text is generic and contains no customer name, product, or amount

#### Scenario: Tapping opens the app
- **WHEN** the merchant taps an order notification
- **THEN** the app's main screen is opened

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
