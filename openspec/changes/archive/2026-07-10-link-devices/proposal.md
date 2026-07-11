## Why

Store owners need a way to add tablets (terminals) to their store from the staff app so those tablets can join the catalog/kiosk ecosystem. Today the staff app has no device-management surface — the Profile tab is a placeholder. This POC delivers the first end-to-end device-linking flow: view the store's linked tablets and pair a new one with a code.

## What Changes

- Add a **"Vincular terminal"** (link terminal) entry point in the Profile section that navigates to a new full-screen destination **without** the bottom navigation bar.
- Add a **Linked Devices** screen that lists the tablets already paired to the store, with a bold top-left "Dispositivos vinculados" title and an empty-state message inviting the user to tap **+** when none exist.
- Add a **floating + button** that opens a near-full-screen dialog with a single code input and a submit button to pair a new tablet.
- On submit, call the pairing API to **approve** the tablet; on error show an inline error, on success close the dialog and **reload** the device list.
- List paired tablets via `GET /api/v1/devices` (OWNER-scoped, returns the store's devices). Pair via `POST /api/v1/devices/pair` with `decision = APPROVE`.

> Note: the source task referenced `GET /api/v1/devices/me` for the list, but that endpoint is DEVICE-role only and returns a single record. Confirmed to use `GET /api/v1/devices` (OWNER) instead — see design.md.

## Capabilities

### New Capabilities
- `device-linking`: Owner-facing management of the store's terminals — listing paired devices and pairing a new device by approving a pairing code.

### Modified Capabilities
<!-- None: no existing spec's requirements change. The Profile tab currently shows a placeholder; wiring the new entry point is additive. -->

## Impact

- **New module:** `features/device-linking` (Compose screen, ViewModel, MVI contract, near-full-screen pair dialog, navigation graph).
- **`core:domain`:** new `Device` model, `DeviceRepository` port, `GetDevicesUseCase`, `PairDeviceUseCase`, and a `PairDeviceError` type.
- **`core:data`:** `DeviceRepositoryImpl`, DTO→domain mappers, Hilt binding in `DataModule`.
- **`core:network`:** `DeviceApiService` (`GET /api/v1/devices`, `POST /api/v1/devices/pair`), request/response DTOs, provider in `NetworkModule` (authenticated client).
- **`:app`:** register the new destination inside `MainShell`'s inner NavHost as a non-top-level route (so the bottom bar hides), and wire the Profile placeholder's entry point to it.
- **APIs consumed:** `GET /api/v1/devices`, `POST /api/v1/devices/pair`. No new permissions (INTERNET already declared).
