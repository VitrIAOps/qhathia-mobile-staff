## Context

The Product Registration (`:features:product-register`) and Product Detail (`:features:product-detail`) forms share the same shape: an MVI `UiState` holding the raw field strings, a stateless `*Content` composable and dialogs driven by state flags (`showDiscardDialog`, `discountDraft`). Registration holds its photos as local `SelectedImage(uri, mimeType)`; detail holds them as remote `imageUrls`.

The ClickUp task asks for a `Vista previa` button below Tags on both forms that opens a full-screen dialog showing how the product will look in Qhathia Discovery: as a vertical list item, a horizontal list item and the product detail screen. The previews are not interactive — they act like a pre-rendered image the merchant can zoom — and are built from the unsaved form values.

Constraints:

- Feature modules must not depend on each other, and both forms need the same preview UI.
- `:core:ui` has no dependency on `:core:domain` and currently no image-loading library; its shared components take primitives and callbacks only (see `QhathiaDiscountDialog`).
- Min SDK is 26; `Modifier.blur` only has an effect on API 31+.
- The app's single activity is not orientation-locked, so the dialog must survive rotation.
- The Discovery designs (Figma `644:26084`, `644:26105`, `644:26093`) belong to another app with its own look; the detail frame is a 1440×900 landscape layout.

## Goals / Non-Goals

**Goals:**

- One shared preview implementation consumed by both forms, with thin per-feature wiring.
- Preview built purely from in-memory form state — no network, no persistence, no new domain or data code.
- Faithful, non-interactive rendering of the three Discovery layouts with zoom and pan, usable in portrait and landscape.
- Clear gating: the preview is only reachable with a name, a price and at least one photo.

**Non-Goals:**

- Fetching real related products for "Podrías acompañar este producto con…" (mocked and blurred).
- Sharing or exporting the preview as an image file.
- Any change to the Discovery app, the API contract or the inventory list.
- A preview entry point outside the two forms (e.g. from the inventory row menu).
- Designing how discounts look in Discovery — the Figma frames do not cover it (see Open Questions).

## Decisions

### 1. Shared preview lives in `:core:ui`; no new Gradle module

The preview components go in `:core:ui` under `component/productpreview/`, and each feature only adds a button, a state flag and a mapper. This mirrors how `QhathiaDiscountDialog` is shared between the same two features.

- *Alternative — a new `:features:product-preview` module:* rejected; the two form features could not depend on it (feature → feature is forbidden), and routing through `:app` navigation would force the unsaved form values across a navigation boundary.
- *Alternative — duplicate the composables in both features:* rejected; three Discovery layouts duplicated is exactly the drift `:core:ui` exists to prevent.

Module interaction (all within the existing dependency graph):

```
:features:product-register ─┐
                            ├─→ :core:ui  (QhathiaProductPreviewDialog + ProductPreviewData)
:features:product-detail  ──┘
```

### 2. Primitive preview model owned by `:core:ui`

`:core:ui` exposes an immutable `ProductPreviewData(name: String, price: Double, description: String?, categoryName: String?, stock: Int?, imageModels: ImmutableList<String>)`. Nullable fields mean "not entered" and are rendered as `DATO NO INGRESADO` by the components. Each feature maps its own `UiState` to this model with a pure extension (`toPreviewData()`), keeping `:core:ui` free of domain types and making the mapping unit-testable in the feature.

`imageModels` are strings Coil can load directly: `content://` / `file://` URIs from registration, HTTPS URLs from detail. The first entry is the primary photo.

### 3. Add Coil to `:core:ui`

The Discovery layouts need to draw the product photos, so `:core:ui` gains `implementation(libs.coil.compose)` (and `libs.coil.network.okhttp` for the remote URLs used by detail), both already in the version catalog and used by the feature modules.

- *Alternative — an image slot lambda (`image: @Composable (String, Modifier) -> Unit`) supplied by each feature:* rejected; it pushes the same Coil call into both features and makes the `:core:ui` `@Preview`s unable to show a photo.

### 4. Preview visibility is MVI state; view controls are local UI state

Each form's `UiState` gains `showPreview: Boolean` plus a derived `isPreviewEnabled`, and each `UiEvent` gains `PreviewRequested` / `PreviewDismissed` — the same pattern as `showDiscardDialog`. The ViewModel ignores `PreviewRequested` when `isPreviewEnabled` is false.

The selected tab, the item orientation and the zoom/pan transform are ephemeral view state and live inside the dialog (`rememberSaveable` for tab and orientation so they survive rotation; zoom resets on tab, orientation or rotation change). Putting them in the ViewModel would add events to two features for state nothing else reads.

