## ADDED Requirements

### Requirement: Refresh Inicio when it returns to the screen

The Inicio tab SHALL re-read its summary, suggestions and pending-order count for the selected period every time it comes back on screen after having been hidden: returning from another bottom-navigation tab, returning from a screen pushed on top of it (such as the sales statistics), or the app returning from the background. This refresh SHALL behave like a manual refresh: it MUST keep the data already on screen while it is in flight and MUST keep the selected period. The first appearance of Inicio SHALL issue a single load, not two.

#### Scenario: Restocked product leaves the restock list
- **WHEN** Inicio lists a product under `Productos para reponer` as `Sin existencias`, the merchant restocks it from Inventario, and switches back to Inicio
- **THEN** Inicio re-reads its data and the product no longer appears as out of stock

#### Scenario: Back from the sales statistics
- **WHEN** the merchant opens the sales statistics from `Ver más` and goes back to Inicio
- **THEN** Inicio re-reads its data for the selected period

#### Scenario: App returns from the background
- **WHEN** the app returns to the foreground with Inicio on screen
- **THEN** Inicio re-reads its data

#### Scenario: Data stays on screen during the refresh
- **WHEN** Inicio refreshes on return and it already shows a summary
- **THEN** the summary and its `Última actualización` time stay visible until the new data arrives, and no full-screen loading state replaces them

#### Scenario: First appearance loads once
- **WHEN** Inicio is shown for the first time after the app shell opens
- **THEN** exactly one load is issued
