## Why

The 2026-10-21 emulator pass (recorded in `fix-low-stock-home-refresh-header-chevron` task 4.5) found two header defects, one of them introduced by that change:

- **The Inventario title is misaligned.** Removing the back chevron left the title's `padding(horizontal = SpaceXs)`, which only existed to separate it from the chevron. The title now starts ~4 dp right of Inicio's and Solicitudes', contradicting the requirement that it start at the header's horizontal padding.
- **Three bells do nothing.** Inventario, Detalle de producto and Registro de producto draw a bell as `IconButton(onClick = {})` with the description "Notificaciones": it ripples, does nothing, and TalkBack announces an action that does not exist — the same "control that lies" pattern as the chevron. The bells on Inicio and Mi perfil already open Solicitudes; the product decision (2026-10-21) is that these do the same.

## What Changes

- Remove the leftover horizontal padding from the Inventario title so it lines up with Inicio and Solicitudes.
- The bells on Inventario, Detalle de producto and Registro de producto open the Solicitudes tab, through the same tab switch the Inicio and Mi perfil bells use, and announce themselves as "Ver solicitudes".
- No badge on these bells (Mi perfil's has none either); the Solicitudes tab already shows the pending count.

## Capabilities

### New Capabilities

_None._

### Modified Capabilities

- `product-inventory`: the Inventario header bell opens Solicitudes.
- `product-detail`: the Detalle de producto header bell opens Solicitudes.
- `product-registration`: the Registro de producto header bell opens Solicitudes.

## Impact

- **`features:product-inventory`** — `InventoryHeader` (padding, bell action, content description), `InventoryScreen`/`InventoryContent` and `inventoryGraph` gain an `onNavigateToOrderRequests` callback; strings (es/en).
- **`features:product-detail`**, **`features:product-register`** — the same for their headers, screens and graphs.
- **`app`** — `MainShell` passes `switchTab(ORDER_REQUESTS_ROUTE)` to the three graphs.
- **No domain, data or network changes.** Feature modules still do not depend on each other: the navigation is a callback the shell provides.
- **Archive order:** this change adds to the `product-inventory` requirements introduced by `fix-low-stock-home-refresh-header-chevron`; archive that change first.
- **Not in scope:** a pending-count badge on these bells, and the dark-theme contrast of the headers (planned separately).
