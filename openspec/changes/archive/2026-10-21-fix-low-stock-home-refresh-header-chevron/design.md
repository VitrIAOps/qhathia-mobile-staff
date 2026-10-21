## Context

Three presentation-only defects, each in one feature module. No domain, data or network code changes, and no module-graph changes.

- **Order detail (`features:order-requests`).** `OrderItemRow` ends with `if (stock != null && stock <= LOW_STOCK_THRESHOLD) LowStockWarning()`, with `LOW_STOCK_THRESHOLD = 5`. `LowStockWarning` is a full-width row in `QhathiaColors.warningContainer` with an info icon and `order_item_low_stock`. The same app already treats `0` separately in Inicio (`RestockProductCard.RemainingStockChip` → `home_out_of_stock`, "Sin existencias"), and Inventory's row does too (`inventory_out_of_stock`).
- **Inicio (`features:home`).** `HomeViewModel.observeLoads()` runs `combine(selectedPeriod, reloadTrigger)` → `flatMapLatest { load(period) }` from `init`. Only `HomeUiEvent.Refresh` (bumps `reloadTrigger`) and `SelectPeriod` re-run it. A reload sets `isLoading = true`, but the UI only shows the full loading state when `isInitialLoading` (`isLoading && summary == null`), and `applyOutcome` keeps previous data on failure — so a reload is already non-destructive.
- **Headers.** `OrderRequestsHeader` and `InventoryHeader` each start their title row with `IconButton(onClick = {}) { Icon(KeyboardArrowLeft, contentDescription = stringResource(*_back_cd)) }`. The shared `QhathiaGradientTopBar` already has the right pattern for roots: a null `onBack` draws no button. Inicio's header has no chevron at all.

## Goals / Non-Goals

**Goals:**
- `0` reads as a stock-out in the order dialog, with the same words the rest of the app uses.
- Inicio is current every time the merchant looks at it, with no double load on first entry and no blanking.
- Root tab headers stop advertising a back action.

**Non-Goals:**
- Comparing stock against the ordered quantity in the dialog.
- Pull-to-refresh, polling or any refresh trigger other than returning to the screen.
- Changing the chevrons of pushed screens (detail, register, statistics, profile sub-screens), which navigate correctly.
- Moving `LOW_STOCK_THRESHOLD` into a shared constant.

## Decisions

### 1. A separate out-of-stock warning, not a parameterised one

Replace the single condition with:

```kotlin
when {
    stock == null -> Unit                       // consult mode: nothing tracked
    stock <= 0 -> OutOfStockWarning()
    stock <= LOW_STOCK_THRESHOLD -> LowStockWarning()
}
```

`<= 0` rather than `== 0` so a negative value (should never arrive, but is not rejected by the DTO) is not shown as "a few left". `OutOfStockWarning` mirrors `LowStockWarning`'s layout, using `MaterialTheme.colorScheme.errorContainer` / `onErrorContainer` and the shared `ic_alert` drawable from `core:ui` (the one `RestockProductCard` uses), so the two warnings differ in colour and icon, not only in text. New string `order_item_out_of_stock`: "Sin existencias" / "Out of stock" — the same wording as Inicio and Inventario. Owning the string in this module keeps feature modules independent (no feature→feature dependency).

### 2. Inicio refreshes on `ON_START`, skipping the first one

`HomeScreen` adds `LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.onEvent(HomeUiEvent.ScreenStarted) }`. Inside a `NavHost` destination, `LocalLifecycleOwner` is the back-stack entry, whose lifecycle drops below `STARTED` when another tab or a pushed screen covers it and when the app goes to the background — exactly the three "returns" in the spec.

`ON_START` is also delivered when the screen first appears, while `init` has already started the first load. The ViewModel therefore ignores the first `ScreenStarted` and turns every later one into the same thing `Refresh` does (bump `reloadTrigger`). The flag lives in the ViewModel, so if Navigation restores the tab with a new ViewModel, `init` loads and the new instance ignores its own first `ScreenStarted` — still one load.

Alternatives considered:
- *Drop the load from `init` and let `ON_START` drive everything:* rejected — the ViewModel would show nothing in tests or previews that never send the event, and it couples the first load to the UI lifecycle.
- *Reuse `Refresh` and accept a duplicate first load:* rejected — `flatMapLatest` would cancel the first request mid-flight, which works but doubles the traffic on every app start for no reason.

`flatMapLatest` already cancels an in-flight load when a new one starts, so quick back-and-forth between tabs cannot stack requests.

### 3. Remove the chevrons rather than hiding them

Delete the `IconButton` and its `Icon` from both headers, the `order_requests_back_cd` and `inventory_back_cd` strings (es/en), and the now unused imports. No spacer is added: the header already has `Dimens.SpaceMd` horizontal padding, which is where Inicio's title starts, so the three root tabs line up. The previous code comment calling the chevron part of "the DS header used across the app's top-level tabs" is removed with it; Inicio is evidence that the DS root header has no chevron.

## Risks / Trade-offs

- **More requests to the dashboard API** → one summary + one suggestions + one count per return to Inicio. These are the same three requests as a manual refresh, cancelled by `flatMapLatest` if the merchant leaves before they finish. Accepted; the triage explicitly prefers fresh data.
- **A failed background refresh shows the error banner** → existing behaviour for any refresh (data stays, banner with retry). Acceptable, and arguably correct: the data on screen may be stale.
- **Visual shift of the titles** by the width of the removed button (48 dp) → intended alignment with Inicio; to be checked against Figma in the manual pass. If Figma keeps the chevron space, a fixed `Spacer` is a one-line follow-up.
