## MODIFIED Requirements

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
