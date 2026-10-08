## Context

Registration and inventory are separate feature modules that cannot depend on each other, so `:app` (`MainShell.kt`) bridges them: `productRegisterGraph(onRegistered = …, onDiscountFailed = …)` writes a result constant into the inventory back-stack entry's `SavedStateHandle` and pops. `inventoryGraph` observes that key and passes it to `InventoryScreen`, whose `LaunchedEffect(registerResult)` calls `viewModel.refresh()`, shows the snackbar and then clears the key.

Three facts shape this change:

- **The product id is already available but dropped.** `RegisterProductUseCase` returns `Result<String>` (the new id) and `ProductRegisterViewModel.submit()` uses it for the discount, but `NavigateBackWithSuccess` / `NavigateBackWithDiscountFailed` are `data object`s and their callbacks are `() -> Unit`. Only a result flag reaches inventory.
- **The list has no hoisted scroll state.** `ProductListCard` in `InventoryContent.kt` builds a `LazyColumn` with an implicit state, one `items` entry per product keyed by `product.id`, so a product's index in `uiState.visibleProducts` is its lazy-list index.
- **The snackbar is shown while the refresh is still in flight.** `refresh()` launches the load and returns; `showSnackbar` runs immediately after. The action can therefore be tapped before the new product is in `loadedProducts`.

## Goals / Non-Goals

**Goals:**

- A `Ver producto` action on the registration success snackbar and on the discount-failed registration snackbar that scrolls the inventory list to the registered product.
- Always reach the product when it exists: clear the search, and clear the category/availability filters and re-query, when either hides it.
- Deterministic behaviour when the list is not loaded yet or the product cannot be found.
- Keep the lookup logic in the ViewModel (unit-testable); keep only the scroll itself in the UI.

**Non-Goals:**

- No action on the plan-limit or product-updated snackbars.
- No navigation to Product Detail from the action.
- No row highlight/flash after scrolling.
- No change to list ordering, pagination, API contracts or DTOs.
- No new Gradle module.

## Decisions

### 1. Modules involved — no new module

| Module | Change |
|---|---|
| `:features:product-register` | Success and discount-failed effects and callbacks carry the product id |
| `:features:product-inventory` | New result key, snackbar action, ViewModel lookup, hoisted `LazyListState` |
| `:app` | `MainShell.kt` forwards the id between the two |

Both features keep depending only on `core:*`; `:app` remains the only place that knows both, as the Dependency Graph Rules require. `core:domain` / `core:data` / `core:network` are untouched.

### 2. Carry the id as a second `SavedStateHandle` key

Add `INVENTORY_REGISTERED_PRODUCT_ID_KEY` next to the existing constants in `InventoryNavigation.kt`. In `MainShell`, `onRegistered = { productId -> … }` and `onDiscountFailed = { productId -> … }` set the id key **before** the result key, so the id is already present when the result's `StateFlow` emits. `inventoryGraph` reads it and passes `registeredProductId: String?` to `InventoryScreen`; `onRegisterResultConsumed` clears both keys.

On the register side, `NavigateBackWithSuccess` and `NavigateBackWithDiscountFailed` become `data class …(val productId: String)`, and `onRegistered` / `onDiscountFailed` become `(String) -> Unit` through `ProductRegisterScreen` and `productRegisterGraph`.

*Alternatives considered:* encoding the id into the result string (`"success:<id>"`) — avoids a second key but turns the `when (registerResult)` constants into parsing. A typed/parcelable result object — cleaner in isolation, but rewrites a channel that four result kinds share.

### 3. Snackbar action with an explicit duration, shared by both results

The success and discount-failed branches of `InventoryScreen` differ only in the message, so both go through one local helper:

```kotlin
suspend fun showRegisteredSnackbar(@StringRes messageRes: Int) {
    val result = snackbarHostState.showSnackbar(
        message = resources.getString(messageRes),
        actionLabel = registeredProductId?.let { resources.getString(R.string.inventory_register_view_product) },
        duration = SnackbarDuration.Long,
    )
    if (result == SnackbarResult.ActionPerformed && registeredProductId != null) {
        viewModel.onEvent(InventoryUiEvent.ViewRegisteredProductClicked(registeredProductId))
    }
}
```

`duration` must be passed explicitly: Material 3 defaults to `SnackbarDuration.Indefinite` whenever `actionLabel` is non-null, which would leave the snackbar on screen (and the result unconsumed) until tapped. `Long` (~10 s) gives time to tap and still auto-dismisses. When `registeredProductId` is null (defensive), the snackbar is shown without the action.

The M3 `Snackbar` already renders `actionLabel` as a `TextButton`, so the existing `SnackbarHost` call is unchanged and no custom snackbar composable is needed.

### 4. The ViewModel resolves the product; the UI only scrolls

New `InventoryUiEvent.ViewRegisteredProductClicked(productId)` and `InventoryUiEffect.ScrollToProduct(index: Int)`. A private `revealProduct(productId)` in `InventoryViewModel` evaluates, in order:

