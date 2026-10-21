## MODIFIED Requirements

### Requirement: Product count

The Inventory screen SHALL display a count label reflecting the number of products currently shown after the active filters and search have been applied. The label SHALL use the singular for exactly one product ("1 producto") and the plural otherwise ("0 productos", "32 productos").

#### Scenario: Count reflects the visible products

- **WHEN** the visible product set changes due to loading, filtering, or searching
- **THEN** the count label updates to the number of products currently displayed

#### Scenario: One product

- **WHEN** a search leaves exactly one product visible
- **THEN** the count label reads "1 producto"

#### Scenario: Several products

- **WHEN** 32 products are visible
- **THEN** the count label reads "32 productos"
