## Why

Dispositivos and Ajustes de la tablet show a snapshot taken when the screen opened, and the command lines tell the merchant things that are not true (`docs/handoff/android-regresion-2026-11-03.md` §3.1, §3.3):

- **F-65 / P-2** — The cards never refresh: a tablet unlinked elsewhere stays "Activo", and "Actualizado hace 13 min" is shown while the tablet reports every minute. Ajustes de la tablet loads once, so a change made from another phone is never seen.
- **F-09** — "Reinicio: Listo" appears as soon as the tablet acknowledges the restart, even when it never comes back. The tablet acknowledges *before* the new process reports, so only a heartbeat after the ack proves it is back.
- **F-61** — An expired command never says "No se pudo entregar". It is filtered out because it "finished" more than 10 minutes after it was requested, so the polling keeps the old "Enviado…" forever with the button disabled, and the line disappears on reopen.
- **F-62** — Unlinking a tablet that is already gone (`404`) says "Inténtalo de nuevo" forever, and the card stays.
- **F-63** — "ETHERNET" is shown raw, always with a Wi-Fi icon, and "OFFLINE" can sit next to "Activo".
- **F-66** — Visual glitches: "Próximament / e" wraps, "Actualizar catálo…" is cut, "N.º" renders as "N.o" in the app's font, and Mi perfil says "3 vinculados (1 activo)" about tablets (feminine in Spanish).

`pairing-flow-v2` already makes `unlinkDevice` fail with `DeviceSettingsError.NotFound` on a `404`, which F-62 needs.

## What Changes

- **Dispositivos refreshes itself (P-2, F-65).** While visible, the screen silently re-reads `GET /devices` every 30 s, and every 15 s while a command is in flight or a restart is waiting for the tablet. Commands are read only for the tablets that need it. Coming back to the screen refreshes at once. Failed refreshes keep what is shown. Nothing runs in the background.
- **Ajustes de la tablet refreshes too (F-65).** While visible, every 30 s and on coming back, it silently re-reads the settings and the tablet, unless a setting is being saved or a dialog is open. A `404` shows "Esta tablet ya no está vinculada a tu tienda".
- **Restart shows its real outcome (F-09).** The restart line moves through:
  - "Enviado…" (pending);
  - "Reiniciando… esperando a que la tablet vuelva a reportar" (delivered, or acknowledged with no heartbeat since);
  - "Listo" (a heartbeat after the ack);
  - after 3 minutes with no heartbeat, "La tablet no volvió a reportar tras el reinicio. Revísala." in red, with Reiniciar enabled again.
- **Expired commands are shown (F-61).** A finished command stays visible for 10 minutes counted from when it *finished*. An expired one reads "No se pudo entregar: la tablet no se conectó en 10 min. Revisa que esté encendida y con internet." and its button is enabled. Each poll replaces a tablet's commands instead of merging them. A command past its `expiresAt` (plus 30 s) is treated as expired even before the server says so.
- **Unlinking a tablet that is gone (F-62).** In Ajustes, a `404` returns to Dispositivos without the card and says "Esta tablet ya no está vinculada a tu tienda". No connection says "Sin conexión. Inténtalo nuevamente.".
- **Connection tile (F-63).** WIFI → "Wi-Fi" with a Wi-Fi icon, MOBILE → "Datos móviles" with a signal icon, ETHERNET → "Cable" with a cable icon, OFFLINE → "Sin conexión" (or "En línea" if the tablet is active), and anything else → "Otra red". The raw value is never shown.
- **Visual details (F-66).**
  - The "Próximamente" tag never wraps, and it sits under the "Clientes hoy" label.
  - The two command buttons stack when the font is large.
  - "N.º de serie" becomes "Número de serie".
  - Mi perfil says "3 vinculadas (1 activa)".

## Capabilities

### New Capabilities

_None._ (`tablet-settings` gains the requirement "Unlink a tablet", which shipped without a spec.)

### Modified Capabilities

- `device-linking`: "List of linked devices" (periodic and on-return refresh, connection labels and icons), "Remote commands to a tablet" (restart phases, expired text), "Follow command outcome" (what is polled and how long finished commands stay).
- `tablet-settings`: "Ajustes de la tablet screen" (silent refresh, "Número de serie", unlinking a tablet that is gone).
- `merchant-profile`: "Menu rows show live subtitles" (the Dispositivos subtitle as shipped, now in the feminine).

## Impact

- **`core:domain`** — `DeviceCommand.deliveredAt`, `ackedAt`, `finishedAt`.
- **`core:data`** — `DeviceMappers` maps both instants.
- **`core:ui`** — `QhathiaComingSoonTag` never wraps.
- **`core:testing`** — `TestFixtures.deviceCommand(...)` with the new fields.
- **`features:device-linking`**
  - `DeviceLinkingUiState`: `RestartPhase`, `restartPhase()`, `needsFastPolling`, `deviceIdsNeedingCommandPoll`, local expiry.
  - `DeviceLinkingViewModel`: a single refresh loop, `latestPerType` by finish time, unlinked-already-gone handling.
  - `DeviceCard`: restart line, network tile, tag placement, stacking buttons.
  - `TabletSettings`: `ScreenStarted` / `ScreenStopped`, silent refresh, unlink 404 and network.
  - Navigation: the "already gone" flag.
  - Strings.
- **`app`** — `MainShell` passes the "already gone" flag back to Dispositivos.
- **`features:profile`** — plurals in `values` / `values-en`.
- **Depends on** `pairing-flow-v2` (branch cut from it: same ViewModel and the `NotFound` on unlink).
- **Not in this change:**
  - showing "Otro equipo cambió estos ajustes" from `settingsVersion` (optional in the handoff; the silent refresh already shows the new values);
  - renaming "Actualizado" to "Último reporte" (optional).
