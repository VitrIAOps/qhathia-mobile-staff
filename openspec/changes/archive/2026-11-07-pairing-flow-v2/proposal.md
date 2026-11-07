## Why

Pairing a tablet from the phone is the start of every store's setup, and the 2026-11-03 QA regression (`docs/handoff/android-regresion-2026-11-03.md` §3.1) finds it unreliable. The backend already supports everything needed (Fase 37):

- **F-60** — The phone treats the `200` of `POST /devices/pair` as "linked". That answer only means "approved": the tablet still has to exchange the code. If it never does, the phone closes the dialog with no warning and leaves an orphan "Inactivo" card. There is no way to reject a code, although the backend accepts `decision: REJECT`.
- **F-58** — The code field keeps lowercase, accepts 4 characters and the ambiguous O/0/I/1/L. Every error shows the same "Código inválido o expirado", although the backend tells apart invalid, expired, used and cancelled codes (all `400`, distinguished only by `errorCode`).
- **F-01** — Right after pairing, the card says "Inactivo" until the merchant leaves and re-enters the screen, because the list is read before the tablet's first heartbeat.
- **F-59** — Every tablet paired from the phone is sent as `deviceName: "Tablet"` and `appVersion: "1.0.0"`. The backend then names it "Tablet N", and has already given the same name to two live tablets; the version reads "1.0.0" until the first heartbeat. A tablet cannot be renamed, and two cards cannot be told apart.

`problem-detail-errors` (P-1) already exposes the `errorCode`, which this change needs to tell the four `400`s apart.

## What Changes

- **Name the tablet when pairing (F-59).** The dialog gets a second field, "Nombre de la tablet", pre-filled with the first free "Tablet N" among the store's tablets and editable (up to 40 characters). The typed name is sent as `deviceName`; `appVersion` and the hard-coded "Tablet" are no longer sent.
- **Code field (F-58).** The field uppercases what is typed, drops anything outside the pairing alphabet (no O, 0, I, 1, L) and stops at 5 characters. A helper shows "5 caracteres, sin O, 0, I, L ni 1 · n/5". "Vincular" is enabled only with exactly 5, and the keyboard's Done key submits.
- **Distinct errors (F-58).** The dialog says which error it is:
  - "Ese código no existe…"
  - "El código venció…"
  - "Ese código ya se usó…"
  - "La tablet descartó ese código…"
  - "Tu catálogo aún no tiene suficientes productos…"
  - "No pudimos verificar tu catálogo…"
  - "Sin conexión…"
- **Wait for the tablet (F-60).** After approving, the dialog shows "Esperando a que la tablet confirme…" and polls `GET /devices/pairing-session/{id}/status` every 2 s for up to 30 s.
  - When the tablet exchanges the code, the dialog closes, "Tablet «Mostrador» vinculada" is shown, and the list is read again (and once more 3 s later, so the card is "Activo" without leaving).
  - If 30 s pass, the dialog says "La tablet no recibió la aprobación…" and offers "Deshacer vinculación" (unlinks the device just approved) or "Seguir esperando".
  - While waiting, the dialog cannot be dismissed.
- **Reject (F-60).** The dialog gets "Rechazar", which sends `decision: REJECT` for the typed code; the tablet then shows "El comercio rechazó la vinculación".
- **Card (F-01, F-59).** A tablet that never reported says "Aún no se conecta" instead of "Inactivo", and the card shows the tablet's model next to its status.
- **Rename (F-59).** In Ajustes de la tablet, "Nombre" opens a dialog to rename the tablet (`PATCH /api/v1/devices/{id}`), up to 40 characters; on success "Nombre actualizado".
- **Dispositivos reloads on return.** Coming back to the screen (for example from Ajustes after a rename) reads the list again. This is the minimal part of P-2; the periodic refresh comes with `devices-live-status`.
- **Shared text field.** `QhathiaTextField` gains a `keyboardActions` parameter, which F-50 (login) will reuse.

## Capabilities

### New Capabilities

_None._

### Modified Capabilities

- `device-linking`: "List of linked devices" (status wording as shipped — "Activo"/"Inactivo" — plus "Aún no se conecta" and the model), "Pair a new device via code" (name, code rules, distinct errors, waiting for the exchange, undo, reject).
- `tablet-settings`: "Ajustes de la tablet screen" (rename from Información).

## Impact

- **`core:network`** — `PairDeviceRequestDto` (optional metadata), new `PairingSessionStatusDto` and `RenameDeviceRequestDto`; `DeviceApiService.getPairingStatus` and `renameDevice`.
- **`core:domain`** — `PairedDevice`, `PairingSessionStatus`; `PairDeviceError` with the new cases; `DeviceRepository.pairDevice(code, name)`, `rejectPairing`, `getPairingStatus`, `renameDevice`; use cases `PairDeviceUseCase` (updated), `RejectPairingUseCase`, `GetPairingStatusUseCase`, `RenameDeviceUseCase`.
- **`core:data`** — `DeviceRepositoryImpl` (errors by `errorCode`, no default metadata, new calls).
- **`core:ui`** — `QhathiaTextField(keyboardActions)`.
- **`core:testing`** — `FakeDeviceRepository`, `TestFixtures`.
- **`features:device-linking`** — `DeviceLinkingUiState` (`pairName`, `pairingPhase`, `isRejecting`), events, effects, ViewModel, `PairDeviceDialog`, `DeviceCard`, `DeviceLinkingScreen` snackbars; `TabletSettings` (rename dialog, events, state); strings.
- **Depends on** `problem-detail-errors` only. The branch is cut from it, independent of the product and order changes.
- **Not in this change:** the periodic refresh of Dispositivos and the command fixes (P-2, F-65, F-09, F-61, F-62, F-63) — `devices-live-status`. The backend's own duplicate "Tablet N" is fixed server-side; the app avoids it by always sending a free name.
