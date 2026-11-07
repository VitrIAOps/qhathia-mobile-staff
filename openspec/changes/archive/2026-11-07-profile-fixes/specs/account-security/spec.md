## MODIFIED Requirements

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
