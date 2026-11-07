## Context

`DeviceLinkingViewModel.submitPair()` calls `PairDeviceUseCase(code)` and, on success, closes the dialog and reloads the list. `DeviceRepositoryImpl.pairDevice` sends:
- `deviceName = "Tablet"` and `appVersion = "1.0.0"`, both hard-coded;
- the code only trimmed;
- and it maps every `400` to `PairDeviceError.InvalidCode`, discarding `sessionId` and `deviceId` from the response.

`PairDeviceRequestDto` makes all metadata required. `DeviceLinkingUiState.canSubmitPair` accepts any non-blank code. The dialog relies on `KeyboardCapitalization.Characters`, which is only a hint to the keyboard.

The backend (API_REFERENCE §35, §37):
- `POST /devices/pair` takes `{pairingToken, decision: APPROVE|REJECT, deviceName?}`. Approve answers `{sessionId, status: APPROVED, deviceId, deviceName, …}`; reject answers `{sessionId, status: REJECTED}`.
- A `deviceName` other than "Tablet" is kept as is; "Tablet" or none becomes "Tablet N" (N = active + 1, which repeats after an unlink).
- Errors are `400 INVALID_PAIRING_TOKEN | PAIRING_TOKEN_EXPIRED | PAIRING_TOKEN_ALREADY_USED | PAIRING_TOKEN_CANCELLED`, `409 CATALOG_THRESHOLD_NOT_MET` and `503`.
- `GET /devices/pairing-session/{sessionId}/status` is public and read-only: `{status, tokenExchanged, …}`. The guideline is to poll it for ~30 s after approving.
- `PATCH /devices/{id} {"name"}` renames: `200`, `404`, `422`.
- `POST /devices/{id}/unlink` undoes: `200`, or `404` when the device is already gone.

`problem-detail-errors` maps every `DeviceApiService` call with `toNetworkResult(json)`, so `HttpError.problem?.errorCode` is available.

Modules (no new module; graph unchanged):

```
features:device-linking ──► core:domain ◄── core:data ──► core:network
        │                     (PairedDevice, PairingSessionStatus,   (DTOs, getPairingStatus,
        └─► core:ui            PairDeviceError, use cases)           renameDevice)
            (QhathiaTextField keyboardActions)
core:testing: FakeDeviceRepository, TestFixtures
```

## Goals / Non-Goals

**Goals:**
- A pairing is reported as done only once the tablet exchanged the code. An unanswered approval can be undone.
- The code field only ever holds a code the backend can accept. Each failure says what to do next.
- Every tablet gets a distinct, editable name, and no fake app version.

**Non-Goals:**
- Periodic refresh of Dispositivos and the command fixes (P-2, F-65, F-09, F-61, F-62, F-63): `devices-live-status`. This change only adds a reload when the screen starts again, so a rename shows on return; the next change replaces it with the full refresh.
- Fixing the backend's duplicate "Tablet N" (server side). The app avoids it by proposing a free name.
- Naming through the heartbeat (not implemented by the backend).

## Decisions

### 1. The dialog is a small state machine

```kotlin
sealed interface PairingPhase {
    data object EnteringCode : PairingPhase
    data class WaitingForTablet(val paired: PairedDevice) : PairingPhase
    data class TabletDidNotRespond(val paired: PairedDevice) : PairingPhase
}
```

`DeviceLinkingUiState` gains `pairName`, `pairingPhase = EnteringCode`, `isRejecting` and `isUndoing`. `pairErrorRes` is reused for every phase's error.
- `DismissPairDialog` is ignored outside `EnteringCode`. The dialog sets `dismissOnBackPress`/`dismissOnClickOutside` to false and hides its close button there.
- Closing the dialog, or leaving `WaitingForTablet`, cancels the polling job.
- *Why a sealed phase and not more booleans:* each phase draws a different body and allows different events. A sealed type makes the impossible combinations (waiting and entering a code) unrepresentable.

### 2. Normalize on input, validate by construction

```kotlin
const val PAIRING_CODE_LENGTH = 5
const val PAIRING_ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ"   // same as the backend's
val normalized = input.uppercase(Locale.ROOT).filter { it in PAIRING_ALPHABET }.take(PAIRING_CODE_LENGTH)
```

`canSubmitPair` (shared by Vincular and Rechazar) is `pairCode.length == 5 && pairingPhase == EnteringCode && !isPairing && !isRejecting`. The repository still sends `code.trim().uppercase()` as a defence. The field uses `KeyboardType.Ascii`, autocorrect off, `ImeAction.Done`, and `keyboardActions = KeyboardActions(onDone = { if (canSubmit) onSubmit() })`. For that, `QhathiaTextField` gains `keyboardActions: KeyboardActions = KeyboardActions.Default`, passed to its `BasicTextField`.

### 3. Errors by `errorCode`

`PairDeviceError` becomes:
- `InvalidCode`, `ExpiredCode`, `AlreadyUsedCode`, `CancelledCode`;
- `CatalogNotReady(detail: String?)`, `CatalogCheckUnavailable`;
- `Network`, `Unknown(message)`.

`DeviceRepositoryImpl` maps `HttpError` in this order:
1. the four pairing `errorCode`s;
2. `CATALOG_THRESHOLD_NOT_MET` or `409` → `CatalogNotReady(problem?.detail)`;
3. `503` → `CatalogCheckUnavailable`;
4. any other `400` → `InvalidCode`;
5. the rest → `Unknown`.

