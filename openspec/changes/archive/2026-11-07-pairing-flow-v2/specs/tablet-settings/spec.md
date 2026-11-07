## MODIFIED Requirements

### Requirement: Ajustes de la tablet screen

The screen SHALL show a gradient top bar with back and the title "Ajustes de la tablet". It SHALL load the tablet's settings from `GET /api/v1/devices/{deviceId}/settings` and the tablet itself from `GET /api/v1/devices`, in parallel.

It SHALL show four groups:
- **Pantalla del cliente:** "Modo Vitrina en reposo" (switch), "Asistente por voz" (switch), and "Brillo" (Automático / Bajo / Medio / Alto).
- **Seguridad:** "Modo kiosco" (switch) and "PIN de salida".
- **Catálogo:** "Sincronizar automáticamente" (switch).
- **Información:** Nombre, Modelo, N.º de serie, Vinculada desde, Versión de la app and Tienda, each showing "—" when absent.

A note SHALL say that changes reach the tablet within a few minutes.

"Nombre" SHALL be tappable and open a dialog to rename the tablet, pre-filled with its current name, at most 40 characters, with "Guardar" and "Cancelar". "Guardar" SHALL be enabled only with a non-blank name different from the current one, and SHALL call `PATCH /api/v1/devices/{deviceId}` with `{"name": …}`.
- On success the dialog SHALL close, Información SHALL show the new name and "Nombre actualizado" SHALL be shown.
- On `422` the dialog SHALL stay open and say "Escribe un nombre de 1 a 40 caracteres".
- On `404` the screen SHALL say the tablet is no longer linked to the store.
- On a network failure the dialog SHALL stay open with "Sin conexión. Inténtalo nuevamente.".

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
