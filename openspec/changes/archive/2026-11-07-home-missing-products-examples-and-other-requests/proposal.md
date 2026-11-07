## Why

"Productos no encontrados" now names each card after the backend's canonical term (`44b5c30`), which is clean but loses how shoppers actually asked ("una lámpara solar" becomes "Lampara solar", without the accent). And since Fase 32 the backend moves demand from outside the store's line of business ("pintura" in a lighting store) out of `groups` into a new `otherRequests` list that the app ignores, so that demand is invisible. Both are listed as optional Staff items in `android-pendientes-v2.md` (§3.2 step 3, Fase 32) and the contract is in `api-contract.md` §17, §32 and §33.

## What Changes

- Each "Productos no encontrados" card shows, under the term, the most frequent way shoppers asked for it, in quotes and muted (`“una lámpara solar”`) — only when it says something the term does not (it is hidden when it equals the term ignoring case, accents and punctuation, or when there are no examples).
- A new row **"También te piden"** right below "Productos no encontrados" shows `otherRequests` with the same card (people count, term, example line) but **without** the `Agregar` button. It is omitted when empty, like the other rows.
- The "Sugerencias para vender más" heading also appears when only "También te piden" has items.
- The requirement text is brought up to date: the card is named after the canonical term, not the raw text (already the behaviour since `44b5c30`, but the spec still says otherwise).

## Capabilities

### New Capabilities

_None._

### Modified Capabilities

- `home-summary`: "Suggest products that could increase sales" — card named after the canonical term, optional example line, and a third row "También te piden" from `otherRequests` without an action button.

## Impact

- **`core:network`** — `InsightsDtos.kt`: `UnmetSearchesDto.otherRequests: List<UnmetSearchGroupDto> = emptyList()`. Test.
- **`core:domain`** — `StoreSuggestions.kt`: `MissingProductSuggestion.example: String? = null`; `StoreSuggestions.otherRequests: List<MissingProductSuggestion> = emptyList()`. KDoc of `MissingProductSuggestion` corrected (name is the canonical term).
- **`core:data`** — `StoreInsightsMappers.kt`: picks the example; maps `otherRequests`; `StoreInsightsRepositoryImpl` builds `StoreSuggestions` with both. Tests in `StoreInsightsMappersTest` / repository test.
- **`core:testing`** — fixtures accept the new fields.
- **`features:home`** — `HomeUiState.otherRequests`, `HomeViewModel.applyOutcome`, `MissingProductCard` (example line, optional action), `SuggestionSection` (new row; icon as `Painter`), `HomeContent`, preview data, strings (es + en). ViewModel test.
- **Not affected:** the API (no new request: `otherRequests` comes in the same response), `Productos para reponer`, other features.
