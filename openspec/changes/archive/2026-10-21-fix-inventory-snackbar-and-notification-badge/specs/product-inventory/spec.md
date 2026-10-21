## ADDED Requirements

### Requirement: Result snackbar is shown once per result

When the Inventory screen receives a result from registration or from Product Detail (registered, registered but discount failed, plan limit exceeded, product updated), the system SHALL treat that result as consumed as soon as it starts handling it, before waiting for its snackbar to be dismissed. Leaving the Inventory screen while that snackbar is visible SHALL NOT cause the same result, or its snackbar, to be handled again when the merchant returns. The `Ver producto` action of the registration snackbars SHALL keep working after the result has been consumed, using the product id that arrived with the result.

#### Scenario: Navigating away while the success snackbar is visible

- **WHEN** the registration success snackbar is visible and the merchant opens a product's detail and then comes back to Inventory
- **THEN** the success snackbar is not shown again and the list is not re-queried because of that registration

#### Scenario: Switching tabs while the product-updated snackbar is visible

- **WHEN** the product-updated snackbar is visible and the merchant switches to another bottom-nav tab and back to Inventory
- **THEN** the product-updated snackbar is not shown again

#### Scenario: Every result kind is consumed before its snackbar

- **WHEN** the Inventory screen handles a registered, discount-failed, plan-limit or product-updated result
- **THEN** the result is cleared before its snackbar is awaited, so leaving the screen during the snackbar leaves no result pending

#### Scenario: Ver producto still works after the result is consumed

- **WHEN** the registration success snackbar is shown and the merchant taps `Ver producto`
- **THEN** the list scrolls to the registered product's row, exactly as before this change
