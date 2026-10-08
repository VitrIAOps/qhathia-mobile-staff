> No new Gradle module is required: every module touched below already exists and is registered in `settings.gradle.kts`. No code is added as packages inside `:app`, and no manifest entries or permissions are needed.

## 1. Design reference & build setup (`:core:ui`)

- [x] 1.1 Load the Figma design context for the four nodes of file `uLhEDY8ZMxdNiQZU4uYA1V` — form button `1478:15846`, vertical item `644:26084`, horizontal item `644:26105`, detail `644:26093` — and note the sizes, spacing, typography and colors each rendering needs.
- [x] 1.2 Add `implementation(libs.coil.compose)` and `implementation(libs.coil.network.okhttp)` to `core/ui/build.gradle.kts` (both aliases already exist in `gradle/libs.versions.toml`).

## 2. Shared preview model & canvas (`:core:ui`)

- [x] 2.1 Add `component/productpreview/ProductPreviewData.kt`: immutable `ProductPreviewData(name: String, price: Double, description: String?, categoryName: String?, stock: Int?, imageModels: ImmutableList<String>)` — primitives only, no domain types; nullable fields mean "not entered".
- [x] 2.2 Add `component/productpreview/DiscoveryPreviewPalette.kt` with the fixed light Discovery colors and text styles taken from Figma, independent of `QhathiaTheme`'s light/dark scheme.
- [x] 2.3 Add `component/productpreview/ProductPreviewCanvas.kt`: lays its content out at a fixed design size, fit-scales it to the available area, applies pinch zoom (1×–4×), clamped pan while zoomed and double-tap toggle (fit ↔ 2×), and resets when a `resetKey` changes.
- [x] 2.4 In `ProductPreviewCanvas`, intercept all pointer input above the content so no child receives taps, and replace the subtree's semantics with a single image-like content description.
- [x] 2.5 Extract the zoom/pan math (scale clamping, offset clamping to content bounds) into a pure function or state holder and unit-test it in `:core:ui` (JUnit 5).

## 3. Discovery renderings (`:core:ui`)

- [x] 3.1 Add `DiscoveryVerticalItem` (Figma `644:26084`, 176×288): primary photo, name (max 3 lines, ellipsized), `S/` price and the static cart control.
- [x] 3.2 Add `DiscoveryHorizontalItem` (Figma `644:26105`, 300×120): primary photo, category name, name and price.
- [x] 3.3 Add `DiscoveryProductDetail` (Figma `644:26093`, 1440×900): header actions, main photo, thumbnail column, name, availability line, price, description, and the static quantity / add-to-cart / share controls and bottom prompt.
- [x] 3.4 Render `DATO NO INGRESADO` for a null description, category name or stock; render the stock as `%d disponibles` when present.
- [x] 3.5 Add the mocked "Podrías acompañar este producto con…" row: four placeholder horizontal items blurred with `Modifier.blur` on API 31+, and neutral skeleton blocks at reduced opacity on API 26–30.
- [x] 3.6 Load photos with Coil using explicit size constraints matching each design slot, with a neutral placeholder while loading or on error.

## 4. Preview dialog (`:core:ui`)

- [x] 4.1 Add `component/productpreview/QhathiaProductPreviewDialog.kt`: full-screen `Dialog` (`usePlatformDefaultWidth = false`) with a top bar (close action + `Vista previa` title), taking `data: ProductPreviewData` and `onDismiss`.
- [x] 4.2 Add the `Item` / `Detalle` tabs, defaulting to `Item`, with the selection held in `rememberSaveable`.
- [x] 4.3 Add the `Vertical` / `Horizontal` switch with `QhathiaSegmentedControl`, held in `rememberSaveable`, default `Vertical`; on the `Detalle` tab show it locked on `Horizontal`.
- [x] 4.4 Show the matching rendering inside `ProductPreviewCanvas`, using tab + orientation as the canvas `resetKey` so zoom resets on every view change.
- [x] 4.5 On the `Detalle` tab, show the rotate-phone hint while the device is in portrait and hide it in landscape.
- [x] 4.6 Add the strings to `core/ui/src/main/res/values/strings.xml` (es) and `values-en/strings.xml`: title, tab labels, orientation labels, close content description, rotate hint, `DATO NO INGRESADO`, `%d disponibles`, the preview content description, and the static Discovery labels.
- [ ] 4.7 Give every dialog control a 48dp touch target and a content description, per `docs/android/android-ui-ux-compose-guidelines.md`.
- [x] 4.8 Add light and dark `@Preview`s: dialog on Item/vertical, Item/horizontal and Detalle (portrait and landscape), plus each rendering with complete data and with missing optional data.

