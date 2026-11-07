# tablet-settings Specification

## Purpose
The Ajustes de la tablet screen: the owner reads and changes one kiosk tablet's remote settings and exit PIN from the phone, and sees its details.
## Requirements
### Requirement: Ajustes de la tablet screen

The screen SHALL show a gradient top bar with back and the title "Ajustes de la tablet". It SHALL load the tablet's settings from `GET /api/v1/devices/{deviceId}/settings` and the tablet itself from `GET /api/v1/devices`, in parallel.

It SHALL show four groups:
- **Pantalla del cliente:** "Modo Vitrina en reposo" (switch), "Asistente por voz" (switch), and "Brillo" (Automático / Bajo / Medio / Alto).
- **Seguridad:** "Modo kiosco" (switch) and "PIN de salida".
- **Catálogo:** "Sincronizar automáticamente" (switch).
- **Información:** Nombre, Modelo, Número de serie, Vinculada desde, Versión de la app and Tienda, each showing "—" when absent.

A note SHALL say that changes reach the tablet within a few minutes.

"Nombre" SHALL be tappable and open a dialog to rename the tablet, pre-filled with its current name, at most 40 characters, with "Guardar" and "Cancelar". "Guardar" SHALL be enabled only with a non-blank name different from the current one, and SHALL call `PATCH /api/v1/devices/{deviceId}` with `{"name": …}`.
- On success the dialog SHALL close, Información SHALL show the new name and "Nombre actualizado" SHALL be shown.
- On `422` the dialog SHALL stay open and say "Escribe un nombre de 1 a 40 caracteres".
- On `404` the screen SHALL say the tablet is no longer linked to the store.
- On a network failure the dialog SHALL stay open with "Sin conexión. Inténtalo nuevamente.".

While the screen is visible, the app SHALL silently re-read the settings and the tablet every 30 seconds and each time the screen starts again after its first start. A silent re-read SHALL NOT replace what is shown while a setting is being saved or a dialog (PIN, rename, unlink) is open. A failed re-read SHALL keep what is shown; a `404` SHALL show that the tablet is no longer linked.

- If the settings fail to load, the screen SHALL show an error state with a retry.
- If the server answers `404`, the screen SHALL say the tablet is no longer linked to the store.
- If only the tablet list fails, the Información group SHALL be hidden and the settings SHALL remain usable.

#### Scenario: Loading the settings
- **WHEN** the owner opens Ajustes de la tablet and both requests succeed
- **THEN** each control shows the tablet's current value and Información shows the tablet's details

#### Scenario: Settings fail to load
- **WHEN** the settings request fails
- **THEN** an error state with a retry is shown

#### Scenario: Tablet no longer linked
- **WHEN** the settings request answers `404`
- **THEN** the screen says "Esta tablet ya no está vinculada a tu tienda"

#### Scenario: Device details fail to load
- **WHEN** the settings load but the device list fails
- **THEN** the settings are shown and the Información group is hidden

#### Scenario: A change made elsewhere
- **WHEN** another phone turns off "Asistente por voz" while the screen is open
- **THEN** within 30 seconds the switch shows it off

#### Scenario: No refresh while saving
- **WHEN** a setting is being saved and a silent re-read answers
- **THEN** the control keeps the value being saved

#### Scenario: Renaming the tablet
- **WHEN** the owner taps "Nombre", types "Caja 2" and saves, and the request succeeds
- **THEN** the dialog closes, Información shows "Caja 2" and "Nombre actualizado" is shown

#### Scenario: Name rejected
- **WHEN** the rename answers `422`
- **THEN** the dialog stays open and says "Escribe un nombre de 1 a 40 caracteres"

#### Scenario: Renaming a tablet that is gone
- **WHEN** the rename answers `404`
- **THEN** the screen says "Esta tablet ya no está vinculada a tu tienda"

#### Scenario: Save disabled for an unchanged name
- **WHEN** the rename field is blank or equal to the current name
- **THEN** "Guardar" is disabled

### Requirement: Changing a setting

Changing a switch or the brightness SHALL update the control immediately and SHALL send `PUT /api/v1/devices/{deviceId}/settings` with only that field. While that request is in flight, that control SHALL be disabled, and the other controls SHALL remain usable.

