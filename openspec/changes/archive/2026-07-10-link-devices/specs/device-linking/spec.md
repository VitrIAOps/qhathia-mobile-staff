## ADDED Requirements

### Requirement: Entry point to device linking from Profile

The system SHALL provide an entry point in the Profile section that opens the Linked Devices screen. Opening the Linked Devices screen SHALL hide the bottom navigation bar so the screen is presented full-screen.

#### Scenario: Owner opens linked devices from Profile

- **WHEN** the owner taps the link-terminal action in the Profile section
- **THEN** the app navigates to the Linked Devices screen
- **AND** the bottom navigation bar is not shown on that screen

#### Scenario: Owner returns from linked devices

- **WHEN** the owner navigates back from the Linked Devices screen
- **THEN** the app returns to the Profile section with the bottom navigation bar visible

### Requirement: List of linked devices

The Linked Devices screen SHALL display the tablets paired to the owner's store, retrieved from `GET /api/v1/devices`. The screen SHALL show a bold title "Dispositivos vinculados" aligned to the top-left. Each listed device SHALL show at least its name and status.

#### Scenario: Store has paired devices

- **WHEN** the Linked Devices screen loads and the store has one or more paired devices
- **THEN** the screen shows the "Dispositivos vinculados" title
- **AND** lists each paired device with its name and status

#### Scenario: Loading the list

- **WHEN** the Linked Devices screen is loading the devices
- **THEN** the screen shows a loading indicator until the result arrives

#### Scenario: Loading the list fails

- **WHEN** loading the device list fails
- **THEN** the screen shows an error state that allows the owner to retry

### Requirement: Empty state

When the store has no paired devices, the Linked Devices screen SHALL show a message indicating there are no terminals yet and inviting the owner to use the + option to add one.

#### Scenario: Store has no paired devices

- **WHEN** the Linked Devices screen loads and the store has no paired devices
- **THEN** the screen shows an empty-state message inviting the owner to tap + to add a terminal
- **AND** the floating + button remains available

### Requirement: Pair a new device via code

The Linked Devices screen SHALL provide a floating + button that opens a near-full-screen dialog containing a single code input and a submit button. On submit, the system SHALL pair the device by calling `POST /api/v1/devices/pair` with `decision = APPROVE`, the entered pairing code, and the required device metadata (name, platform `ANDROID`, app version).

#### Scenario: Owner opens the pairing dialog

- **WHEN** the owner taps the floating + button
- **THEN** a near-full-screen dialog opens with a code input and a submit button

#### Scenario: Successful pairing

- **WHEN** the owner enters a valid pairing code and submits
- **AND** the pairing request succeeds
- **THEN** the dialog closes
- **AND** the device list is reloaded

#### Scenario: Failed pairing

- **WHEN** the owner submits and the pairing request fails
- **THEN** the dialog stays open and shows an error message
- **AND** the owner can correct the code and submit again

#### Scenario: Submit disabled without a code

- **WHEN** the code input is empty
- **THEN** the submit action is disabled