`rejectPairing` uses the same mapping. The ViewModel maps each case to one string.
- *Why the app's copy and not the server's `detail`:* the app's text tells the merchant what to do on the tablet ("toca «Generar nuevo código»"). The detail of `CatalogNotReady` is kept in the error for a future, more specific message.

### 4. Approve → wait for the exchange → done, undo or keep waiting

- `pairDevice(code, name): Result<PairedDevice>` returns `PairedDevice(deviceId, sessionId, name)`. A response without `deviceId` or `sessionId` is `Unknown`.
- `GetPairingStatusUseCase(sessionId): Result<PairingSessionStatus(status, tokenExchanged)>`.
- `waitForTablet(paired)` sets `WaitingForTablet` and launches the polling job: up to 15 reads, 2 s apart, ignoring failed reads.
  - On `tokenExchanged`: close the dialog and emit `ShowPaired(name)`. Then `loadDevices()`, `delay(3 s)` and `loadDevices()` again. The tablet's first heartbeat goes out together with the exchange, so the second read shows the card "Activo".
  - After the last read: `TabletDidNotRespond(paired)`.
- `KeepWaiting` restarts `waitForTablet`.
- `UndoPair` calls `UnlinkDeviceUseCase(deviceId)`, which already exists for Ajustes. Success or `404` closes the dialog, emits `ShowPairUndone` and reloads. A network failure keeps the phase with `pairErrorRes = network`. For the `404` to be recognizable, `DeviceRepositoryImpl.unlinkDevice` now fails with `DeviceSettingsException` (`404 → NotFound`), like rename and settings; `devices-live-status` reuses it for F-62. Ajustes' own unlink keeps its current message until then.
- *Why poll `/status` and not `GET /devices` for `lastPingAt`:* `/status` says whether the code was exchanged. The ping cannot tell "not exchanged" from "exchanged but not yet reported" (handoff "No hacer").
- Durations are constants on the ViewModel companion (`PAIR_STATUS_POLL = 2 s`, `PAIR_STATUS_MAX_POLLS = 15`, `FIRST_HEARTBEAT_GRACE = 3 s`), so tests advance virtual time.

### 5. Reject

`RejectPairingUseCase(code)` → `POST /devices/pair {pairingToken, decision: REJECT}`. Success closes the dialog and emits `ShowPairRejected`. Failure stays in `EnteringCode` with the mapped error.

### 6. A free name, proposed

```kotlin
internal fun suggestTabletName(devices: List<Device>): String {
    val used = devices.mapNotNull { it.name?.trim()?.lowercase(Locale.ROOT) }.toSet()
    return generateSequence(1) { it + 1 }.map { "Tablet $it" }.first { it.lowercase(Locale.ROOT) !in used }
}
```

`OpenPairDialog` seeds `pairName` with it. `PairNameChanged` keeps at most 40 characters. The request sends `pairName.trim().ifBlank { null }`. Because "Tablet 2" is not the literal "Tablet", the backend keeps it, so the app avoids the duplicate name without waiting for the server fix. `PairDeviceRequestDto`'s `deviceName`, `platform` and `appVersion` become `String? = null`. With the shared `Json` not encoding defaults, `appVersion` is never sent; `platform` stays `"ANDROID"`.

### 7. Rename from Ajustes

- `RenameDeviceRequestDto(name)`, `DeviceApiService.renameDevice(id, body) = PATCH api/v1/devices/{id}`, which answers the device.
- `DeviceRepository.renameDevice(id, name): Result<Device>` reuses `DeviceSettingsException`: `404 → NotFound`, `400/422 → InvalidInput`.
- `RenameDeviceUseCase`.
- `TabletSettingsUiState.renameDialog: RenameDialogState(value, isSaving, errorRes)`, with events `RenameClicked`, `RenameChanged` (40 characters max), `RenameConfirmed` and `RenameDismissed`.
  - Success → `device = updated`, close the dialog, and `ShowMessage(tablet_settings_renamed)`.
  - `NotFound` → `load = NOT_FOUND`.
  - `InvalidInput` → inline "Escribe un nombre de 1 a 40 caracteres".
  - Network → inline network error.
- `InformationSection` makes the Nombre row tappable (`onClick`), with a pencil hint, only when `device != null`.
- Dispositivos shows the new name on return: `DeviceLinkingViewModel` reloads on `ScreenStarted` from the second start on. That is the minimal half of P-2; `devices-live-status` turns it into the periodic silent refresh.

### 8. Card

`DeviceIdentity`'s status line:
- `lastPingAt == null` → "Aún no se conecta" (neutral);
- otherwise "Activo"/"Inactivo" as today;
- in both cases followed by " · <model>" when `model` is known.

Text only; the existing semantics merge keeps it one heading for TalkBack.

## Risks / Trade-offs

- [Polling a public endpoint for 30 s] → 15 small GETs, only while the dialog is waiting, and cancelled the moment the dialog leaves that phase or the ViewModel is cleared.
- [Undo after the tablet exchanged just past 30 s] → The unlink revokes it (Fase 35 cuts the tablet's token with `DEVICE_UNLINKED`), and the tablet goes back to Welcome. That is the merchant's explicit choice; "Seguir esperando" is offered next to it.
- [The suggested name collides with one added meanwhile by another phone] → The backend keeps whatever name it receives. In the worst case two tablets share a name, and either can be renamed from Ajustes.
- [Reload on screen start] → Same request as today's initial load; the spinner only shows when there are no cards, so a return does not flash.

## Migration Plan

Single merge, no flag. Rollback is reverting the commit. The backend accepts both the old body (with metadata) and the new one.

## Open Questions

_None._
