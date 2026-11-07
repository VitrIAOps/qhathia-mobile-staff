# account-security Specification

## Purpose
TBD - created by archiving change add-merchant-profile-settings. Update Purpose after archive.
## Requirements
### Requirement: Seguridad screen layout

The Seguridad screen SHALL show:
- A gradient top bar with back and the title "Seguridad".
- A card with the row "Cambiar contraseña" and the toggle "Ingresar con huella" (subtitle "Entra sin escribir tu contraseña").
- The section "Sesiones activas" with its session list.
- The action "Cerrar sesión en otros equipos" in the error colour.

The "Cambiar contraseña" row SHALL have a trailing chevron and the subtitle "Actualizada <relative time>", derived from `passwordUpdatedAt` of `GET /api/v1/users/me`. When that value is null, the subtitle SHALL read "Nunca actualizada".

#### Scenario: Password never changed
- **WHEN** `passwordUpdatedAt` is null
- **THEN** the "Cambiar contraseña" subtitle reads "Nunca actualizada"

#### Scenario: Password changed months ago
- **WHEN** `passwordUpdatedAt` is four months before now
- **THEN** the subtitle reads "Actualizada hace 4 meses"

### Requirement: List active sessions

The screen SHALL load `GET /api/v1/auth/sessions`, sending the stored refresh token in the `X-Refresh-Token` header. The backend keeps at most 20 sessions per user, so one request lists them all.

The current session SHALL be shown first, labelled "<deviceLabel> · este equipo", and SHALL have no close action. A session is current if `current` is true or if its `sessionId` equals the stored session id.

The other sessions SHALL be grouped by `deviceLabel` (trimmed; a blank one is shown as the unknown-device label), groups ordered by their most recent use:
- A group with one session SHALL be one row with a device icon, the label, "Activa <relative time of lastUsedAt>" and "Cerrar".
- A group with two or more SHALL be one row reading "<deviceLabel> · <N> sesiones", with "Activa <relative time of the most recent lastUsedAt>" and "Cerrar todas".

#### Scenario: Current session first
- **WHEN** the response contains "Redmi Note 12" with `current` true and "Tablet vitrina" with `current` false
- **THEN** the first row reads "Redmi Note 12 · este equipo" without "Cerrar"
- **AND** the second row reads "Tablet vitrina" with "Cerrar"

#### Scenario: Current flag missing
- **WHEN** every session has `current` false but one `sessionId` equals the stored session id
- **THEN** that session is shown first as "este equipo"

#### Scenario: Several sessions of one device
- **WHEN** the response holds the current session and 19 sessions labelled "qa-sim-tablet"
- **THEN** after "este equipo" a single row reads "qa-sim-tablet · 19 sesiones" with "Cerrar todas"

#### Scenario: Closing every session of a device
- **WHEN** the merchant taps "Cerrar todas" on "qa-sim-tablet · 19 sesiones" and confirms "¿Cerrar 19 sesiones de qa-sim-tablet?"
- **THEN** each of those sessions is closed with `DELETE /api/v1/auth/sessions/{sessionId}`, the list is read again, the group disappears, and this phone's session stays

#### Scenario: A close in the group fails
- **WHEN** one of those requests fails for a reason other than `404`
- **THEN** the list is read again and an error message is shown

#### Scenario: Sessions fail to load
- **WHEN** the request fails
- **THEN** the session section shows an error message with retry, and the rest of the screen stays usable

### Requirement: Close another session

Tapping "Cerrar" on a session SHALL ask for confirmation, then call `DELETE /api/v1/auth/sessions/{sessionId}`. On `204` or `404`, the row SHALL be removed. On any other failure, the row SHALL stay and an error message SHALL be shown.

#### Scenario: Session closed
- **WHEN** the merchant confirms closing "Tablet vitrina" and the API returns `204`
- **THEN** the row disappears from the list

#### Scenario: Session already gone
- **WHEN** the API returns `404`
- **THEN** the row disappears from the list without an error

### Requirement: Close all other sessions

Tapping "Cerrar sesión en otros equipos" SHALL ask for confirmation, then call `POST /api/v1/auth/sessions/revoke-others` with the stored refresh token in `X-Refresh-Token`. On success, the list SHALL keep only the current session. The action SHALL be hidden when the current session is the only one listed.

#### Scenario: Revoke others
- **WHEN** the merchant confirms and the API returns `204`
- **THEN** only "este equipo" remains in the list and the merchant stays signed in

#### Scenario: Nothing to revoke
- **WHEN** the only listed session is the current one
- **THEN** "Cerrar sesión en otros equipos" is not shown

### Requirement: Change password

"Cambiar contraseña" SHALL open a screen with:
- A gradient top bar with back.
- The password fields "Contraseña actual", "Nueva contraseña" and "Confirmar nueva contraseña", each with show/hide.
- A primary "Guardar" button.

