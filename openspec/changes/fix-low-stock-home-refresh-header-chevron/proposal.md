## Why

Three small defects from the 2026-10-18 triage (`docs/handoff/android-pendientes.md` §4.2, §4.3, §4.4) where the screen tells the merchant something that is not true:

- **A sold-out item reads "Quedan pocos disponibles"** (BUG-017). The order detail dialog warns when `productStock <= 5`, and `0 <= 5`, so a stock-out takes the low-stock path. Softening "agotado" into "quedan pocos" is what led a merchant to confirm an order that could not be shipped. Inicio already solved this (`RestockProductCard`: "Solo quedan 0" would soften a stock-out) — the order dialog never got the same branch.
- **Inicio shows stale data after coming back** (BUG-009). Inicio is a bottom-nav tab whose ViewModel survives navigation, and it only reloads on an explicit refresh or a period change. A product restocked from Inventory stays in "Productos para reponer" as "Sin existencias" until the merchant pulls to refresh. The backend has no cache; the app simply never asks again.
- **A back chevron that goes nowhere** (BUG-011). The Solicitudes and Inventario headers draw an `IconButton(onClick = {})` with a `Volver` content description. Both are root tabs, so there is nothing to go back to: the button ripples, does nothing, and TalkBack announces a back action that does not exist.

## What Changes

- **Order detail:** an item whose `productStock` is `0` shows an out-of-stock warning (`Sin existencias`) in the error tones instead of the low-stock one. Stock `1..5` keeps `Quedan pocos disponibles`; unknown stock (consult mode) still shows nothing.
- **Inicio:** the tab re-reads its data every time it comes back on screen (returning from another tab or a pushed screen, or the app returning from the background), without blanking what is already shown. The first appearance does not load twice.
- **Headers:** the decorative back chevron is removed from the Solicitudes and Inventario headers, together with its `*_back_cd` strings. The title aligns to the header's padding, as in Inicio.

## Capabilities

### New Capabilities

_None._

### Modified Capabilities

- `order-requests`: the order detail distinguishes a stock-out from low stock; the Solicitudes header has no back control.
- `home-summary`: Inicio refreshes when it returns to the screen.
- `product-inventory`: the Inventario header has no back control.

## Impact

- **`features:order-requests`** — `OrderDetailDialog` (new out-of-stock branch and composable, preview), `OrderRequestsHeader` (chevron removed), strings (es/en).
- **`features:home`** — `HomeUiEvent` gains a "screen started" event, `HomeViewModel` handles it, `HomeScreen` sends it on `ON_START`; tests.
- **`features:product-inventory`** — `InventoryHeader` (chevron removed), strings (es/en).
- **No API, domain or data changes.** Everything uses fields that already arrive.
- **Not in scope:** warning when the stock is above zero but below the ordered quantity (the backend already rejects that confirm with `STOCK_INSUFFICIENT`), and the back chevrons of pushed screens, which do navigate.
