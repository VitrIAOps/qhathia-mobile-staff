## Why

Merchants register and edit products without ever seeing how they will look to shoppers in Qhathia Discovery (the kiosk catalog), so weak photos, truncated names or missing descriptions are only noticed after the product is live. A preview built from the values currently on the form lets them check and fix the listing before saving. Source: ClickUp task `86e3mqxh3` — "Previsualización en registro de producto".

## What Changes

- Add a `Vista previa` outlined button below the Tags field on both the Product Registration form and the Product Detail (edit) form (Figma `1478:15846`).
- The button is enabled only when the form holds the minimum data for a preview: a valid product name, a valid price and at least one photo. Otherwise it is disabled and a hint explains what is missing.
- Tapping it opens a full-screen preview dialog built from the values currently on the form — nothing is saved and no request is issued.
- The dialog has two tabs:
  - **Item** — the product as a Discovery catalog list item, with an orientation switch: vertical card (Figma `644:26084`) or horizontal card (Figma `644:26105`).
  - **Detalle** — the Discovery product detail screen (Figma `644:26093`), always rendered in landscape, with a hint inviting the merchant to rotate the phone.
- Previews behave like a pre-rendered image: none of the Discovery controls inside them (cart, share, quantity, thumbnails) react to taps. The merchant can pinch to zoom, pan while zoomed, and use the dialog in either device orientation.
- Optional data missing from the form (description, category, stock) is rendered as `DATO NO INGRESADO`.
- The "Podrías acompañar este producto con…" section of the detail preview is filled with mocked, blurred placeholder items — related products are not fetched.

## Capabilities

### New Capabilities

- `product-preview`: the full-screen preview dialog — minimum-data rule, Item / Detalle tabs, vertical / horizontal item orientation, landscape-only detail, non-interactive image-like rendering with zoom and pan, missing-data placeholder, and the mocked blurred related-products section.

### Modified Capabilities

- `product-registration`: the registration form gains the `Vista previa` action below Tags, gated by the minimum preview data, which opens the preview from the unsaved form values.
- `product-detail`: the product detail form gains the same `Vista previa` action below Tags, built from the form's current (possibly edited, unsaved) values and the product's registered photos.

## Impact

- **`:core:ui`** — new shared preview components (dialog, zoomable non-interactive canvas, Discovery vertical item, horizontal item and detail renderings), strings (es + en) and `@Preview`s. Gains a Coil dependency (`libs.coil.compose`, already in the version catalog) to render the product photos.
- **`:features:product-register`** — `ProductRegisterUiState` / `UiEvent` / `ViewModel` / `Content`: preview visibility flag, enablement rule, mapping of form values to the preview data, button below Tags.
- **`:features:product-detail`** — the same additions on `ProductDetailUiState` / `UiEvent` / `ViewModel` / `Content`.
- **No new Gradle module**, no `:app` changes, no navigation route (the preview is a dialog owned by each form).
- **No API, DTO, domain or data-layer changes**; no backend dependency. No new permissions or manifest entries.
