## ADDED Requirements

### Requirement: Login screen presentation

The system SHALL present a login screen optimized for phones (not tablets) that displays a large "Qhathia" wordmark as the brand reference, an email input, a password input, and a primary login button. Inputs SHALL use placeholders (no labels) with a representative leading icon each, following the Qhathia Design System input component (rounded 8dp, 40dp height, Urbanist typography).

#### Scenario: Screen shown on launch when unauthenticated
- **WHEN** the app starts and no valid session exists
- **THEN** the login screen is displayed with empty email and password inputs and the login button enabled

#### Scenario: Password is visually protected
- **WHEN** the user types into the password input
- **THEN** the characters are masked and a toggle allows showing/hiding the password

### Requirement: Client-side input validation

The system SHALL validate inputs before any network call. The email MUST match a valid email format and the password MUST be non-empty. Invalid inputs SHALL surface an inline error message on the affected field and SHALL NOT trigger a network request.

#### Scenario: Invalid email format
- **WHEN** the user submits with an email that is not a valid format
- **THEN** the email field shows an error state with a message and no login request is sent

#### Scenario: Empty password
- **WHEN** the user submits with an empty password
- **THEN** the password field shows an error state with a message and no login request is sent

### Requirement: Authenticate against the Auth API

The system SHALL authenticate the merchant by calling `POST /api/v1/auth/login` with the email and password. On `200`, it SHALL hand the returned session (access token, refresh token, expiry, user, active store) to the session layer and navigate to Home.

#### Scenario: Successful login
- **WHEN** valid credentials are submitted and the API returns `200`
- **THEN** the session is persisted and the app navigates to the Home destination

#### Scenario: Invalid credentials
- **WHEN** the API returns `401`
- **THEN** the screen shows an "incorrect email or password" message and remains on the login screen

#### Scenario: Validation rejected by server
- **WHEN** the API returns `400`
- **THEN** the screen shows a validation error message and remains on the login screen

### Requirement: Loading and disabled state during sign-in

While a login request is in flight, the system SHALL replace the login button with a circular progress indicator and SHALL disable the email and password inputs to prevent concurrent submissions.

#### Scenario: Request in progress
- **WHEN** a login request has been sent and no response has returned yet
- **THEN** the login button is replaced by a circular progress indicator and both inputs are disabled

#### Scenario: Request completes with error
- **WHEN** a login request fails (HTTP error, network error, or timeout)
- **THEN** the progress indicator is replaced by the login button, inputs are re-enabled, and an error message is shown

### Requirement: Network and timeout error handling

The system SHALL apply request timeouts appropriate for a mobile network and SHALL map network failures and timeouts to a user-facing, non-technical message without crashing or leaking exceptions to the UI.

#### Scenario: Network unavailable or timeout
- **WHEN** the login request fails due to no connectivity or a timeout
- **THEN** the screen shows a connectivity error message and the user can retry
