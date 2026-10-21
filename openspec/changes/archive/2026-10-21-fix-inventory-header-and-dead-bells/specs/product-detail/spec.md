## ADDED Requirements

### Requirement: Detalle de producto header bell opens Solicitudes

The bell in the Detalle de producto header SHALL open the Solicitudes tab and SHALL be announced to accessibility services as "Ver solicitudes". Because it leaves the form, it SHALL go through the same rule as back and cancel ("Discard guard"): with no unsaved changes it SHALL leave immediately; with unsaved changes it SHALL show the discard-confirmation dialog and SHALL only leave if the merchant confirms. Leaving this way SHALL close the Detalle de producto screen, so that returning to Inventario shows the list and not the abandoned form. While a save is in progress the bell SHALL be disabled.

#### Scenario: Bell on a pristine form
- **WHEN** the merchant taps the bell without having changed anything
- **THEN** the Detalle de producto screen closes and the Solicitudes tab is shown

#### Scenario: Bell on a dirty form, confirmed
- **WHEN** the merchant has unsaved changes, taps the bell and confirms the discard dialog
- **THEN** the changes are discarded, the Detalle de producto screen closes and the Solicitudes tab is shown

#### Scenario: Bell on a dirty form, dismissed
- **WHEN** the merchant has unsaved changes, taps the bell and dismisses the discard dialog
- **THEN** the merchant stays on Detalle de producto with the entered data intact

#### Scenario: Back to Inventario afterwards
- **WHEN** the merchant left Detalle de producto through the bell and then taps the Inventario tab
- **THEN** the Inventario list is shown, not the Detalle de producto form

#### Scenario: Bell while saving
- **WHEN** a save is in progress
- **THEN** the bell is disabled
