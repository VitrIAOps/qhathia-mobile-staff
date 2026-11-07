# biometric-unlock Specification

## Purpose
TBD - created by archiving change add-merchant-profile-settings. Update Purpose after archive.
## Requirements
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

### Requirement: Unlock failure and fallback

If the prompt is dismissed or fails, the unlock screen SHALL remain and the merchant SHALL be able to retry with "Desbloquear con huella". The screen SHALL also offer "Ingresar con contraseña", which runs the sign-out flow and shows the login screen. After signing in again, fingerprint sign-in SHALL remain enabled.

#### Scenario: Retry after cancel
- **WHEN** the merchant cancels the prompt
- **THEN** the unlock screen stays and "Desbloquear con huella" opens the prompt again

#### Scenario: Use password instead
- **WHEN** the merchant taps "Ingresar con contraseña"
- **THEN** the session is signed out and the login screen is shown

### Requirement: Biometrics no longer available

If fingerprint sign-in is enabled but the device has no enrolled biometric at entry, the gate SHALL NOT block the merchant indefinitely. It SHALL show the message "Tu huella ya no está configurada en este equipo. Ingresa con tu contraseña." with "Continuar", and keep the gate up while the message is shown. "Continuar" SHALL run the sign-out flow and, after it, clear the preference, so the next sign-in is not met by a gate that can never open. The preference SHALL NOT be cleared before the merchant continues: clearing it first lifts the gate and skips the message.

#### Scenario: Enrollment removed
- **WHEN** the app starts with fingerprint sign-in enabled and no enrolled biometric
- **THEN** the message is shown with "Continuar" and the merchant does not enter the app

#### Scenario: Continuing after the enrollment was removed
- **WHEN** the merchant taps "Continuar"
- **THEN** the session is signed out, the login screen is shown, and the preference is cleared

#### Scenario: Signing in again
- **WHEN** the merchant then signs in with the password
- **THEN** the app opens without the biometric gate and Seguridad shows "Ingresar con huella" off

