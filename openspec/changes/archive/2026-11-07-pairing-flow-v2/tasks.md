## 1. Network (`core:network`)

- [x] 1.1 `PairDeviceRequestDto`: `deviceName`, `platform`, `appVersion` become `String? = null`; KDoc (omitted fields are not sent)
- [x] 1.2 New `PairingSessionStatusDto(sessionId, status, expiresAt, tokenExchanged = false, tokenExchangedAt, deviceId)` and `RenameDeviceRequestDto(name)`
- [x] 1.3 `DeviceApiService.getPairingStatus(sessionId)` (`GET api/v1/devices/pairing-session/{id}/status`) and `renameDevice(id, body)` (`PATCH api/v1/devices/{id}`), both with `toNetworkResult(json)`
- [x] 1.4 `DeviceApiServiceTest`: the approve body has no `appVersion` and carries the name; status decodes `tokenExchanged`; rename sends `PATCH` with the name

## 2. Domain (`core:domain`)

- [x] 2.1 New models `PairedDevice(deviceId, sessionId, name)` and `PairingSessionStatus(status, tokenExchanged)`
- [x] 2.2 `PairDeviceError`: `InvalidCode`, `ExpiredCode`, `AlreadyUsedCode`, `CancelledCode`, `CatalogNotReady(detail)`, `CatalogCheckUnavailable`, `Network`, `Unknown`
- [x] 2.3 `DeviceRepository`: `pairDevice(code, name): Result<PairedDevice>`, `rejectPairing(code)`, `getPairingStatus(sessionId)`, `renameDevice(id, name): Result<Device>`; KDocs
- [x] 2.4 Use cases: `PairDeviceUseCase(code, name)` (updated), `RejectPairingUseCase`, `GetPairingStatusUseCase`, `RenameDeviceUseCase`

## 3. Data (`core:data`)

- [x] 3.1 `DeviceRepositoryImpl.pairDevice`: body with the uppercased code, `APPROVE`, the name (or null), platform `ANDROID`, no app version; map the response to `PairedDevice` (missing ids → `Unknown`); drop `DEFAULT_DEVICE_NAME` / `DEFAULT_APP_VERSION`
- [x] 3.2 `rejectPairing` (decision `REJECT`), `getPairingStatus`, `renameDevice` (`DeviceSettingsException`: 404 → `NotFound`, 400/422 → `InvalidInput`)
- [x] 3.3 `toPairError()` on `HttpError` by `errorCode` first (design decision 3); update the class KDoc
- [x] 3.4 `DeviceRepositoryImplTest`: each `errorCode` maps to its case; `409` → `CatalogNotReady` with the detail; `503` → `CatalogCheckUnavailable`; plain `400` → `InvalidCode`; success returns the ids; missing ids → `Unknown`; reject sends `REJECT`; rename maps 404/422

## 4. Testing fixtures (`core:testing`)

- [x] 4.1 `FakeDeviceRepository`: `pairResult: Result<PairedDevice>`, `lastPairName`, `rejectResult` / `rejectedCodes`, `statusResults: ArrayDeque<Result<PairingSessionStatus>>` (then a default), `renameResult` / `renamedTo`, `unlinkResult` if missing
- [x] 4.2 `TestFixtures.pairedDevice(...)`

## 5. Shared UI (`core:ui`)

- [x] 5.1 `QhathiaTextField(keyboardActions: KeyboardActions = KeyboardActions.Default)` passed to `BasicTextField`; KDoc

## 6. Pairing (`features:device-linking`)