1. **Load in flight** (`isLoading`) → remember the id in a private `pendingRevealProductId` and return.
2. **In `visibleProducts`** → emit `ScrollToProduct(index)`.
3. **In `loadedProducts` only** (hidden by search) → clear `searchQuery`, set `visibleProducts` to the full loaded list, emit `ScrollToProduct(index in loadedProducts)`. No request.
4. **Not loaded, a category or availability filter is active** → set `selectedCategoryId = null`, `selectedAvailability = null`, `searchQuery = ""`, store the id in `pendingRevealProductId` and call `loadProducts()`.
5. **Not loaded, no filter active** → emit `ShowMessage(R.string.inventory_register_view_product_not_found)`.

`loadProducts()` takes the pending id (clearing the field) in `onSuccess`, after updating state, and calls `revealProduct` with it; in `onFailure` it just drops it (the existing `ShowError(R.string.inventory_error)` is the feedback). Because the field is cleared before re-resolving and case 4 requires an active filter — which the reload it triggers has just removed — the unfiltered reload can only end in cases 2 or 5. There is at most one extra request per tap and no loop.

`pendingRevealProductId` is a plain private field, not `UiState`: nothing renders from it. The effect carries an **index** rather than the id so the UI needs no access to the product list inside its effect collector (which would otherwise capture a stale `uiState`), and so the index computation is covered by ViewModel tests.

*Alternatives considered:*
- *Resolve in the composable* (`uiState.visibleProducts.indexOfFirst` in `InventoryScreen`) — puts the search/filter branching in untested UI code.
- *`scrollToProductId` in `UiState`* — a one-shot command modelled as state needs a "consumed" event back; the standards reserve `UiEffect` for this.
- *Keep the filters and show a message instead of clearing them* — preserves the merchant's filter choice but leaves the action doing nothing visible; rejected in favour of always reaching the product.
- *Switch the category filter to the product's own category* — would keep a narrower list, but the inventory does not know the new product's category without loading it; `Todas` is the one state guaranteed to contain it.

### 5. Hoist `LazyListState` and wait for the list before scrolling

`InventoryScreen` creates `val listState = rememberLazyListState()` and passes it down: `InventoryContent(listState = rememberLazyListState(), …)` → `ProductListCard` → `LazyColumn(state = listState)`. The default parameter keeps the previews and any other caller unchanged.

On `ScrollToProduct`, the effect collector launches (it must not block the collector, which also serves error snackbars):

```kotlin
snapshotFlow { listState.layoutInfo.totalItemsCount }.first { it > effect.index }
listState.animateScrollToItem(effect.index)
```

The wait matters whenever the list content changes together with the effect: the search was just cleared, the unfiltered reload just arrived, or `LoadingState` was on screen and the `LazyColumn` has only just entered composition. State and effect are emitted together but the list recomposes a frame later; without the wait `animateScrollToItem` would clamp to the old item count.

### 6. Strings

In `features/product-inventory/src/main/res/values/strings.xml`:

- `inventory_register_view_product` → `Ver producto`
- `inventory_register_view_product_not_found` → `No se encontró el producto en la lista`

## Risks / Trade-offs

- **[The action discards the merchant's filters]** Case 4 resets category, availability and search without asking. → Accepted by product decision: it only happens on an explicit tap of `Ver producto`, and the `Todas` chip and empty search make the new state obvious.
- **[Result replays if the merchant leaves mid-snackbar]** The key is cleared only after `showSnackbar` returns (clearing earlier would change the `LaunchedEffect` key and cancel the snackbar). Leaving the Inventory tab while it is visible cancels the effect with the key still set, so returning shows it again. This already happens today; `Long` widens the window from ~4 s to ~10 s. → Accepted for this change; the replay is harmless (a refresh and a repeated confirmation, the action still works). A real fix means moving result handling into the ViewModel and is out of scope.
- **[Overlapping loads]** Case 1 defers to whatever load is in flight; if the merchant changes a filter while a reveal is pending, each load's `onSuccess` races and the first to finish resolves the reveal against its own result. → `loadProducts()` has no cancellation today for any path; the worst outcome is a reveal resolved against a filtered list, which then falls into case 4 and reloads unfiltered. Not worsened enough to justify adding job cancellation here.
- **[Product not returned by the backend]** `GET /api/v1/products` is loaded as a single list today, so a fresh product is expected in it. If the backend later paginates, case 5 (message, no scroll) covers it. → No extra handling now.
- **[Effect buffer drops]** `_uiEffect` has `extraBufferCapacity = 1` with `DROP_OLDEST`; a load error emitted at the same instant could displace the scroll effect. → Not reachable in practice: the scroll is emitted from `onSuccess`, the error from `onFailure`.
- **[Longer snackbar covers the FAB area]** The snackbar sits at `BottomCenter` over the list for up to ~10 s. → Acceptable; it is swipe-dismissable and the action dismisses it.
- **[Callback signature changes]** Compile-time only (`ProductRegisterScreen`, `productRegisterGraph`, `MainShell`). → Covered by the build.
