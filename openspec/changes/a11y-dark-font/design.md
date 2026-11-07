## Context

`Theme.kt` uses `tertiary = QhathiaPalette.NavInactive` (`#1C4466`) in both schemes; in dark mode that is ~1.6:1 on `DarkSurface` (`#0E1721`). `tertiary` is read only by the bottom bar's inactive items and Inicio's "Ver más".

Labels that must stay on one line use `maxLines = 1` with no fallback: `QhathiaBottomBar` items, Inicio's `PeriodTabs`, `QhathiaSegmentedControl` (Resumen de ventas tabs and period filter), and `ActivityTab`'s "vs …" pills. Compose BOM 2026.05 gives `Text(autoSize = TextAutoSize.StepBased(...))`. Font-scaled `sp` sizes stay scaled inside auto-size, so at 2.0 even the minimum can be too wide for "Mensual" in a quarter of the screen.

`InventoryContent`'s list has `contentPadding = PaddingValues(SpaceMd)` and a `SmallFloatingActionButton` over its bottom end. `InventoryScreen` keeps one `LazyListState` across filter changes.

Accessibility today:
- `QhathiaDiscountToggleRow` is `toggleable(role = Role.Switch)`; Compose exposes the role as a role description, which `uiautomator` does not show, and no state description.
- `QhathiaTextField` is placeholder-only and its `BasicTextField` has no semantics name; `LabeledField` (Detalle, Registro) draws the label as a separate `Text`.
- `ProductRow` gives every thumbnail "Imagen del producto"; `QhathiaPhotoStrip.PhotoThumbnail` passes `contentDescription = null`.
- `OrderFilterChips` and `PeriodTabs` use `.semantics { selected }.clickable`, with no role and no group; `QhathiaSegmentedControl` already does it right (`selectable(role = Role.Tab)` in a `selectableGroup`).

## Goals / Non-Goals

**Goals:**
- Every text readable in dark mode and complete at font scale 2.0 on the screens QA covers.
- Every shared control announced with its name, role and state.

**Non-Goals:**
- A landscape layout for Inventario (the fixed header leaves room for one row): it needs a layout redesign, not a fix.
- A full dark palette from design (the DS still has none).
- Instrumented UI tests: the project has none; QA checks with TalkBack and `inspect_screen`.

## Decisions

1. **One dark-only colour.** `QhathiaPalette.DarkNavInactive = #8FB3D9` as `tertiary` in `DarkColors` only. It is ~8:1 on `#0E1721`; light keeps `#1C4466`.

2. **Auto-size, then short labels.** The single-line labels get `autoSize = TextAutoSize.StepBased(minFontSize, maxFontSize = style size)` with `softWrap = false`. For the period pills, which hold the longest words in the narrowest cells, `SalesPeriodUi.shortLabelRes` ("Mes", "Sem.", "Día", "Hora" / "Month", "Wk.", "Day", "Hour") and a composable `SalesPeriodUi.selectorLabel()` that picks it when `LocalDensity.current.fontScale >= 1.5`. Inicio and the statistics period filter use it; the statistics tabs (Resumen / Ranking / Actividad) only need auto-size.
   - *Why both?* Auto-size alone still cuts at 2.0; short labels alone would shrink nothing at 1.3.

3. **Inventory list.** `contentPadding` gains a bottom of `SpaceMd + FabClearance` (the FAB's 40 dp plus its margin, about 84 dp in all) so the last row's ⋮ can scroll above the button. `InventoryScreen` scrolls the list to the top when the selected category or availability changes (a `LaunchedEffect` keyed on both, skipping the first composition so returning to the tab keeps the position).

4. **Screen-reader semantics.**
   - Discount row: `stateDescription` "Activado" / "Desactivado" next to its `Role.Switch`.
   - `QhathiaTextField(label: String? = null)`: when set, the `BasicTextField` gets `semantics { contentDescription = label }`, so the field is "Precio, edit box, 189.90". `LabeledField` in Detalle and Registro passes its label.
   - Inventory thumbnail: the image container is one node, "Foto de <name>" or "Sin foto" when there is none; the image itself is `null`.
   - Photo strip: each thumbnail is "Foto <n> de <total>", plus ", principal" for the first.
   - Tabs: `OrderFilterChip` and `PeriodTab` move to `selectable(selected, role = Role.Tab, onClick)` inside a `selectableGroup()` row, as `QhathiaSegmentedControl` does.
   - *Why `contentDescription` on the field and not a merged label?* The label is drawn outside the field by each screen; naming the editable node directly works whatever the layout.

## Risks / Trade-offs

- [`contentDescription` on an editable node] → some TalkBack versions read it instead of the hint; the value is still read. QA confirms with TalkBack (S:DET-090).
- [Auto-size reflow on font changes] → limited to single-line labels; their containers have fixed weight.
- [Scroll to top surprising the merchant] → only on a filter change, never on return to the tab or after a save.

## Migration Plan

Ships with the app, no data migration. Rollback is reverting the change.

## Open Questions

_None._
