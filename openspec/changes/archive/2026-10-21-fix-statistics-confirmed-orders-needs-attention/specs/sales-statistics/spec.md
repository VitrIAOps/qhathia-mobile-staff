## MODIFIED Requirements

### Requirement: Surface the products that need attention

The Ranking tab SHALL show a `Lo que necesita atención` card with a highlighted total in the DS error colours reading `S/ <total immobilized>`, followed by one row per active product with stock that did not sell in the period, ordered by immobilized amount from highest to lowest. The total is the money immobilized in the listed products (`stock × price`), not revenue, and its caption MUST say so: it SHALL read `Inmovilizados en los N productos sin ventas` (`Inmovilizados en el producto sin ventas` for one), where N is the number of listed products. When the backend reports a total count of products without sales larger than N, the caption SHALL read `Inmovilizados en N de los M productos sin ventas`, where M is that total count. The caption MUST NOT use the wording `Generados`, which the revenue card uses. Each row SHALL show an alert glyph — error colour on the first row, warning colour on the others — thumbnail, name, `N unidades sin vender` (singular for one) and the product's immobilized amount (`stock × price`). When there are none the card SHALL show an empty message instead of rows.

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
- **THEN** the card shows an empty message and no rows

### Requirement: Show tablet activity

The Actividad tab SHALL show an `Actividad en la tablet` card with three figures for the selected period, read as a funnel: **Clientes usaron la tablet** (distinct tablet sessions) and **Productos consultados** (product views) side by side, and **Pedidos confirmados** (orders confirmed in the period, from `confirmedOrders`) below them. Each figure SHALL carry a pill with its variation against the previous period — success colours with an increase glyph when zero or positive, error colours with a decrease glyph when negative — worded for the period (e.g. `+ 12% vs semana pasada`). When the backend reports no variation the pill MUST be omitted. Zero figures MUST be shown as `0`, not as an error. When the backend does not send `confirmedOrders` at all, the confirmed-orders figure SHALL be omitted rather than shown as `0`.

#### Scenario: Activity figures
- **WHEN** 63 customers used the tablet (+12.5%), 148 products were viewed (+8.8%) and 9 orders were confirmed (+12.5%) this week
- **THEN** the card shows `63` with `+ 13% vs semana pasada`, `148` with `+ 9% vs semana pasada`, and `9` `Pedidos confirmados` with `+ 13% vs semana pasada`

#### Scenario: Negative variation
- **WHEN** product views fell 20% against last week
- **THEN** that pill reads `- 20% vs semana pasada` in the error colours

#### Scenario: No variation base
- **WHEN** the backend reports no variation for customers
- **THEN** the customers figure shows no pill

#### Scenario: Tablet without events
- **WHEN** the backend reports 0 sessions, 0 views and 0 confirmed orders
- **THEN** all three figures read `0` and no error is shown

#### Scenario: Older backend without confirmed orders
- **WHEN** the activity response has no `confirmedOrders` field
- **THEN** the card shows only the customers and products figures, and no error is shown
