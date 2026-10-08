## ADDED Requirements

### Requirement: Preview action on the product detail form

The Product Detail form SHALL show a `Vista previa` button directly below the Tags field, above the cancel and save actions. The button SHALL be enabled only when the form holds a valid name and a valid price and the product has at least one registered photo, and SHALL be disabled while a save is in progress. While it is disabled for missing data, a hint below it SHALL state that a name, a price and at least one photo are needed. Tapping the enabled button SHALL open the product preview built from the form's current values — including edits that have not been saved — and the product's registered photos, without saving the product.

#### Scenario: Button shown below Tags

- **WHEN** the product has loaded and the detail form is displayed
- **THEN** a `Vista previa` button is shown below the Tags field

#### Scenario: Enabled for a loaded product with photos

- **WHEN** the loaded product has a valid name, a valid price and at least one registered photo
- **THEN** the `Vista previa` button is enabled

#### Scenario: Product with no photos

- **WHEN** the loaded product has no registered photos
- **THEN** the `Vista previa` button is disabled and the hint about the required name, price and photo is shown

#### Scenario: Disabled when the name or price becomes invalid

- **WHEN** the merchant clears the name or enters an invalid price
- **THEN** the `Vista previa` button becomes disabled

#### Scenario: Preview shows unsaved edits

- **WHEN** the merchant changes the price without saving and taps `Vista previa`
- **THEN** the product preview opens showing the new price, and no update request is issued

#### Scenario: Preview does not affect the save action

- **WHEN** the merchant opens and closes the preview on a pristine form
- **THEN** the save action stays disabled and leaving the screen shows no discard dialog

#### Scenario: Disabled while saving

- **WHEN** a save is in progress
- **THEN** the `Vista previa` button is disabled
