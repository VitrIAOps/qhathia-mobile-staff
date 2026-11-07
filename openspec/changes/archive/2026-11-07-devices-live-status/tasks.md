## 1. Domain and data (`core:domain`, `core:data`)

- [x] 1.1 `DeviceCommand`: `deliveredAt: Instant? = null`, `ackedAt: Instant? = null`, `finishedAt` (design decision 3), `isOverdue(now)`; KDoc
- [x] 1.2 `DeviceMappers.toDomain` (command) maps `deliveredAt` and `ackedAt`
- [x] 1.3 `DeviceMappersTest`: both instants mapped; unparseable → null

## 2. Fixtures and shared UI (`core:testing`, `core:ui`)

- [x] 2.1 `TestFixtures.deviceCommand(...)` with `deliveredAt`, `ackedAt`
- [x] 2.2 `QhathiaComingSoonTag`: `maxLines = 1`, `softWrap = false`

## 3. Dispositivos state and refresh (`features:device-linking`)

- [x] 3.1 `DeviceLinkingUiState`: `RestartPhase`, `restartPhase(device)`; `isCommandBusy` (restart waiting counts, overdue does not); `needsFastPolling`; `deviceIdsNeedingCommandPoll`; `hasCommandsInFlight` ignores overdue; constant `RESTART_BACK_TIMEOUT = 3 min`
- [x] 3.2 `DeviceLinkingViewModel`: single refresh loop and `refreshSilently()` (design decision 1); `DEVICES_REFRESH_INTERVAL = 30 s`; `lastDevicesLoadAt`; `ScreenStarted` refreshes at once from the second start (replaces the plain reload); `DeviceUnlinked(deviceId, alreadyGone)` emits `ShowUnlinked(alreadyGone)` and refreshes
- [x] 3.3 `latestPerType` by `finishedAt ?: requestedAt` (design decision 3)
- [x] 3.4 `DeviceCard`: restart line by phase; command status treats overdue as expired; `networkPresentation` for the connection tile (design decision 6); "Próximamente" under the label; stacked buttons at large font scale (design decision 7)
- [x] 3.5 `DeviceLinkingScreen` / `DeviceLinkingContent`: pass `restartPhase`; `ShowUnlinked(alreadyGone)` picks its message
- [x] 3.6 Strings `values` / `values-en`: `device_command_restarting`, `device_command_restart_not_back`, `device_command_expired` (longer text), `device_network_wifi` ("Wi-Fi"), `device_network_ethernet`, `device_network_offline`, `device_network_other`, `device_unlink_error_network`, `tablet_settings_info_serial` ("Número de serie")
- [x] 3.7 Tests (with `MutableClock`): the loop re-reads devices every 30 s and every 15 s while a command is in flight; a refresh replaces commands (no merge); a second `ScreenStarted` refreshes at once; stop cancels; restart `ACKED` without a later ping → `RESTARTING` and busy; ping after ack → `BACK`; 3 min without ping → `NOT_BACK`, not busy; a `PENDING` polled as `EXPIRED` after its deadline shows expired and stops fast polling; still visible 5 min after expiring, hidden 11 min after; overdue `PENDING` treated as expired; `DeviceUnlinked(alreadyGone = true)` emits the "already gone" message

## 4. Navigation (`features:device-linking`, `app`)

- [x] 4.1 `TABLET_UNLINKED_ALREADY_GONE_KEY`; `deviceLinkingGraph(onTabletUnlinked: (String, Boolean) -> Unit)` reads both keys and clears them
- [x] 4.2 `MainShell` stores both keys on the previous entry before popping

## 5. Ajustes de la tablet (`features:device-linking` settings)

- [x] 5.1 Events `ScreenStarted` / `ScreenStopped`; `TabletSettingsScreen` sends them with `LifecycleStartEffect`
- [x] 5.2 ViewModel: 30 s silent refresh while started and on a later start, guarded (design decision 4); 404 → `NOT_FOUND`
- [x] 5.3 `unlink()`: `NotFound` → `Unlinked(deviceId, alreadyGone = true)`; `Network` → network message; else current message. `TabletSettingsUiEffect.Unlinked(deviceId, alreadyGone)` and `onUnlinked(deviceId, alreadyGone)`
- [x] 5.4 Tests: a later start re-reads; the 30 s refresh applies new values; it is skipped while saving or with a dialog open; a refresh 404 → `NOT_FOUND`; stop cancels; unlink 404 → `Unlinked(alreadyGone = true)`; unlink network → network message and stays

## 6. Mi perfil (`features:profile`)

- [x] 6.1 Plurals `menu_devices_linked` / `menu_devices_connected` in the feminine (es); check the profile tests that assert the subtitle

## 7. Verification

- [x] 7.1 Run, one at a time: `:core:data:testDebugUnitTest`, `:features:device-linking:testDebugUnitTest`, `:features:profile:testDebugUnitTest`, `:app:testDebugUnitTest`, then `testDebugUnitTest`
- [x] 7.2 Run `./gradlew lintDebug` and `./gradlew assembleDebug`

## 8. Manual — QA

- [ ] 8.1 Manual — X:EXP-006 / X:XRE-001: Dispositivos open, the tablet reports → "Activo / hace un momento" within 30 s without leaving; nginx shows `GET /api/v1/devices` every ~30 s and nothing in the background
- [ ] 8.2 Manual — S:DIS-066: unlink a tablet by curl while Dispositivos is open → its card leaves within 30 s; also after coming back from Ajustes
- [ ] 8.3 Manual — X:XCO-031: change a setting by curl while Ajustes is open → seen within 30 s
- [ ] 8.4 Manual — X:XCR-021: Reiniciar with the real tablet → "Enviado…" → "Reiniciando…" → "Listo" ≤ 75 s after the ack; Sim B (acks, never beats) → after 3 min the red "no volvió a reportar" and Reiniciar enabled
- [ ] 8.5 Manual — S:DIS-043 / X:XCR-023: Actualizar catálogo with the tablet stopped → at 10 min "No se pudo entregar…" within 15 s and the button enabled; still shown after leaving and coming back
- [ ] 8.6 Manual — S:DIS-082 / S:EXP-029: unlink by curl, then "Desvincular" in the app → back to Dispositivos without the card and "Esta tablet ya no está vinculada a tu tienda"; one `POST …/unlink 404` in nginx
- [ ] 8.7 Manual — S:DIS-005: heartbeat `ETHERNET` → "Cable" with a cable icon; `MOBILE` → "Datos móviles"; never "OFFLINE" next to "Activo"
- [ ] 8.8 Manual — S:EXP-009 / S:DIS-094 (font 1.3) / S:DIS-010: "Próximamente" in one line, both command buttons readable, "Número de serie", Mi perfil "3 vinculadas (1 activa)"
