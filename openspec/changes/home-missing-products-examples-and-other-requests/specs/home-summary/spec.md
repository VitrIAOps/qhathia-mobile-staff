## MODIFIED Requirements

### Requirement: Suggest products that could increase sales

The Inicio tab SHALL show a `Sugerencias para vender más` section containing three horizontally-scrollable rows:

- **Productos no encontrados** — one card per kiosk search group that found no product in the store's line of business (`groups` of `GET /api/v1/insights/unmet-searches`), showing the number of **distinct people** who searched for it (`personCount`), the canonical term (`term`, first letter capitalised) as the product name, a secondary line with how shoppers asked for it, and an `Agregar` button.
- **También te piden** — one card per group that shoppers asked for but is outside the store's line of business (`otherRequests` of the same response), with the same contents as a "Productos no encontrados" card but **without** any action button.
- **Productos para reponer** — one card per product below the restock threshold (`GET /api/v1/dashboard/restock`), showing its image, its name, a warning chip reading `Solo queda N` (or its plural) and a `Reabastecer` button.

The secondary line SHALL show the group's most frequent example (`examples[].rawText` with the highest `count`), trimmed and in quotes, and SHALL be omitted when there is no example or when it equals the term once case, accents and punctuation are ignored.

The `Agregar` and `Reabastecer` buttons SHALL be rendered as designed but MUST NOT navigate anywhere. A row with no items MUST be omitted rather than rendered empty, and the section heading SHALL be shown whenever at least one row has items.

#### Scenario: Missing-product card contents
- **WHEN** 9 distinct people searched for the term `taladro`, most often as `taladro percutor bosch`
- **THEN** a card shows `9`, `Personas`, `Taladro`, the line `“taladro percutor bosch”` and an `Agregar` button

#### Scenario: Example that adds nothing is omitted
- **WHEN** the most frequent example of the term `amoladora` is `Amoladora.`
- **THEN** the card shows `Amoladora` and no secondary line

#### Scenario: Group without examples
- **WHEN** a group has no examples
- **THEN** its card shows the count and the term, and no secondary line

#### Scenario: Requests outside the line of business
- **WHEN** the response's `otherRequests` holds `pintura` asked for by 3 people
- **THEN** a `También te piden` row shows a card with `3`, `Personas` and `Pintura`, and the card has no button

#### Scenario: No requests outside the line of business
- **WHEN** `otherRequests` is empty or absent
- **THEN** the `También te piden` row is not rendered

#### Scenario: Only requests outside the line of business
- **WHEN** `groups` and the restock list are empty but `otherRequests` is not
- **THEN** the `Sugerencias para vender más` heading and the `También te piden` row are shown, and the other two rows are not

#### Scenario: Restock card contents
- **WHEN** `Taladro Percutor Bosch GSB 13 RE` has 1 unit left
- **THEN** a card shows its image, its name, a `Solo queda 1` warning chip and a `Reabastecer` button

#### Scenario: Restock card for a product with no stock left
- **WHEN** a product in the restock row has 0 units left
- **THEN** its warning chip reads `Sin existencias` instead of `Solo quedan 0`

#### Scenario: Suggestion buttons are inert
- **WHEN** the merchant taps `Agregar` or `Reabastecer`
- **THEN** nothing is navigated to and the merchant stays on the Inicio tab

#### Scenario: Rows scroll horizontally
- **WHEN** a row holds more cards than fit on screen
- **THEN** the row scrolls horizontally and the rest of the screen does not move with it

#### Scenario: Empty row is omitted
- **WHEN** there are no products to restock
- **THEN** the `Productos para reponer` row and its heading are not rendered
