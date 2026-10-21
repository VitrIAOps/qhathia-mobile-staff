## MODIFIED Requirements

### Requirement: Authenticate against the Auth API

The system SHALL authenticate the merchant by calling `POST /api/v1/auth/login` with the email, the password, and a `deviceLabel` naming the phone. The label is built from the device manufacturer and model (for example "Xiaomi Redmi Note 12") and truncated to 100 characters. On `200`, the system SHALL hand the returned session (access token, refresh token, expiry, user, active store and `sessionId`) to the session layer and navigate to Home.

#### Scenario: Successful login
- **WHEN** valid credentials are submitted and the API returns `200`
- **THEN** the session, including `sessionId`, is persisted and the app navigates to the Home destination

#### Scenario: Device label sent
- **WHEN** the merchant logs in from a Redmi Note 12
- **THEN** the login request body includes `deviceLabel` naming that model

#### Scenario: Response without session id
- **WHEN** the API returns `200` without `sessionId`
- **THEN** login still succeeds and the session is persisted without a session id

#### Scenario: Invalid credentials
- **WHEN** the API returns `401`
- **THEN** the screen shows an "incorrect email or password" message and remains on the login screen

#### Scenario: Validation rejected by server
- **WHEN** the API returns `400`
- **THEN** the screen shows a validation error message and remains on the login screen
