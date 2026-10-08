## ADDED Requirements

### Requirement: Search clear action

The order search box SHALL show a trailing clear (`x`) button whenever it contains text. Tapping it SHALL remove all the entered text, which restores the list for the active status filter. The button SHALL NOT be shown while the search box is empty.

#### Scenario: Clear button appears with text

- **WHEN** the merchant has typed text in the search box
- **THEN** a clear button is shown at the end of the search box

#### Scenario: Clearing the text

- **WHEN** the merchant taps the clear button
- **THEN** the search box becomes empty, the clear button disappears and the orders are re-queried with the active status filter and no search text
