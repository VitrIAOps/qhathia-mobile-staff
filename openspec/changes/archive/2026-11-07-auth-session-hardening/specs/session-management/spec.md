## MODIFIED Requirements

### Requirement: Transparent access-token refresh

When the access token is expired or rejected with `401`, the system SHALL attempt to obtain a new access token via `POST /api/v1/auth/refresh` using the stored refresh token, persisting the rotated tokens. If the server rejects the refresh, the session SHALL be cleared.

A `401` whose ProblemDetail `errorCode` is `CURRENT_PASSWORD_INCORRECT` is not a rejected token and SHALL NOT trigger a refresh or a retry. A refresh that fails for lack of connectivity SHALL keep the stored session: the request fails, and the next one tries again.

#### Scenario: Access token refreshed successfully
- **WHEN** an authenticated request needs a token and the current access token is expired
- **THEN** the system refreshes the token, persists the new access and refresh tokens, and proceeds

#### Scenario: Refresh token invalid or expired
- **WHEN** the refresh call returns `401`
- **THEN** the stored session is cleared and the authentication state becomes unauthenticated

#### Scenario: Wrong current password
- **WHEN** `POST /api/v1/auth/change-password` answers `401` with `errorCode` `CURRENT_PASSWORD_INCORRECT`
- **THEN** no refresh is attempted and the change-password request is sent only once

#### Scenario: Refresh without connectivity
- **WHEN** an authenticated request needs a refresh and the refresh call fails for lack of connectivity
- **THEN** the stored session is kept and the merchant stays signed in
