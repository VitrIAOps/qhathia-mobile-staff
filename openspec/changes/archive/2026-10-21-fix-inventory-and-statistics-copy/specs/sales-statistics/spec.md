## MODIFIED Requirements

### Requirement: Rank the products that sell the most

The Ranking tab SHALL show a `Lo que más vende` card with a highlighted total in the DS success colours reading `S/ <total>` and `Generados por tus N productos principales` (N being the number of products listed), followed by one row per top-selling product with its position badge, thumbnail, name, `N vendidos` (singular for one) and revenue. When there are no top sellers the card SHALL show only its title and an empty message: no total, no trend icon, no caption and no rows.

#### Scenario: Top sellers card
- **WHEN** three products generated S/ 4,090 and the first sold 34 units for S/ 1,870
- **THEN** the card shows `S/ 4,090`, `Generados por tus 3 productos principales` and a first row with badge `1`, its name, `34 vendidos` and `S/ 1,870`

#### Scenario: Singular units
- **WHEN** a top seller sold 1 unit
- **THEN** its row reads `1 vendido`

#### Scenario: No top sellers
- **WHEN** nothing sold in the period
- **THEN** the card shows its title and an empty message, and no `S/ 0`, no caption and no rows

### Requirement: Surface the products that need attention

The Ranking tab SHALL show a `Lo que necesita atención` card with a highlighted total in the DS error colours reading `S/ <total immobilized>`, followed by one row per active product with stock that did not sell in the period, ordered by immobilized amount from highest to lowest. The total is the money immobilized in the listed products (`stock × price`), not revenue, and its caption MUST say so: it SHALL read `Inmovilizados en los N productos sin ventas` (`Inmovilizados en el producto sin ventas` for one), where N is the number of listed products. When the backend reports a total count of products without sales larger than N, the caption SHALL read `Inmovilizados en N de los M productos sin ventas`, where M is that total count. The caption MUST NOT use the wording `Generados`, which the revenue card uses. Each row SHALL show an alert glyph — error colour on the first row, warning colour on the others — thumbnail, name, `N unidades sin vender` (singular for one) and the product's immobilized amount (`stock × price`). When there are none the card SHALL show only its title and an empty message: no total, no trend icon, no caption and no rows.

#### Scenario: Needs-attention card
- **WHEN** three products hold S/ 4,020 of unsold stock, the backend reports a total count of 3, and one of them has 34 units immobilizing S/ 3,400
- **THEN** the card shows `S/ 4,020`, `Inmovilizados en los 3 productos sin ventas`, and that product's row reads `34 unidades sin vender` and `S/ 3,400`

#### Scenario: More products without sales than listed
- **WHEN** the card lists 3 products and the backend reports a total count of 17
- **THEN** the caption reads `Inmovilizados en 3 de los 17 productos sin ventas`

#### Scenario: Total count missing
- **WHEN** the card lists 3 products and the backend does not send a total count
- **THEN** the caption reads `Inmovilizados en los 3 productos sin ventas`

#### Scenario: A single product
- **WHEN** the card lists one product and the total count is 1
- **THEN** the caption reads `Inmovilizados en el producto sin ventas`

#### Scenario: Rows ordered by immobilized money
- **WHEN** the backend returns products immobilizing S/ 50, S/ 620 and S/ 100
- **THEN** the rows are shown in the order S/ 620, S/ 100, S/ 50 and only the first uses the error colour

#### Scenario: Nothing needs attention
- **WHEN** every product with stock sold in the period
- **THEN** the card shows its title and an empty message, and no `S/ 0`, no caption and no rows
