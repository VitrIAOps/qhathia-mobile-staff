## ADDED Requirements

### Requirement: Entry to a tablet's settings

Each tablet card in Dispositivos SHALL offer an "Ajustes de la tablet" action below its command buttons. It SHALL open the Ajustes de la tablet screen for that tablet. The action SHALL be available whether or not the tablet is connected, since settings are stored and applied when it next reports.

#### Scenario: Opening a tablet's settings
- **WHEN** the owner taps "Ajustes de la tablet" on a tablet's card
- **THEN** the Ajustes de la tablet screen opens for that tablet

#### Scenario: Disconnected tablet
- **WHEN** a tablet's card shows "Desconectada"
- **THEN** "Ajustes de la tablet" is still available