Opening the preview does not mark the form dirty and issues no request.

### 5. Minimum-data rule

`isPreviewEnabled` = valid name (`isValidProductName`) ∧ valid price (`isValidPrice`) ∧ at least one photo (`images` on registration, `imageUrls` on detail) ∧ not submitting/saving. It reuses the validators already in `:core:common`. When disabled, a short hint under the button states the requirement. On detail, a product with no registered photos therefore cannot be previewed — photos are read-only there, and the hint says so.

### 6. "Pre-rendered image" via a fixed-size, non-interactive canvas

Each Discovery layout is composed at its Figma design size (vertical item 176×288, horizontal item 300×120, detail 1440×900) inside a shared `ProductPreviewCanvas` that:

- lays the content out at the fixed design size and scales it uniformly to fit the available area (`graphicsLayer`), so the result looks identical on every device, like an image;
- applies the merchant's zoom (1×–4×, pinch) and pan (only while zoomed, clamped to the content bounds), with double-tap to toggle between fit and 2×;
- intercepts all pointer input above the content so no child ever receives a tap, and replaces the subtree's semantics with a single image-like content description so TalkBack does not announce dead buttons.

- *Alternative — capture the composable to a bitmap (`GraphicsLayer.toImageBitmap`) and show it in an `Image`:* rejected; it adds an async capture step, must wait for Coil to finish loading before capturing, and blurs when zoomed. Scaling live composables stays sharp and gives the same "it's a picture" behavior.
- *Alternative — render with responsive layouts:* rejected; the point is to show the Discovery layout, not a reflowed approximation.

The Discovery renderings use a fixed light palette taken from the Figma frames regardless of the staff app's theme — they depict another app. The dialog chrome (top bar, tabs, hints) follows `QhathiaTheme` in light and dark.

### 7. Dialog structure

A `Dialog` with `usePlatformDefaultWidth = false` filling the screen:

- top bar: close action and the `Vista previa` title;
- tabs `Item` / `Detalle`;
- an orientation switch `Vertical` / `Horizontal` (reusing `QhathiaSegmentedControl`), active on the Item tab; on the Detalle tab it is shown locked on `Horizontal`;
- the canvas filling the rest;
- on the Detalle tab while the device is in portrait, a hint inviting the merchant to rotate the phone.

System back and the close action both dismiss the dialog and return to the form untouched.

### 8. Related products are mocked and blurred

The detail layout's "Podrías acompañar este producto con…" row shows four placeholder horizontal items built from the product's own primary photo and static mock text, blurred with `Modifier.blur` on API 31+. On API 26–30, where blur is a no-op, the row is drawn as neutral skeleton blocks at reduced opacity instead, so mock text is never legible.

### 9. Price shown in the preview

The preview shows what a shopper would pay: the discounted price when a discount is applied on the form (`discountedPrice`), otherwise the entered price. Only the single price slot from the Figma frames is used — no struck-through original price is invented.

## Risks / Trade-offs

- [The Discovery layouts are re-implemented here and can drift from the real Discovery app] → Each component's KDoc names its Figma node; the components are isolated in one package so a redesign touches one place.
- [The 1440×900 detail canvas is tiny on a portrait phone] → It is fit-scaled, zoomable to 4×, and the rotate hint is shown; landscape gives it the full width.
- [Blur unavailable below API 31] → Skeleton fallback (Decision 8).
- [Large local photos could be decoded at full size inside a scaled canvas] → Request Coil images with explicit size constraints matching the design slots, per the performance standards.
- [Swallowing pointer input could hide the preview from accessibility services] → The canvas exposes one merged content description summarising the product (name and price) and the dialog controls stay fully accessible with 48dp targets.
- [Rotation recreates the activity while the dialog is open] → `showPreview` lives in the ViewModel and tab/orientation in `rememberSaveable`, so the dialog reopens in the same view.

## Migration Plan

Purely additive client-side UI: no data migration, no backend coordination, no feature flag. Rollback is reverting the change.

## Open Questions

- **Discount in the preview:** the Discovery frames do not show a discounted product. Decision 9 shows the final price only; confirm with design whether Discovery will display the original price or a discount badge, and adjust the renderings then.
- **Orientation wording in the task ("voltear vertical y horizontalmente"):** interpreted as the Vertical / Horizontal item switch plus full support for rotating the device. If a free image rotation or mirror gesture was intended, it would be an addition to the canvas.
- **Detail preview with no photos:** currently not previewable (Decision 5). Confirm this is acceptable for legacy products without media.
