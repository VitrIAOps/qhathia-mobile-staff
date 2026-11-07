## 1. Dark mode (`core:ui`) — F-84

- [x] 1.1 `QhathiaPalette.DarkNavInactive = #8FB3D9`; `DarkColors.tertiary` uses it (design decision 1)

## 2. Large fonts — F-84

- [x] 2.1 `QhathiaBottomBarItem` label: `autoSize = TextAutoSize.StepBased(10.sp, style size)`, `softWrap = false` (design decision 2)
- [x] 2.2 `QhathiaSegmentedControl` label: the same auto-size
- [x] 2.3 `SalesPeriodUi.shortLabelRes` and `@Composable SalesPeriodUi.selectorLabel()` (short at `fontScale >= 1.5`); strings `sales_period_*_short` (`values`, `values-en`)
- [x] 2.4 Inicio `PeriodTabs`: auto-size and `selectorLabel()`; statistics `FiltersPanel`: `selectorLabel()`
- [x] 2.5 `ActivityTab` delta pill: auto-size, `softWrap = false`

## 3. Inventario (`features:product-inventory`) — F-84

- [x] 3.1 `InventoryContent` list `contentPadding` with a bottom clearance for the FAB (design decision 3)
- [x] 3.2 `InventoryScreen`: scroll to top on a category or availability change, not on the first composition

## 4. Screen readers — F-92

- [x] 4.1 `QhathiaDiscountToggleRow`: `stateDescription` from `discount_toggle_on` / `discount_toggle_off` ("Activado" / "Desactivado", "On" / "Off")
- [x] 4.2 `QhathiaTextField(label: String? = null)` → `semantics { contentDescription = label }` on the `BasicTextField`; KDoc
- [x] 4.3 Detalle and Registro `LabeledField` pass their label; the other `QhathiaTextField` uses in those forms pass theirs
- [x] 4.4 `ProductRow` thumbnail: one node "Foto de %1$s" / "Sin foto" (`inventory_product_photo_cd`, `inventory_product_no_photo_cd`), image `contentDescription = null`; drop `inventory_product_image_cd`
- [x] 4.5 `QhathiaPhotoStrip.PhotoThumbnail`: "Foto %1$d de %2$d" and ", principal" (`photo_position_cd`, `photo_main_suffix_cd`)
- [x] 4.6 `OrderFilterChips` and Inicio `PeriodTabs`: `selectable(selected, role = Role.Tab, onClick)` inside `selectableGroup()`

## 5. Verification

- [x] 5.1 Run, one at a time: `:core:ui:testDebugUnitTest`, `:features:home:testDebugUnitTest`, `:features:product-inventory:testDebugUnitTest`, `:features:order-requests:testDebugUnitTest`, then `testDebugUnitTest`
- [x] 5.2 Run `./gradlew lintDebug` and `./gradlew assembleDebug`

## 6. Manual — QA

- [ ] 6.1 Manual — S:EXP-015: dark mode, inactive tabs and "Ver más" readable
- [ ] 6.2 Manual — S:EXP-016: font scale 2.0, "Inventario" and "Solicitudes" complete; period pills "Mes", "Sem.", "Día", "Hora"; Resumen de ventas tabs and "vs …" pills complete
- [ ] 6.3 Manual — S:EXP-013/014/018/019 and S:SOL-091: last inventory row's ⋮ reachable above the FAB; a new category shows the first row
- [ ] 6.4 Manual — S:EXP-010, S:INV-080, S:REG-098, S:DET-090, S:SOL-090 with TalkBack and `inspect_screen`: discount "Activado"/"Desactivado"; "Precio", "Stock", "Tags" named; "Foto de …"; "Foto 1 de 3, principal"; Solicitudes chips as tabs
