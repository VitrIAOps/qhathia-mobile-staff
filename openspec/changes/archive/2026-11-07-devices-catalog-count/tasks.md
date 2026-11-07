## 1. Catalog count data (`core:network`, `core:domain`, `core:data`, `core:testing`) — F-94

- [x] 1.1 `CatalogCountDto`: `visibleProducts: Int? = null`, `hidden: CatalogHiddenDto? = null` (`outOfStock`, `lowStock`, `consult`, `noActiveCategory`)
- [x] 1.2 `CatalogCount(active, visible)` with `shown` and `hidden` derived; `DeviceRepository.getCatalogCount(): Result<CatalogCount>`, `GetCatalogCountUseCase` (design decision 1)
- [x] 1.3 `DeviceRepositoryImpl.getCatalogCount` maps both (negative values to 0, `visible` capped at `active`); `FakeDeviceRepository.catalogCountResult` becomes `Result<CatalogCount>`, with a call counter
- [x] 1.4 Tests: DTO decodes `visibleProducts` and `hidden`; repository maps a Fase 42 answer and an older one; `CatalogCount` derives `shown` / `hidden`

## 2. Dispositivos (`features:device-linking`) — F-94, F-76

- [x] 2.1 `DeviceLinkingUiState.catalogCount: CatalogCount?`; `DeviceCard` / `CatalogBox`: "<shown> productos" and, when `hidden > 0`, "<hidden> no se muestran (agotados, pocas unidades o a consultar)" (plural `device_catalog_hidden`, `values` / `values-en`); update previews
- [x] 2.2 `DeviceLinkingViewModel.refreshSilently(includeCatalog)`: `ScreenStarted` re-reads the count with the devices; a re-read that finds a `REFRESH_CATALOG` turned `ACKED` re-reads it once; a failed read keeps the number (design decision 2)
- [x] 2.3 Add `core:ui` `component/time/TimeOfDay.kt` identical to `feature/home-stats-fixes`; `DeviceCard` formats `expiresAt` with `rememberShortTimeFormatter()`; remove `DeviceFormatters.kt` (design decision 3)
- [x] 2.4 `DeviceLinkingViewModelTest`: "coming back re-reads the catalog count"; "a catalog refresh turning ACKED re-reads the count"; "a plain poll tick does not read the count"; "a failed count read keeps the number"

## 3. Verification

- [x] 3.1 Run, one at a time: `:core:network:testDebugUnitTest`, `:core:data:testDebugUnitTest`, `:features:device-linking:testDebugUnitTest`, then `testDebugUnitTest`
- [x] 3.2 Run `./gradlew lintDebug` and `./gradlew assembleDebug`

## 4. Manual — QA

- [ ] 4.1 Manual — X:XCA-050: Dispositivos shows "Catálogo visible 46 productos" and "4 no se muestran…", matching `/catalog/count`; create a product, come back to Dispositivos: the number goes up without restarting the app
- [ ] 4.2 Manual — "Actualizar catálogo" → when the card reads "Listo", the count is re-read (`GET /catalog/count` in nginx)
- [ ] 4.3 Manual — S:INI-004 on Dispositivos: phone on 12 h → "podrás reintentar a las 5:04 p. m."; on 24 h → "17:04"
