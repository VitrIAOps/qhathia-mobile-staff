## ADDED Requirements

### Requirement: Prefill the name from a missing-product suggestion

The Registro form SHALL accept an optional product name when it is opened. When one is given, the `Nombre` field SHALL start with it. The prefilled name SHALL NOT count as an unsaved change: leaving without editing anything SHALL NOT ask to discard. Opened without a name, the form SHALL start empty as before.

#### Scenario: Opened from Agregar
- **WHEN** the form is opened with the name `Soldadora`
- **THEN** `Nombre` reads `Soldadora` and back leaves without the discard dialog

#### Scenario: Editing the prefilled form
- **WHEN** the form was opened with `Soldadora` and the merchant types a price
- **THEN** back asks to discard the changes

#### Scenario: Opened from Inventario
- **WHEN** the form is opened from the Inventario `+` button
- **THEN** `Nombre` is empty
