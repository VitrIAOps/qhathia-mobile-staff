## Why

Three pieces of copy found in the 2026-10-21 emulator pass read wrong:

- **"1 productos".** The Inventario count is a plain string, `%1$d productos`, so a search that leaves one product reads "1 productos". It is the only count string in the app that is not a plural.
- **"Tu cuenta y los datos de tu tienda".** The Mi perfil row for Datos personales promises store data the screen does not show: it edits the merchant's name and phone and shows the email.
- **"Generados por tus 0 productos principales".** When nothing sold in the period, the `Lo que más vende` card still draws its summary box — `S/ 0`, an upward-trend icon and a caption counting zero products — above the empty message that already says there are no sales. The `Lo que necesita atención` card does the same when nothing is stalled ("Inmovilizados en los 0 productos sin ventas").

## What Changes

- The Inventario count becomes a plural: "1 producto" / "N productos" (en: "1 product" / "N products").
- On the Ranking tab, a card with no rows shows its title and its empty message only: no total, no trend icon, no caption.
- The Datos personales row subtitle in Mi perfil reads "Nombre, correo y teléfono" (en: "Name, email and phone").

## Capabilities

### New Capabilities

_None._

### Modified Capabilities

- `product-inventory`: the product count label uses the singular for one product.
- `sales-statistics`: both Ranking cards hide their summary when they have no rows.
- `merchant-profile`: the Datos personales row subtitle describes what the screen holds.

## Impact

- **`features:product-inventory`** — `ProductCountAndFilterBar` uses `pluralStringResource`; `inventory_product_count` becomes a `<plurals>` (es/en).
- **`features:sales-statistics`** — `RankingCard` draws the summary box only when it has rows; previews.
- **`features:profile`** — `menu_personal_subtitle` (es/en).
- **No domain, data or network changes.**
- **Not in scope:** the other findings of the same pass (order detail actions, dark theme, photo after registration), planned as separate changes.
