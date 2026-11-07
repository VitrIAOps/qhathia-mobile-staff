## Context

`GET /api/v1/insights/unmet-searches` (vo) returns `groups` (in the store's line of business) and, since Fase 32, `otherRequests` (same shape, outside it). Each group has `term`, `personCount`, `searchCount`, `examples[] {rawText, count}` and `storeFit`. The app maps only `groups` → `MissingProductSuggestion(id = term, name = Term, personCount)` and drops `examples` and `otherRequests`. Suggestions are loaded together with restock in `StoreInsightsRepositoryImpl.getSuggestions()` and land in `HomeUiState` through `HomeViewModel.applyOutcome`.

Modules involved — all existing:

```
features:home → core:domain ← core:data → core:network
                core:testing (fixtures)
```

## Goals / Non-Goals

**Goals:**
- Show the shopper's own wording when it adds information over the canonical term.
- Make `otherRequests` visible as information, clearly separate from what the store could stock.

**Non-Goals:**
- Making `Agregar` do anything (still inert by spec).
- Showing `storeFit` or `searchCount`.
- Re-canonicalising raw text on the client (the backend owns `term`).

## Decisions

### 1. One `example: String?` chosen in the data layer

The mapper picks the example and the domain carries a single nullable string, not the list: the UI shows one line, and deciding "does it add anything" is a data rule, not a rendering one.

```kotlin
val example = examples
    .filter { it.rawText.isNotBlank() }
    .maxByOrNull { it.count }          // first on a tie: the backend already orders by count
    ?.rawText?.trim()
    ?.takeUnless { it.normalized() == canonical.normalized() }
```

`normalized()` = lowercase (es-PE), NFD with combining marks removed, punctuation and repeated spaces collapsed. So `Amoladora.` under `amoladora` is dropped, `una lámpara solar` under `lampara solar` is kept.

*Alternative:* carry `examples: List<String>` (as the handoff suggested) and decide in the card. Moves a text rule into Compose and makes it harder to test; nothing needs the full list.

### 2. `otherRequests` reuses the domain type and the card

Same shape, same mapper (`toMissingProductSuggestion`), de-duplicated by id like `groups`. `StoreSuggestions` gains `otherRequests` with a default so existing call sites compile. It fails and refreshes together with the other two rows — it is the same request.

`MissingProductCard` takes `onAdd: (() -> Unit)?`; `null` draws no button. A separate card composable would duplicate the count block and the text layout for one missing button.

### 3. Row placement and icon

"También te piden" goes right after "Productos no encontrados" (the two are halves of one response) and before "Productos para reponer". `SuggestionSection` currently takes a `@DrawableRes`; it is changed to take a `Painter` so the new row can use `Icons.Outlined.ChatBubbleOutline` from material-icons-extended (already a dependency of `features:home`) without adding a drawable to `core:ui`.

### 4. Example line style

`bodySmall`, `onSurfaceVariant`, max 2 lines with ellipsis, under the term in the same text column; string `home_missing_example` = `“%1$s”`. Shown in both rows.

## Risks / Trade-offs

- **[Card heights differ]** Cards with an example are taller, and "También te piden" cards have no button. → `LazyRow` aligns them at the top; acceptable, cards within a row are independent.
- **[A long example]** Raw text can be a sentence. → Two lines with ellipsis.
- **[Empty `otherRequests` on old data]** Events before Fase 32 count as in-line. → The row simply does not show until there is such demand.

## Migration Plan

Client-only, additive (`otherRequests` defaults to empty, so an older backend just shows no new row). Rollback is reverting the change.

## Open Questions

_None._
