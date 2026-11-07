## 1. Network (`core:network`)

- [x] 1.1 In `InsightsDtos.kt`, add `otherRequests: List<UnmetSearchGroupDto> = emptyList()` to `UnmetSearchesDto` and document it (Fase 32)
- [x] 1.2 Test that a response with and without `otherRequests` decodes (empty when absent)

## 2. Domain (`core:domain`)

- [x] 2.1 In `StoreSuggestions.kt`, add `example: String? = null` to `MissingProductSuggestion` (fix its KDoc: `name` is the canonical term) and `otherRequests: List<MissingProductSuggestion> = emptyList()` to `StoreSuggestions`

## 3. Data (`core:data`)

- [x] 3.1 In `StoreInsightsMappers.kt`, pick the example per design decision 1 (with a private `normalized()` helper) and add `UnmetSearchesDto.toOtherRequestSuggestions()` with the same mapping and de-duplication
- [x] 3.2 In `StoreInsightsRepositoryImpl.getSuggestions()`, build `StoreSuggestions` with `otherRequests`
- [x] 3.3 Mapper tests: example kept when it differs (`una lámpara solar` / `lampara solar`), dropped when equal ignoring case/accents/punctuation (`Amoladora.` / `amoladora`), most frequent chosen, blank ignored, none → `null`; `otherRequests` mapped and de-duplicated
- [x] 3.4 Repository test: `otherRequests` reach `StoreSuggestions`

## 4. Testing fixtures (`core:testing`)

- [x] 4.1 `missingProductSuggestion(...)` accepts `example`; `storeSuggestions(...)` accepts `otherRequests`

## 5. Home (`features:home`)

- [x] 5.1 `HomeUiState.otherRequests: ImmutableList<MissingProductSuggestion>`; `HomeViewModel.applyOutcome` fills it with the same keep-on-failure rule as the other rows
- [x] 5.2 `MissingProductCard`: example line under the term (design decision 4); `onAdd: (() -> Unit)?` with no button when `null`; update KDoc
- [x] 5.3 `SuggestionSection` takes a `Painter`; add `OtherRequestsSection` ("También te piden", `Icons.Outlined.ChatBubbleOutline`, omitted when empty)
- [x] 5.4 `HomeContent`: render the new row after "Productos no encontrados"; show the heading when any of the three rows has items
- [x] 5.5 Strings es + en: `home_other_requests_title` (`También te piden` / `Also asked for`), `home_missing_example` (`“%1$s”`)
- [x] 5.6 Preview data with examples and other requests; previews light/dark
- [x] 5.7 ViewModel tests: `otherRequests` loaded; kept on a failed refresh

## 6. Verification

- [x] 6.1 Run `./gradlew :core:network:testDebugUnitTest :core:data:testDebugUnitTest :features:home:testDebugUnitTest lintDebug assembleDebug`; all pass

## 7. Manual — QA

- [ ] 7.1 Manual — Search "quiero una lámpara solar" on the kiosk of a lighting store: Inicio shows "Lampara solar" with “quiero una lámpara solar” under it
- [ ] 7.2 Manual — Search something outside the line of business ("pintura"): it shows under "También te piden" without a button
