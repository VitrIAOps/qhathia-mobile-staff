## MODIFIED Requirements

### Requirement: Discounted price on the product row

When a product has a discount in effect, its Inventory row SHALL show the original `price` struck through and the `discountedPrice` from `GET /api/v1/products` next to it, to its right. The discounted price SHALL be drawn in the same style as the price of a product without a discount. The struck-through original price SHALL be drawn in a light font weight, lighter than the regular price text, so that the discounted price reads as the price that applies. The discounted price SHALL be the value sent by the server; the app SHALL NOT compute it for the row from the product's discounts. When `discountedPrice` is absent or `null`, the row SHALL show only the price, unstyled. Screen readers SHALL announce both prices with their meaning (original price and price with discount), not just two numbers.

#### Scenario: Product with a discount in effect

- **WHEN** a product row is rendered for a product with `price` 35.90 and `discountedPrice` 30.90
- **THEN** the row shows `S/ 35.90` struck through in a light weight, followed on its right by `S/ 30.90` in the same style as a regular price, not bold

#### Scenario: Product without a discount

- **WHEN** a product row is rendered for a product whose `discountedPrice` is `null`
- **THEN** the row shows only `S/ 35.90`, not struck through and in the regular weight

#### Scenario: Screen reader on a discounted row

- **WHEN** a screen reader focuses the price of a discounted row
- **THEN** it announces the original price and the price with discount as such
