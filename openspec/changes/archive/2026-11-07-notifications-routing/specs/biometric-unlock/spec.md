## MODIFIED Requirements

### Requirement: Biometric gate on app entry

When a stored session exists and fingerprint sign-in is enabled, entering the app SHALL show an unlock screen instead of the main shell. The unlock screen SHALL show the Qhathia wordmark, the user's name, and a "Desbloquear con huella" button, and SHALL present the system biometric prompt automatically.

The gate SHALL apply on a cold start and when the app returns to the foreground after at least 5 minutes in the background. It SHALL NOT reappear on configuration changes. The shell, and any destination requested by a notification tap, SHALL be shown only after a successful authentication. A destination requested by a notification tap while the gate is due SHALL be kept until then and opened right after the authentication.

#### Scenario: Cold start with fingerprint enabled
- **WHEN** the app starts with a stored session and fingerprint sign-in enabled
- **THEN** the unlock screen is shown and the biometric prompt opens
- **AND** after a successful authentication the main shell is shown

#### Scenario: Fingerprint disabled
- **WHEN** the app starts with a stored session and fingerprint sign-in disabled
- **THEN** the main shell is shown directly

#### Scenario: Notification tap while locked
- **WHEN** the merchant taps an order notification and the app opens behind the gate
- **THEN** Solicitudes is shown only after a successful authentication

#### Scenario: Short background
- **WHEN** the app returns to the foreground less than 5 minutes after leaving it
- **THEN** no unlock is requested

#### Scenario: Notification tap after a long background
- **WHEN** fingerprint sign-in is enabled, the app has been in the background for at least 5 minutes, and the merchant taps an order notification
- **THEN** the unlock screen is shown, and after a successful authentication the app opens on Solicitudes with the Pendientes tab selected, not on Inicio

#### Scenario: Notification tap without fingerprint sign-in
- **WHEN** fingerprint sign-in is disabled and the merchant taps an order notification
- **THEN** the app opens on Solicitudes with the Pendientes tab selected, and the destination is not opened again later
