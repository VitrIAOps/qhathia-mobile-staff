## Why

The Inventory list shows a stock number that is invented: `mockedStock` derives a value between 10 and 99 from the hash of the product id. It is stable across recompositions, so it looks trustworthy, and it already caused a lost sale (the merchant saw "19 stock", the product had 1, and an order for 2 was rejected — triage BUG-019/BUG-021). The same list also shows every product at full price even when it has an active discount (BUG-013). Since 2026-10-18 the backend sends the real `stock`, plus a server-computed `discountedPrice` and `discountEndsAt`, on every row of `GET /api/v1/products`, so both problems can be fixed by reading fields that already arrive.

## What Changes

- Add `stock`, `discountedPrice` and `discountEndsAt` to the products list transport model and to the `Product` domain model, mapped at the data boundary.
- The Inventory row shows the product's real stock. When `stock` is `null` (legacy products and "consult" mode) no number is shown; when it is `0` the row reads "Sin existencias" instead of "0 stock", matching how the rest of the app names a stock-out.
- **Remove** the `mockedStock` helper and the "mocked" wording from the code and the spec.
- When `discountedPrice` is present, the Inventory row shows the original price struck through and the discounted price highlighted. The app does not compute this price for the row: the server's value is the only source.
- After the merchant saves a discount from the row's `⋮` menu, the product list is re-read without blanking the screen, so the row shows the server's new `discountedPrice`.
- Product Detail and Product Registration keep computing the resulting price locally. On those screens it is a live preview of a form the merchant is still editing (price and discount not yet saved), which the server cannot know about.

## Capabilities

### New Capabilities

_None._

### Modified Capabilities

- `product-inventory`: the stock indicator stops being a mock and reads the real `stock` (with defined behaviour for `null` and `0`); the product row gains a discounted-price display driven by the server's `discountedPrice`; saving a discount from the row menu refreshes the list.

## Impact

- **`core:network`** — `ProductDto` gains three optional fields. No request changes; the fields have defaults, so an older backend still deserializes.
- **`core:domain`** — `Product` gains `stock`, `discountedPrice`, `discountEndsAt`.
- **`core:data`** — `ProductDto.toDomain()` maps the new fields.
- **`core:testing`** — `TestFixtures.product(...)` gains the new parameters with defaults.
- **`features:product-inventory`** — `ProductRow` (stock, struck-through price, removal of `mockedStock`), `InventoryViewModel` (quiet reload after a discount save), strings (es/en), previews and tests.
- **Not affected:** `features:product-detail`, `features:product-register`, the Discovery app (its part of BUG-013 is tracked in its own repo).
- **Contract:** `docs/android/api-contract.md` §25 and §26 (fields in production since 2026-10-18). The `GET /api/v1/products` example in §8 does not list the new fields yet.
