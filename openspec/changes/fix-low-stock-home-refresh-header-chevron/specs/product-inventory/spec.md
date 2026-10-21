## ADDED Requirements

### Requirement: Inventario header has no back control

Inventario is a root bottom-navigation destination, so its header SHALL NOT show a back chevron or any control that suggests going back. The header SHALL NOT expose a back action to accessibility services. The title SHALL start at the header's horizontal padding.

#### Scenario: No back chevron in the header
- **WHEN** the Inventario tab is shown
- **THEN** the header shows the title without a back chevron

#### Scenario: TalkBack announces no back action
- **WHEN** a screen-reader user explores the Inventario header
- **THEN** no element is announced as `Volver`