## 5. Product registration (`:features:product-register`)

- [x] 5.1 Extend `ProductRegisterUiState` with `showPreview: Boolean = false` and derived `isPreviewEnabled` (valid name, valid price, at least one image, not submitting).
- [x] 5.2 Add a pure `ProductRegisterUiState.toPreviewData()` mapper: name, `discountedPrice ?: parsed price`, blank description → null, selected category's name or null, valid quantity or null, image URIs in order.
- [x] 5.3 Extend `ProductRegisterUiEvent` with `PreviewRequested` and `PreviewDismissed`; handle them in `ProductRegisterViewModel` (ignore `PreviewRequested` when `isPreviewEnabled` is false; neither touches `isDirty`).
- [x] 5.4 In `ProductRegisterContent`, add the outlined `Vista previa` button below `RegisterTagsField` and above `FormActions` per Figma `1478:15846`, with the requirement hint below it while disabled for missing data.
- [x] 5.5 Show `QhathiaProductPreviewDialog` while `showPreview` is true, dispatching `PreviewDismissed` on close.
- [x] 5.6 Add `register_preview` and `register_preview_hint` strings (es + en).
- [x] 5.7 Add light and dark `@Preview`s for the form with the preview button enabled and disabled.

## 6. Product detail (`:features:product-detail`)

- [x] 6.1 Extend `ProductDetailUiState` with `showPreview: Boolean = false` and derived `isPreviewEnabled` (loaded, valid name, valid price, `imageUrls` not empty, not saving).
- [x] 6.2 Add a pure `ProductDetailUiState.toPreviewData()` mapper mirroring registration, using `imageUrls` as the image models.
- [x] 6.3 Extend `ProductDetailUiEvent` with `PreviewRequested` and `PreviewDismissed`; handle them in `ProductDetailViewModel` without changing `isDirty`.
- [x] 6.4 In `ProductDetailContent`'s `EditForm`, add the outlined `Vista previa` button below `DetailTagsField` and above `FormActions`, with the requirement hint below it while disabled for missing data.
- [x] 6.5 Show `QhathiaProductPreviewDialog` while `showPreview` is true, dispatching `PreviewDismissed` on close.
- [x] 6.6 Add `detail_preview` and `detail_preview_hint` strings (es + en).
- [x] 6.7 Add light and dark `@Preview`s for the form with the preview button enabled and disabled (no photos).

## 7. Tests & Definition of Done

- [x] 7.1 Extend `ProductRegisterViewModelTest`: `isPreviewEnabled` gating (name, price, image, submitting; independent of category/stock/tags), `PreviewRequested` opens only when enabled, `PreviewDismissed` closes, neither changes `isDirty` nor triggers a use case.
- [x] 7.2 Unit-test `ProductRegisterUiState.toPreviewData()`: complete data, blank description, no category, blank/invalid quantity, discounted price, image order.
- [x] 7.3 Extend `ProductDetailViewModelTest`: `isPreviewEnabled` gating (loading, no photos, invalid name/price, saving), open/dismiss, unsaved edits reflected in the preview data, `isDirty` and `isSaveEnabled` unchanged by the preview.
- [x] 7.4 Unit-test `ProductDetailUiState.toPreviewData()` with the same cases as 7.2.
- [ ] 7.5 Add a Compose UI test in `:core:ui` `androidTest`: tab switch changes the rendering, orientation switch changes the item, a click on the drawn cart control triggers nothing, and missing data shows `DATO NO INGRESADO`.
- [x] 7.6 Manually verify on a device or emulator: both forms open the preview, pinch/pan/double-tap work, rotation keeps the tab, back closes only the dialog, and the related row is obscured on API < 31 and API ≥ 31.
- [x] 7.7 Run `./gradlew test` and `./gradlew lint` and confirm both pass; confirm every new or changed screen composable has working light and dark `@Preview`s.
