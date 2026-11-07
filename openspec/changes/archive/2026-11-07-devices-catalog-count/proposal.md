## Why

Two fixes from the 2026-11-03 QA regression (`docs/handoff/android-regresion-2026-11-03.md` §3.3 and §3.2) touch Dispositivos, which changes 4 and 5 rewrote, so they were split out of `home-stats-fixes`:

- **F-94** — "Catálogo visible 50 productos" counts every active product, but the tablet hides the ones out of stock, low on stock or "a consultar". The backend now sends `visibleProducts` and `hidden` on `GET /api/v1/catalog/count` (§42): 46 visible, 4 hidden. The count is also read only on the first load, so a product created meanwhile does not show until the app restarts (X:XCA-050).
- **F-76 (Dispositivos)** — "podrás reintentar a las 17:04" ignores the phone's 12-hour setting, as Inicio did.

## What Changes

- **Visible catalog (F-94).** The card shows the products the tablet shows ("46 productos") and, when some are hidden, "4 no se muestran (agotados, pocas unidades o a consultar)". Without `visibleProducts` (older backend) it keeps "50 productos".
- **The count follows the store.** Coming back to Dispositivos re-reads the catalog count with the devices, and so does an "Actualizar catálogo" command that reaches `ACKED`.
- **12/24 hours (F-76).** The retry time uses the shared `shortTimeFormatter` (the phone's setting and the app's language). The `core:ui` helper is the same file `home-stats-fixes` adds, so the two branches merge cleanly.

## Capabilities

### New Capabilities

_None._

### Modified Capabilities

- `device-linking`: "List of linked devices" (visible and hidden products; re-read on return and after a catalog refresh), "Remote commands to a tablet" (12/24-hour retry time). This delta builds on the version in `devices-live-status`, which must be archived first.

## Impact

- **`core:network`** — `CatalogCountDto.visibleProducts`, `hidden`.
- **`core:domain`** — new `CatalogCount(active, visible)`; `DeviceRepository.getCatalogCount()` and `GetCatalogCountUseCase` return it.
- **`core:data`** — `DeviceRepositoryImpl.getCatalogCount`.
- **`core:ui`** — `component/time/TimeOfDay.kt` (identical to `home-stats-fixes`).
- **`core:testing`** — `FakeDeviceRepository`.
- **`features:device-linking`** — `DeviceLinkingUiState`, `DeviceLinkingViewModel`, `DeviceCard`, strings; remove `DeviceFormatters.toShortLocalTime`.
- **Branch:** cut from `feature/devices-live-status` (change 5).
