## ADDED Requirements

### Requirement: Inventario header bell opens Solicitudes

The bell in the Inventario header SHALL open the Solicitudes tab, the same way the Inicio and Mi perfil bells do: as a tab switch, not as a screen pushed on top of Inventario. It SHALL be announced to accessibility services as "Ver solicitudes". It SHALL NOT be a control without an action.

#### Scenario: Tapping the bell
- **WHEN** the merchant taps the bell in the Inventario header
- **THEN** the Solicitudes tab is shown and selected in the bottom bar

#### Scenario: Returning to Inventario
- **WHEN** the merchant opened Solicitudes from the Inventario bell and then taps the Inventario tab
- **THEN** Inventario is shown with its search and filters as they were

#### Scenario: Accessible label
- **WHEN** a screen-reader user focuses the bell
- **THEN** it is announced as "Ver solicitudes"