The button SHALL be enabled only when the current password is non-empty, the new password has 8–100 characters, and the confirmation matches. On submit, the app SHALL call `POST /api/v1/auth/change-password` with `currentPassword`, `newPassword` and the stored refresh token as `currentRefreshToken`. It SHALL send it once: a wrong current password does not trigger a token refresh. The keyboard's Done action on the confirmation field SHALL submit when "Guardar" is enabled.

On `204`, the app SHALL return to Seguridad, show "Contraseña actualizada", and keep the merchant signed in. Errors SHALL map as follows, by the ProblemDetail `errorCode` first, and the form SHALL stay open:

| Response | Shown |
|---|---|
| `401` with `CURRENT_PASSWORD_INCORRECT` | "La contraseña actual no es correcta" on the current-password field |
| `400` | "La nueva contraseña debe ser distinta a la actual" |
| `422` | "La contraseña debe tener entre 8 y 100 caracteres" |
| Connectivity failure | The generic connectivity message |
| Any other failure | The generic error message |

While the request is in flight, the button SHALL show progress and the inputs SHALL be disabled, and back SHALL do nothing.

With any field typed, the system back and the top bar's back SHALL ask "¿Descartar cambios?" with "Descartar" and "Seguir editando". "Seguir editando" SHALL keep the typed values; "Descartar" SHALL return to Seguridad. With the three fields empty, back SHALL return at once.

#### Scenario: Password changed
- **WHEN** valid passwords are submitted and the API returns `204`
- **THEN** the app returns to Seguridad with "Contraseña actualizada" and the session remains active
- **AND** the "Cambiar contraseña" subtitle reads "Actualizada hace un momento" after the profile reloads

#### Scenario: Wrong current password
- **WHEN** the API returns `401` with `errorCode` `CURRENT_PASSWORD_INCORRECT`
- **THEN** the current-password field shows "La contraseña actual no es correcta"
- **AND** only one change-password request and no refresh request were sent
- **AND** the merchant is not signed out

#### Scenario: Submitting from the keyboard
- **WHEN** the three fields are valid and the merchant taps Done on the confirmation field
- **THEN** the change-password request is sent

#### Scenario: Confirmation mismatch
- **WHEN** the confirmation differs from the new password
- **THEN** the confirmation field shows "Las contraseñas no coinciden" and "Guardar" is disabled

#### Scenario: Back with typed passwords
- **WHEN** the merchant has typed in any of the three fields and presses back (system or top bar)
- **THEN** "¿Descartar cambios?" is shown
- **AND** "Seguir editando" keeps the three values, and "Descartar" returns to Seguridad

#### Scenario: Back with empty fields
- **WHEN** the three fields are empty and the merchant presses back
- **THEN** the app returns to Seguridad without asking

### Requirement: Fingerprint sign-in setting

The "Ingresar con huella" toggle SHALL reflect a preference stored locally on the phone, which is off by default. It SHALL use only the biometrics already enrolled in the device settings, and no credential SHALL be sent to or stored for the backend.

Turning the toggle on SHALL first check the device's biometric capability:
- **Enrolled**: show the system biometric prompt. Turn the preference on only after a successful authentication; if the prompt is cancelled or fails, leave it off.
- **Hardware present, nothing enrolled**: leave the preference off and show "Configura tu huella en los ajustes del equipo para usar esta opción", with an action that opens the system biometric enrollment settings where available, or the security settings otherwise.
- **No biometric hardware, or hardware unavailable**: leave the preference off and show "Tu equipo no tiene lector de huella disponible".

Turning the toggle off SHALL turn the preference off without a prompt.

When Seguridad finds the preference on but no usable biometric, it SHALL turn the preference off and say so: "Desactivamos «Ingresar con huella» porque este equipo ya no tiene huellas registradas."

#### Scenario: Enable with enrolled fingerprint
- **WHEN** the device has an enrolled biometric and the merchant turns the toggle on and authenticates
- **THEN** the toggle stays on and the preference is persisted

#### Scenario: No biometric configured
- **WHEN** the device has biometric hardware but nothing enrolled and the merchant turns the toggle on
- **THEN** the toggle stays off
- **AND** the message "Configura tu huella en los ajustes del equipo para usar esta opción" is shown with an action to open the device settings

#### Scenario: Prompt cancelled
- **WHEN** the merchant cancels the biometric prompt while enabling
- **THEN** the toggle stays off

#### Scenario: Enrollment removed later
- **WHEN** the preference is on but the device no longer has an enrolled biometric
- **THEN** the Seguridad screen shows the toggle off, the preference is cleared, and the message "Desactivamos «Ingresar con huella»…" is shown

#### Scenario: Disable
- **WHEN** the merchant turns the toggle off
- **THEN** the preference is cleared without a biometric prompt