- [x] 6.1 `DeviceLinkingUiState`: `PAIRING_CODE_LENGTH`, `PAIRING_ALPHABET`, `pairName`, `pairingPhase` (new `PairingPhase`), `isRejecting`, `isUndoing`; `canSubmitPair` per design decision 2; KDoc
- [x] 6.2 Events `PairNameChanged`, `RejectPair`, `KeepWaiting`, `UndoPair`; effects `ShowPaired(name)`, `ShowPairRejected`, `ShowPairUndone`
- [x] 6.3 ViewModel: inject `RejectPairingUseCase`, `GetPairingStatusUseCase`, `UnlinkDeviceUseCase`; `OpenPairDialog` seeds `suggestTabletName`; code normalization; `submitPair` → `waitForTablet` (design decision 4); `KeepWaiting`, `UndoPair`, `RejectPair`; `DismissPairDialog` ignored outside `EnteringCode` and cancels the job; each `PairDeviceError` mapped to its string
- [x] 6.4 ViewModel: `ScreenStarted` reloads the list from the second start on (design decision 7)
- [x] 6.5 `PairDeviceDialog`: three bodies by phase; name field; code field with helper, `Ascii`, no autocorrect, Done submits; "Rechazar"; not dismissable outside `EnteringCode`
- [x] 6.6 `DeviceLinkingScreen`: snackbars for the three new effects
- [x] 6.7 `DeviceCard.DeviceIdentity`: "Aún no se conecta" when `lastPingAt == null`; " · <model>" when known
- [x] 6.8 Strings in `values` / `values-en`: `device_pair_name_placeholder`, `device_pair_code_helper` (`%1$d`), `device_pair_reject`, `device_pair_waiting`, `device_pair_not_responding`, `device_pair_undo`, `device_pair_keep_waiting`, `device_pair_success` (`%1$s`), `device_pair_rejected`, `device_pair_undone`, `device_pair_error_expired`, `device_pair_error_used`, `device_pair_error_cancelled`, `device_pair_error_catalog`, `device_pair_error_catalog_unavailable`, `device_pair_error_network`, `device_never_reported`, `device_status_with_model` (`%1$s · %2$s`); reword `device_pair_error_invalid_code`
- [x] 6.9 `DeviceLinkingViewModelTest`: lowercase is uppercased; ambiguous characters dropped; 4 characters cannot submit; the dialog suggests the first free "Tablet N"; the typed name is sent; exchanged → dialog closed, `ShowPaired`, list read again and again 3 s later; never exchanged → `TabletDidNotRespond` after 30 s; keep waiting polls again; undo unlinks and closes (404 too); reject sends the code and closes; dismiss ignored while waiting; each error maps to its string; a second `ScreenStarted` reloads

## 7. Rename (`features:device-linking` settings)

- [x] 7.1 `TabletSettingsModels`: `RenameDialogState(value, isSaving, errorRes)` with `canSave`; events `RenameClicked`, `RenameChanged`, `RenameConfirmed`, `RenameDismissed`
- [x] 7.2 `TabletSettingsViewModel`: inject `RenameDeviceUseCase`; per design decision 7
- [x] 7.3 `TabletSettingsContent`: the Nombre row is tappable; `RenameTabletDialog` (prefilled, 40 max, Guardar/Cancelar, inline error)
- [x] 7.4 Strings: `tablet_settings_rename_title`, `tablet_settings_rename_save`, `tablet_settings_rename_cancel`, `tablet_settings_rename_invalid`, `tablet_settings_renamed`, `tablet_settings_rename_network`
- [x] 7.5 `TabletSettingsViewModelTest`: rename updates the device and shows the message; 404 → `NOT_FOUND`; 422 keeps the dialog with the error; blank or unchanged cannot save

## 8. Verification

- [x] 8.1 Run, one at a time: `:core:network:testDebugUnitTest`, `:core:domain:test`, `:core:data:testDebugUnitTest`, `:features:device-linking:testDebugUnitTest`, then `testDebugUnitTest`
- [x] 8.2 Run `./gradlew lintDebug` and `./gradlew assembleDebug`

## 9. Manual — QA

- [ ] 9.1 Manual — X:XVI-021 / S:DIS-030: type "rwnza" → "RWNZA"; with 4 characters "Vincular" is disabled and nginx sees no `POST /pair`; "O0I1L" never appears
- [ ] 9.2 Manual — S:DIS-024: "ZZZZZ" → "no existe"; an expired code → "venció"; a used one → "ya se usó"; S:DIS-026: correct it and retry without closing
- [ ] 9.3 Manual — X:XVI-001: approve within 60 s → "Tablet «…» vinculada" in ≤ 5 s and the card turns "Activo" without leaving the screen
- [ ] 9.4 Manual — X:XVI-010: approve a code nobody exchanges → "La tablet no recibió la aprobación" in ≤ 30 s → "Deshacer" → `POST /devices/{id}/unlink 200`, no new card; nginx shows `GET …/status` every 2 s
- [ ] 9.5 Manual — X:XVI-020: "Rechazar" → `POST /devices/pair` with `"decision":"REJECT"` → the tablet shows "El comercio rechazó la vinculación"
- [ ] 9.6 Manual — S:DIS-006: pair with "Mostrador" → card "Mostrador"; rename to "Caja 2" in Ajustes → back on Dispositivos the card says "Caja 2" (`PATCH /devices/{id} 200`)
- [ ] 9.7 Manual — X:XVI-030 / X:EXP-011 / X:XCR-034: two tablets paired with the proposed names → "Tablet 1" and "Tablet 2", the model visible, the older one first; "Versión de la app" shows "—" until the first heartbeat, never "1.0.0"
