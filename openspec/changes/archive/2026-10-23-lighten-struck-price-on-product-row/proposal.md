## Why

On a discounted Inventory row, the discounted price is drawn in bold, so it does not look like the row's regular price, and the struck-through original price is drawn at the regular weight, so it competes with it. The discounted price should simply take the place of the regular price, and the original price should read as a lighter reference.

## What Changes

- On an Inventory row with a discount in effect, the discounted price is drawn in the same style as the regular price of a product without a discount (`bodyMedium`, `onSurface`, regular weight): it is no longer bold.
- The original price (struck through, on the left) is drawn in the **Light (300)** weight of the app's typeface.
- Nothing else on the row changes: the order stays the same (struck original on the left, discounted price on the right), the plain price of a product without a discount keeps its current style, and the screen-reader announcement is unchanged.
- The app theme registers the Light weight of Urbanist so the lighter price actually renders lighter. Today only Normal, Medium, SemiBold and Bold are registered, and a Light request would silently fall back to Normal.

## Capabilities

### New Capabilities

_None._

### Modified Capabilities

- `product-inventory`: the "Discounted price on the product row" requirement now states the original price is drawn in a light weight, to the left of the discounted price, which uses the same style as a regular price.

## Impact

- **`core:ui`** — `Type.kt`: `UrbanistFamily` gains the Light (300) weight. Same variable font file; no new asset, no change to existing text styles.
- **`features:product-inventory`** — `ProductRow.kt` (`ProductPrice`): the struck-through price uses the Light weight and the discounted price drops its bold. Previews are re-checked.
- **Not affected:** `features:product-detail`, `features:product-register`, the discount dialog in `core:ui`, the API, the domain and data layers.
