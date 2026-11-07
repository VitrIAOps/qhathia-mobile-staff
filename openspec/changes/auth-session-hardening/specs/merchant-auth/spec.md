## MODIFIED Requirements

### Requirement: Login screen presentation

The system SHALL present a login screen optimized for phones (not tablets) that displays a large "Qhathia" wordmark as the brand reference, an email input, a password input, and a primary login button. Inputs SHALL use placeholders (no labels) with a representative leading icon each, following the Qhathia Design System input component (rounded 8dp, 40dp height, Urbanist typography).

The keyboard's Done action on the password input SHALL hide the keyboard and submit the login, exactly as the login button does. The password show/hide toggle SHALL be announced in the app's language: "Mostrar contraseña" while the password is hidden, "Ocultar contraseña" while it is shown.

#### Scenario: Screen shown on launch when unauthenticated
- **WHEN** the app starts and no valid session exists
- **THEN** the login screen is displayed with empty email and password inputs and the login button enabled

#### Scenario: Password is visually protected
- **WHEN** the user types into the password input
- **THEN** the characters are masked and a toggle allows showing/hiding the password

#### Scenario: Signing in from the keyboard
- **WHEN** the merchant typed the email and password and taps the keyboard's Done
- **THEN** the login request is sent, as if "Iniciar sesión" had been tapped

#### Scenario: Toggle announced in Spanish
- **WHEN** TalkBack focuses the password toggle with the app in Spanish and the password hidden
- **THEN** it reads "Mostrar contraseña", and "Ocultar contraseña" once the password is shown
