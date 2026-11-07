## ADDED Requirements

### Requirement: Write money the same way everywhere

Every amount of money the app shows SHALL start with `S/ ` and SHALL use a point as decimal separator and a comma to group thousands, whatever the app language. Prices — product prices, discounted prices, order totals, unit and list prices, line totals and plan prices — SHALL show exactly two decimals, rounded half up. Dashboard totals and figures (sales summaries, ranking, chart labels) SHALL show whole soles, rounded half up. No amount SHALL be shown in scientific notation.

#### Scenario: A large price in Inventario
- **WHEN** a product costs 12345678.5
- **THEN** Inventario shows `S/ 12,345,678.50`, in Spanish and in English

#### Scenario: A price with cents
- **WHEN** a product costs 189.9
- **THEN** every screen that shows its price shows `S/ 189.90`

#### Scenario: The struck price in Aplicar descuento
- **WHEN** the discount dialog shows the original price 189.9
- **THEN** it reads `S/ 189.90`, struck through

#### Scenario: An order total
- **WHEN** an order totals 1234.5
- **THEN** its card and its detail show `S/ 1,234.50`

#### Scenario: A dashboard total
- **WHEN** the weekly sales total 2848.5
- **THEN** Inicio shows `S/ 2,849`

#### Scenario: Plan price
- **WHEN** the plan costs 49.9 a month
- **THEN** Mi suscripción shows `S/ 49.90`

### Requirement: Seed the price field with a typeable number

The product Detalle price field SHALL open with the product's price written as the merchant would type it: without decimals for a whole price, and with exactly two decimals otherwise, using a point, without grouping and never in scientific notation. The text it opens with SHALL be accepted by the price validation, so a product's discount can be edited without retyping its price.

#### Scenario: A whole price
- **WHEN** the product costs 149.0
- **THEN** the field shows `149`

#### Scenario: A price with cents
- **WHEN** the product costs 189.9
- **THEN** the field shows `189.90`

#### Scenario: A large price
- **WHEN** the product costs 12345678.5
- **THEN** the field shows `12345678.50`, it is valid, and the discount switch is enabled
