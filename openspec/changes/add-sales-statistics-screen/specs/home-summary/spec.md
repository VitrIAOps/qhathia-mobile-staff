## ADDED Requirements

### Requirement: Open the sales statistics from Ver más

The `Resumen` heading of the Inicio tab SHALL show an enabled `Ver más` action that opens the "Resumen de ventas" screen, passing the period currently selected on the summary card. The navigation MUST push the screen on top of Inicio — not switch tabs — so that going back returns to Inicio with its state preserved.

#### Scenario: Ver más opens the statistics screen
- **WHEN** the merchant taps `Ver más` while `Semanal` is selected
- **THEN** the "Resumen de ventas" screen opens with `Semanal` selected

#### Scenario: Back restores Inicio
- **WHEN** the merchant had selected `Diario` on Inicio, opened `Ver más` and went back
- **THEN** Inicio is shown with `Diario` still selected

#### Scenario: Ver más is announced as an action
- **WHEN** a screen reader focuses `Ver más`
- **THEN** it is announced as an enabled button
