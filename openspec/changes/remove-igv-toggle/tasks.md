## 1. Transport and data

- [x] 1.1 `core:network` — remove `pricesIncludeIgv` and `igvRate` from `StoreSettingsDto` and update its KDoc (the display-settings slice is now just the language)
- [x] 1.2 `core:network` — remove the `pricesIncludeIgv` parameter and its `put` from `StoreApiService.updateStore`
- [x] 1.3 `core:data` — `StoreSettingsDto.toDomain()` maps `id` and `language` only; remove `updatePricesIncludeIgv` from `StoreSettingsRepositoryImpl`
- [x] 1.4 `core:data` tests — drop the `igvRate` fallback assertion in `ProfileMappersTest` and the IGV update in `ProfileRepositoriesTest`; keep the language cases

## 2. Domain and testing

- [x] 2.1 `core:domain` — remove `pricesIncludeIgv` / `igvRate` from `StoreSettings` (and the now unused `BigDecimal` import), `updatePricesIncludeIgv` from `StoreSettingsRepository`, and delete `SetPricesIncludeIgvUseCase`; fix the KDoc of `GetStoreSettingsUseCase` ("language and IGV" → language)
- [x] 2.2 `core:testing` — remove `igvUpdates` and `updatePricesIncludeIgv` from `FakeStoreSettingsRepository`, and the IGV parameters from `TestFixtures.storeSettings(...)`
- [x] 2.3 `./gradlew :core:network:testDebugUnitTest :core:data:testDebugUnitTest :core:domain:test`

## 3. Configuración

- [x] 3.1 `AppSettingsViewModel` — remove `SetPricesIncludeIgvUseCase`, `ToggleIgv`, `RetryStore`, `AppSettingsUiEffect` and the effect flow, and `toggleIgv()`; reduce `AppSettingsUiState` to `language`; keep the store load + language reconciliation private and silent on failure (design Decision 2); update the KDoc
- [x] 3.2 `AppSettingsScreen` — remove `IgvSetting`, the divider before it, the effect collection and the `SnackbarHost`; the card holds only the language selector; update the KDoc and the previews (drop `PreviewStore` and the store/IGV preview states)
- [x] 3.3 Strings (es/en) — remove `settings_igv`, `settings_igv_subtitle`, `settings_igv_read_only`, `settings_igv_save_error`, `settings_store_error`; change `menu_settings_subtitle` to "Idioma de la app" / "App language"
- [x] 3.4 `AppSettingsViewModelTest` — delete the two IGV tests and the IGV half of the seller test; keep the language-mirror tests; adapt "a store that fails to load leaves the language usable" to assert no error is exposed and selection still works
- [x] 3.5 Grep the repo (code, resources, tests) for `igv` / `Igv` / `IGV` and `settings_store_error`: only docs and archived OpenSpec changes may still mention it
- [x] 3.6 `./gradlew :features:profile:testDebugUnitTest :features:profile:lintDebug`

## 4. Docs and verification

- [x] 4.1 Mark §6 as done in `docs/handoff/android-pendientes.md`, including the `pricesIncludeIgv` / `igvRate` row of "Resumen de campos nuevos"
- [x] 4.2 Full build: `./gradlew testDebugUnitTest lintDebug assembleDebug`
- [ ] 4.3 Manual — as OWNER and as SELLER: Configuración shows only Idioma, switching language still applies the app locale; Mi perfil's Configuración row reads "Idioma de la app"; in airplane mode Configuración shows no error
