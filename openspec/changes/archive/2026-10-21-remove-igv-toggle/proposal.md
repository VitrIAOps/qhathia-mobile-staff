## Why

The "Precios con IGV incluido" toggle in Configuración does nothing (triage BUG-012, `docs/handoff/android-pendientes.md` §6). The flag was a declaration, not a calculation: its only effect was choosing between two lines of text in the quote PDF. Turning it off never moved a price, and merchants reported it as broken — rightly, because a control with no observable effect reads as broken.

The backend has since retired it (`docs/android/api-contract.md` §27, in production since 2026-10-18): in Peru consumer prices include IGV by law, every store loaded them that way, so `pricesIncludeIgv` and `igvRate` no longer exist in `GET/PUT /api/v1/stores/{id}`. Today the app sends a field the server ignores and reads one that never arrives, so the toggle sits on "on" forever without doing anything. Nothing breaks meanwhile — this is cleanup, not an emergency — but the control is a lie until it is gone.

## What Changes

- **Remove** the IGV toggle from Configuración. The card keeps only the Idioma selector.
- **Remove** the inline "No pudimos cargar la configuración de la tienda" + retry row. It existed only to stand in for the toggle when the store failed to load; the language selector never depended on the store.
- Keep reading the store in the background so the existing best-effort language mirror (re-sending the device language when the store disagrees) keeps working, silently.
- **Remove** `pricesIncludeIgv` / `igvRate` from every layer: `StoreSettingsDto`, the `pricesIncludeIgv` parameter of `StoreApiService.updateStore`, the mapper, `StoreSettings`, `StoreSettingsRepository.updatePricesIncludeIgv`, `SetPricesIncludeIgvUseCase`, the ViewModel's IGV state/event/effect, fakes, fixtures, tests and the `settings_igv*` / `settings_store_error` strings.
- The Mi perfil hub's Configuración subtitle changes from "Idioma y precios" to "Idioma de la app" ("App language"), since prices are no longer configured there.

## Capabilities

### New Capabilities

_None._

### Modified Capabilities

- `app-settings`: the Configuración layout loses the IGV toggle; the "IGV display setting" requirement is **removed**.
- `merchant-profile`: the Configuración row subtitle no longer mentions prices.

## Impact

- **`core:network`** — `StoreSettingsDto` loses two fields; `updateStore` loses a parameter. The JSON configuration already ignores unknown keys, so a backend that still sent them would not break.
- **`core:domain`** — `StoreSettings` loses `pricesIncludeIgv` and `igvRate`; `StoreSettingsRepository` loses `updatePricesIncludeIgv`; `SetPricesIncludeIgvUseCase` is deleted.
- **`core:data`** — mapper and `StoreSettingsRepositoryImpl`; their tests.
- **`core:testing`** — `FakeStoreSettingsRepository` (IGV recording) and `TestFixtures.storeSettings(...)`.
- **`features:profile`** — `AppSettingsViewModel`, `AppSettingsScreen`, strings (es/en), `AppSettingsViewModelTest`.
- **No API change on our side**: we stop sending and reading fields the server already dropped.
- **Archive order:** the `app-settings` and `merchant-profile` main specs are created by the archive of `add-merchant-profile-settings`, on the `fix/inventory-snackbar-and-notification-badge` branch. Archive this change after that branch is merged.
- **Not in scope:** any IGV breakdown in the quote PDF (backend-owned) or a future "net prices" mode, which the contract says would be derived on read, not configured here.
