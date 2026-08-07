## ADDED Requirements

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
