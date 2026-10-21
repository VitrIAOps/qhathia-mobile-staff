## Context

The IGV toggle spans every layer, top to bottom:

| Layer | Piece |
|---|---|
| `core:network` | `StoreSettingsDto.pricesIncludeIgv` (default `true`), `.igvRate` (default `0.18`); `StoreApiService.updateStore(…, pricesIncludeIgv)` |
| `core:data` | `StoreSettingsDto.toDomain()` maps both (with a `0.18` fallback); `StoreSettingsRepositoryImpl.updatePricesIncludeIgv` |
| `core:domain` | `StoreSettings.pricesIncludeIgv` / `.igvRate`; `StoreSettingsRepository.updatePricesIncludeIgv`; `SetPricesIncludeIgvUseCase` |
| `core:testing` | `FakeStoreSettingsRepository.igvUpdates` / `updatePricesIncludeIgv`; `TestFixtures.storeSettings(pricesIncludeIgv, igvRate)` |
| `features:profile` | `AppSettingsUiState.isIgvSaving`, `ToggleIgv`, `ShowIgvSaveError`, `toggleIgv()`; `IgvSetting` composable and the snackbar host; strings `settings_igv`, `_subtitle`, `_read_only`, `_save_error`, `settings_store_error` |

`StoreSettings` has exactly one other consumer: `AppSettingsViewModel`'s language mirror. On open, it loads the store and, for OWNER/ADMIN, re-sends the device language if the store disagrees (a previous mirror may have failed). `SetAppLanguageUseCase` mirrors on every selection. The store-load error row in the UI (`settings_store_error` + retry) only ever replaced the IGV toggle — the language selector reads the device value and never waited on the store.

The backend dropped both fields (`api-contract.md` §27). The JSON configuration ignores unknown keys, and the DTO has defaults, which is why the app has kept working while the toggle silently stuck on "on".

## Goals / Non-Goals

**Goals:**
- No IGV control, state, call, field or string left anywhere in the app.
- Configuración keeps its language behaviour exactly, including the best-effort store mirror.
- Configuración never shows an error about something the merchant cannot see or act on.

**Non-Goals:**
- Changing how the language is stored, applied or mirrored.
- Removing `StoreSettings` / `GetStoreSettingsUseCase` (still needed for the mirror).
- Anything about the PDF's tax line, which is backend-owned.

## Decisions

### 1. Delete through every layer, not just the UI

Hiding the toggle and leaving the field would keep a `pricesIncludeIgv = true` default in the DTO that pretends the server said something it no longer says, and a repository method that sends a field the server ignores. Both are the "data that lies" pattern from the triage. Everything listed in Context is removed; `StoreSettings` becomes `(storeId, language)` and `StoreSettingsDto` becomes `(id, language)`. Nothing else reads them, so no consumer needs a migration.

### 2. The store load becomes silent

With the toggle gone, the only reason to load the store is the language mirror, which is best-effort by design. So:

- `AppSettingsUiState` keeps only `language`. `store`, `canEditStore` and `isIgvSaving` leave the UI state; the role check and the loaded store become private to the ViewModel's reconciliation.
- `AppSettingsUiEvent.RetryStore`, `AppSettingsUiEffect` (its only member was `ShowIgvSaveError`), the effect flow, the screen's `SnackbarHost` and the error row are removed.
- A failed store load is simply ignored; the mirror runs again next time Configuración opens, as it does today.

Alternative considered: keep the error row "for the language mirror". Rejected — the merchant cannot see the mirror, the device language already applied, and an error with a retry for an invisible background sync would be its own control that does nothing visible.

### 3. Hub subtitle: "Idioma de la app"

"Idioma y precios" advertised the IGV setting. The new subtitle "Idioma de la app" / "App language" says what is left and why it is device-level. It stays a static string (no live value), as before.

## Risks / Trade-offs

- **A future "net prices" mode** → the contract keeps `stores.prices_include_igv` in the database and says such a mode would derive prices on read. If it ever comes back, it returns as a real feature with its own change; nothing here makes that harder.
- **Old builds in the field** keep sending `pricesIncludeIgv` → the backend answers `200` (it ignores the property, covered by a backend test). No coordination needed.
- **Silent mirror failures** → unchanged from today in effect (the error row never retried the mirror itself, only the load); the next open retries.
