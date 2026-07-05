## MODIFIED Requirements

### Requirement: Product registration entry point

The Inventory screen SHALL display a floating action button for registering a new product. Tapping it SHALL navigate to the Product Registration screen.

#### Scenario: Registration FAB is visible

- **WHEN** the Inventory screen is displayed
- **THEN** a floating action button for product registration is visible over the product list

#### Scenario: Registration FAB navigates

- **WHEN** the merchant taps the registration floating action button
- **THEN** the app navigates to the Product Registration screen

## ADDED Requirements

### Requirement: Inventory refresh after successful registration

After a product is successfully registered and the app returns to the Inventory screen, the inventory list SHALL refresh so the newly registered product can appear, and a success snackbar SHALL be shown using the design system's success (green) tones.

#### Scenario: Returning from a successful registration

- **WHEN** the merchant completes a product registration and returns to the Inventory screen
- **THEN** the inventory list re-queries its products and a green success snackbar confirms the registration

#### Scenario: Returning after a plan-limit rejection

- **WHEN** the merchant returns to the Inventory screen because registration was rejected for exceeding the plan's product limit
- **THEN** a message indicating the plan limit was exceeded is shown on the Inventory screen
