# session-management Specification

## Purpose

TBD: Defines how the authenticated session is persisted, exposed to the app, refreshed, and attached to authenticated requests after a merchant logs in.

## Requirements

### Requirement: Persist the authenticated session

The system SHALL persist the authenticated session — access token, refresh token, token expiry, user identity (id, email, full name, role), and active store (id, name, category) — so it survives process death and app restarts. Tokens MUST be stored encrypted at rest.

#### Scenario: Session persisted after login
- **WHEN** a login succeeds
- **THEN** the access token, refresh token, expiry, user, and active store are written to encrypted storage

#### Scenario: Session restored on restart
- **WHEN** the app is restarted and a non-expired session exists in storage
- **THEN** the session is restored and the user is not prompted to log in again

### Requirement: Expose session state to the app

The system SHALL expose the current session and an `isAuthenticated` signal as an observable stream so that navigation and feature modules can react to authentication state without reading storage directly.

#### Scenario: Authenticated state observed
- **WHEN** a valid session is present
- **THEN** the exposed authentication state reports authenticated and provides the active store and user

#### Scenario: Unauthenticated state observed
- **WHEN** no session is present
- **THEN** the exposed authentication state reports unauthenticated

### Requirement: Transparent access-token refresh

When the access token is expired or rejected with `401`, the system SHALL attempt to obtain a new access token via `POST /api/v1/auth/refresh` using the stored refresh token, persisting the rotated tokens. If refresh fails, the session SHALL be cleared.

#### Scenario: Access token refreshed successfully
- **WHEN** an authenticated request needs a token and the current access token is expired
- **THEN** the system refreshes the token, persists the new access and refresh tokens, and proceeds

#### Scenario: Refresh token invalid or expired
- **WHEN** the refresh call returns `401`
- **THEN** the stored session is cleared and the authentication state becomes unauthenticated

### Requirement: Attach bearer token to authenticated requests

The system SHALL attach the current access token as an `Authorization: Bearer <token>` header to outgoing requests that require authentication, sourced from the session layer rather than passed manually per call.

#### Scenario: Authorized request carries the token
- **WHEN** an authenticated API call is made while a valid session exists
- **THEN** the request includes the `Authorization: Bearer <accessToken>` header

### Requirement: Explicit sign out

The system SHALL provide a user-triggered sign-out action on the Profile screen. Signing out SHALL, while the session is still valid: (1) unregister the device's push token (see `order-notifications`), then (2) invalidate the session server-side via `POST /api/v1/auth/logout` with the stored refresh token. It SHALL then clear the stored session so that the observable authentication state becomes unauthenticated and the app returns to the login screen. Both network steps are best-effort — sign out SHALL complete even if either fails.

#### Scenario: Merchant signs out from Profile
- **WHEN** the merchant taps the Logout action on the Profile screen and confirms
- **THEN** the push token is unregistered, the session is invalidated server-side via `POST /api/v1/auth/logout`, the stored session is cleared, and the app navigates to the login screen

#### Scenario: Refresh token invalidated server-side
- **WHEN** sign out calls `POST /api/v1/auth/logout` with the stored refresh token
- **THEN** the refresh token can no longer be used to obtain new access tokens

#### Scenario: Sign out returns to unauthenticated state
- **WHEN** the session is cleared by sign out
- **THEN** the exposed authentication state reports unauthenticated

#### Scenario: Sign out resilient to network failure
- **WHEN** push-token unregistration or the server-side logout call fails during sign out
- **THEN** the session is still cleared and the merchant is signed out
