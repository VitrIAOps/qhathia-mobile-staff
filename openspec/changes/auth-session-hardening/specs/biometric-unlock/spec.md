## MODIFIED Requirements

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
