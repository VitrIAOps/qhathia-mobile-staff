## Why

After registering a product the merchant lands back on the inventory list with a "Producto registrado correctamente" snackbar, but the list keeps its previous scroll position and the new product can be anywhere in it. To check what was just created the merchant has to hunt for the row by hand. A direct shortcut from the confirmation closes that gap.

## What Changes

- The registration success snackbar on the Inventory screen gains a text action labelled `Ver producto`.
- The "registered but the discount could not be applied" snackbar gains the same action, since the product exists in that case too.
- Tapping `Ver producto` locates the newly registered product in the loaded inventory list and scrolls the list to its row.
- If the product is hidden by the name search, the search is cleared so the row can be shown.
- If the active category/availability filters exclude it from the loaded list, the filters (and search) are cleared, the list is re-queried and it then scrolls to the product.
- If the action is tapped while the post-registration refresh is still in flight, the scroll happens as soon as the refreshed list arrives.
- The registration flow hands the new product's id back to the Inventory screen alongside the existing success and discount-failed results (today only a result flag crosses the boundary).
- The snackbars with the action stay visible longer than a plain confirmation so the merchant has time to tap, and still dismiss on their own.
- The plan-limit and product-updated messages are unchanged and carry no action.

## Capabilities

### New Capabilities

_None._

### Modified Capabilities

- `product-inventory`: the "Inventory refresh after successful registration" and "Inventory feedback when a registration's discount fails" requirements change — both snackbars now carry a `Ver producto` action that scrolls the list to the registered product, with defined behaviour when the product is hidden by search, excluded by filters, or the refresh has not finished.
- `product-registration`: the "Registration feedback and navigation" requirement changes — whenever a product was created (with or without its discount) the app returns to the inventory list identifying the newly registered product, so the inventory can locate it.

## Impact

- `:features:product-register` — `ProductRegisterUiEffect.NavigateBackWithSuccess` and `NavigateBackWithDiscountFailed` carry the product id; the `onRegistered` / `onDiscountFailed` callbacks of `ProductRegisterScreen` / `productRegisterGraph` take the id; ViewModel test updated.
- `:features:product-inventory` — new result key for the registered product id in `InventoryNavigation.kt`; `InventoryScreen` shows the snackbar action and owns the list scroll state; `InventoryContent` accepts a hoisted `LazyListState`; new `InventoryUiEvent` / `InventoryUiEffect` entries and ViewModel logic; new strings; ViewModel tests.
- `:app` — `MainShell.kt` writes the product id into the inventory entry's `SavedStateHandle` in `onRegistered` and `onDiscountFailed`.
- No new Gradle modules, no API/DTO changes (the register endpoint already returns the id, and `RegisterProductUseCase` already exposes it), no new dependencies.
