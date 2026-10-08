## ADDED Requirements

### Requirement: Preview action on the registration form

The Product Registration form SHALL show a `Vista previa` button directly below the Tags field, above the cancel and submit actions. The button SHALL be enabled only when the form holds a valid name, a valid price and at least one image, and SHALL be disabled while registration is in progress. While it is disabled for missing data, a hint below it SHALL state that a name, a price and at least one photo are needed. Tapping the enabled button SHALL open the product preview built from the form's current values, without registering the product.

#### Scenario: Button shown below Tags

- **WHEN** the registration form is displayed
- **THEN** a `Vista previa` button is shown below the Tags field

#### Scenario: Disabled on an empty form

- **WHEN** the registration form has just opened
- **THEN** the `Vista previa` button is disabled and the hint about the required name, price and photo is shown

#### Scenario: Enabled with the minimum data

- **WHEN** the merchant has entered a valid name and price and added at least one image, with no category, description, stock or tags
- **THEN** the `Vista previa` button is enabled and the hint is hidden

#### Scenario: Disabled again when a photo is removed

- **WHEN** the merchant removes the only image
- **THEN** the `Vista previa` button becomes disabled

#### Scenario: Opening the preview

- **WHEN** the merchant taps the enabled `Vista previa` button
- **THEN** the product preview opens showing the entered name, price and selected images, and no registration request is issued

#### Scenario: Preview does not require a submittable form

- **WHEN** the form has a name, a price and an image but no category, so the submit action is disabled
- **THEN** the `Vista previa` button is still enabled

#### Scenario: Disabled while submitting

- **WHEN** registration is in progress
- **THEN** the `Vista previa` button is disabled