- **On success:** the screen SHALL show the settings returned by the server.
- **On failure:** that control SHALL return to its previous value, and a message SHALL read "No se pudo guardar el ajuste".

#### Scenario: Turning kiosk mode off
- **WHEN** the owner turns "Modo kiosco" off and the server accepts it
- **THEN** the switch stays off and the request body carries only `kioskMode: false`

#### Scenario: Changing brightness
- **WHEN** the owner selects "Alto"
- **THEN** the request body carries only `brightnessMode: "HIGH"` and "Alto" stays selected

#### Scenario: Saving fails
- **WHEN** the owner turns "Asistente por voz" off and the request fails
- **THEN** the switch returns to on and "No se pudo guardar el ajuste" is shown

#### Scenario: Two quick changes
- **WHEN** the owner turns off "Modo Vitrina en reposo" and, before it answers, turns off "Sincronizar automáticamente"
- **THEN** two requests are sent, each with its own field, and both switches end off

### Requirement: Exit PIN

The "PIN de salida" row SHALL read "Configurado" when the tablet has a PIN and "Sin PIN" otherwise. The PIN SHALL never be displayed.

Tapping the row SHALL open a dialog with "Nuevo PIN" and "Repite el PIN". Both fields are numeric and masked. "Guardar" SHALL be enabled only when both fields hold the same 4 to 6 digits. Saving SHALL send `exitPin`.
- **On success:** the dialog closes, the row reads "Configurado", and "PIN actualizado" is shown.
- **On failure:** the dialog stays open with an error.

When a PIN is set, the dialog SHALL also offer "Quitar PIN". It SHALL ask for confirmation ("Cualquiera podrá salir del modo kiosco en esta tablet") and then send `removeExitPin: true`. On success the row reads "Sin PIN" and "PIN quitado" is shown.

While kiosk mode is on and no PIN is set, the "Modo kiosco" row SHALL warn "Sin PIN, cualquiera puede salir del modo kiosco".

#### Scenario: Setting a PIN
- **WHEN** the owner types 1234 in both fields and taps Guardar
- **THEN** the request carries `exitPin: "1234"`, the row reads "Configurado" and "PIN actualizado" is shown

#### Scenario: PINs do not match
- **WHEN** the two fields hold different digits
- **THEN** Guardar is disabled

#### Scenario: PIN too short
- **WHEN** both fields hold 123
- **THEN** Guardar is disabled

#### Scenario: Removing the PIN
- **WHEN** the tablet has a PIN and the owner taps "Quitar PIN" and confirms
- **THEN** the request carries `removeExitPin: true`, the row reads "Sin PIN" and "PIN quitado" is shown

#### Scenario: Kiosk mode without a PIN
- **WHEN** kiosk mode is on and no PIN is set
- **THEN** the "Modo kiosco" row warns that anyone can exit kiosk mode

### Requirement: Unlink a tablet

Ajustes de la tablet SHALL offer "Desvincular tablet", which asks "¿Desvincular <nombre>?" and, on confirm, calls `POST /api/v1/devices/{deviceId}/unlink`.
- On success the screen SHALL close, Dispositivos SHALL drop the card and say "Tablet desvinculada".
- On `404` (the tablet was already unlinked, or belongs to another store) the screen SHALL close as well, Dispositivos SHALL drop the card and say "Esta tablet ya no está vinculada a tu tienda".
- Without connection the screen SHALL stay and say "Sin conexión. Inténtalo nuevamente.".
- On any other failure it SHALL stay and say "No se pudo desvincular la tablet. Inténtalo de nuevo.".

#### Scenario: Unlinking
- **WHEN** the owner confirms "Desvincular" and the request succeeds
- **THEN** the app returns to Dispositivos without that card and shows "Tablet desvinculada"

#### Scenario: Unlinking a tablet that is already gone
- **WHEN** the owner confirms "Desvincular" and the request answers `404`
- **THEN** the app returns to Dispositivos without that card and shows "Esta tablet ya no está vinculada a tu tienda"

#### Scenario: Unlinking without connection
- **WHEN** the owner confirms "Desvincular" without connection
- **THEN** the screen stays and shows "Sin conexión. Inténtalo nuevamente."

