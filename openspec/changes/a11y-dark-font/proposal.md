## Why

Two findings from the 2026-11-03 QA regression (`docs/handoff/android-regresion-2026-11-03.md` §3.3) were left for last because they touch shared components every branch used; with changes 0–11 in `develop`, they can now be done in one place:

- **F-84 — dark theme and large fonts.** In dark mode the inactive bottom-bar tabs and "Ver más" are `#1C4466` on `#0E1721`, nearly invisible (S:EXP-015). With font scale 2.0 the bottom bar reads "InventariSolicitud" and the period pills "Mensu", "Seman", "Por" (S:EXP-016). The plan also lists the inventory FAB covering the last row's ⋮, the list not returning to the top on a new category, and the "vs semana" pills being cut.
- **F-92 — accessibility.** The discount row exposes no on/off state to every tool; Precio, Stock and Tags are text fields with no name; the inventory thumbnails all say "Imagen del producto" and the photo strip says nothing; Solicitudes' chips (and Inicio's period pills) are clickable text with a `selected` flag but no tab role (S:EXP-010, S:INV-080, S:REG-098, S:DET-090, S:SOL-090).

## What Changes

- **Readable in dark mode.** A dark-only inactive colour (`#8FB3D9`, ≥ 4.5:1 on the dark surface) for the bottom-bar tabs and "Ver más". Light mode is unchanged.
- **Labels that fit at any font size.** Bottom-bar labels, Inicio's period pills, the segmented controls of Resumen de ventas and the "vs …" pills shrink to fit instead of being cut; at font scale 1.5 or more the period pills use short labels ("Mes", "Sem.", "Día", "Hora").
- **Inventario.** The list leaves room under its last row for the floating button, and goes back to the top when the category or availability filter changes.
- **Screen readers.**
  - The discount row announces "Activado" / "Desactivado".
  - Form fields are named after their label ("Precio", "Stock", "Tags"…) in Detalle and Registro.
  - Inventory thumbnails say "Foto de <producto>" (or "Sin foto"), the photo strip "Foto 1 de 3, principal".
  - Solicitudes' chips and Inicio's period pills are tabs in a tab group.

## Capabilities

### New Capabilities

- `accessibility-display`: legibility in dark mode and at large font sizes, and what screen readers announce for the shared controls.

### Modified Capabilities

_None._ The screens' own requirements do not change; the new capability states the display and screen-reader rules once.

## Impact

- **`core:ui`** — `Color.kt` / `Theme.kt` (dark `tertiary`), `QhathiaBottomBar`, `QhathiaSegmentedControl`, `QhathiaDiscountToggleRow`, `QhathiaTextField(label)`, `QhathiaPhotoStrip`, `SalesPeriodResources` (short labels), strings.
- **`features:home`** — `PeriodTabs` (auto-size, short labels, tab role).
- **`features:sales-statistics`** — `FiltersPanel` (short labels), `ActivityTab` (pill auto-size).
- **`features:product-inventory`** — `InventoryContent` (bottom padding), `InventoryScreen` (scroll to top), `ProductRow` (thumbnail description).
- **`features:product-detail`**, **`features:product-register`** — `LabeledField` passes its label to the field.
- **`features:order-requests`** — `OrderFilterChips` (tab role, group).
- **Branch:** cut from `develop`, which holds changes 0–11.
