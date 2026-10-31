## ADDED Requirements

### Requirement: Discount end countdown on product detail

The Product Detail screen SHALL show, below the discount summary line, how long remains until the discount in force ends, using `discountEndsAt` from `GET /api/v1/products/{id}`. The countdown SHALL be shown only while the form's price and discount are the ones loaded with the product and the server reports a discount in force (`discountedPrice` is not `null`) with a `discountEndsAt`. It SHALL read:

- `Termina en {d} días y {h} horas` when one day or more remains and the leftover hours are not zero, using the singular `día` / `hora` for one;
- `Termina en {d} días` (or `1 día`) when one day or more remains and the leftover hours are zero;
- `Termina en {h} horas` (or `1 hora`) when less than one day but at least one hour remains;
- `Termina en menos de 1 hora` when less than one hour remains.

Days and hours SHALL be whole units rounded down. The countdown SHALL refresh at least once a minute while the screen is open, and SHALL NOT be shown once the end has passed.

#### Scenario: Several days remaining

- **WHEN** the product loads with a discount in force that ends in 6 days and 4 hours and 20 minutes
- **THEN** the line below the summary reads `Termina en 6 días y 4 horas`

#### Scenario: Whole days remaining

- **WHEN** the discount ends in exactly 1 day and 30 minutes
- **THEN** the line reads `Termina en 1 día`

#### Scenario: Last day

- **WHEN** the discount ends in 5 hours and 59 minutes
- **THEN** the line reads `Termina en 5 horas`

#### Scenario: Last hour

- **WHEN** the discount ends in 40 minutes
- **THEN** the line reads `Termina en menos de 1 hora`

#### Scenario: Discount already ended

- **WHEN** the end of the discount passes while the screen is open
- **THEN** the countdown line is no longer shown

#### Scenario: Merchant edits the discount or the price

- **WHEN** the merchant applies a different discount or changes the price
- **THEN** the countdown line is hidden

#### Scenario: No discount in force on the server

- **WHEN** the product loads with a discount on the form but the server's `discountedPrice` is `null`
- **THEN** no countdown line is shown

#### Scenario: Product without a discount

- **WHEN** the product loads without a discount
- **THEN** no countdown line is shown
