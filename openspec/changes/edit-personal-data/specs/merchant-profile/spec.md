## REMOVED Requirements

### Requirement: Datos personales is read-only

**Reason**: `PUT /api/v1/users/me` now updates `fullName` and `phone` (api-contract §3, §18). A screen that draws inputs which cannot be edited reads as broken (handoff §5).

**Migration**: Replaced by "Edit personal data". Nothing to migrate: the same fields are shown, and the email stays non-editable.

## ADDED Requirements

### Requirement: Edit personal data

The Datos personales screen SHALL show, from `GET /api/v1/users/me`:
- A gradient top bar with back and the title "Datos personales".
- A card with the initials avatar and a "Cambiar foto" action marked *Próximamente*.
- A card with the fields "Nombre completo", "Correo" and "Teléfono", and a **Guardar cambios** button.

"Nombre completo" and "Teléfono" SHALL be editable. "Correo" SHALL be shown disabled, with a note that it cannot be changed because it is the account's login, and SHALL never be sent. The phone SHALL be edited as its 9 digits, with the `+51` prefix shown by the app; the field SHALL accept digits only and at most 9 of them.

The name is valid when, after trimming, it has 2 to 150 characters. The phone is valid when it has exactly 9 digits and starts with 9. A phone that was already set SHALL NOT be cleared: an empty phone is invalid when the profile had one, and valid (meaning "still none") when it had none. Invalid values SHALL show an error under their field.

**Guardar cambios** SHALL be enabled only when at least one editable value differs from the loaded profile, every value is valid, and no save is in progress. Saving SHALL send `PUT /api/v1/users/me` with only the changed fields, the name trimmed. While saving, the fields and the button SHALL be disabled and the button SHALL show progress.

On success, the screen SHALL show the values returned by the server, confirm with a snackbar, and update the stored session's name, so that the Inicio greeting and the Mi perfil header use the new name without signing in again. On failure, the typed values SHALL be kept and a message SHALL be shown: one asking to review the data for a `422`, one about connectivity for a network failure, and a generic one otherwise.

The screen SHALL show a loading state while fetching and an error state with retry when the profile cannot be loaded.

#### Scenario: Fields displayed
- **WHEN** the profile has `fullName` "Carla Mendoza", `email` "carla.mendoza@gmail.com" and `phone` "999888777"
- **THEN** the name field shows "Carla Mendoza", the email "carla.mendoza@gmail.com" disabled, and the phone field "999888777" with the "+51" prefix
- **AND** Guardar cambios is disabled

#### Scenario: Save a new name
- **WHEN** the merchant changes the name to "  Carla Mendoza Ruiz " and taps Guardar cambios
- **THEN** `PUT /api/v1/users/me` is sent with `{ "fullName": "Carla Mendoza Ruiz" }` and no other field
- **AND** the screen shows the saved name, a confirmation snackbar appears, and Inicio greets "Carla"

#### Scenario: Save a new phone
- **WHEN** the merchant changes the phone to "987654321" and taps Guardar cambios
- **THEN** `PUT /api/v1/users/me` is sent with `{ "phone": "987654321" }` and no other field

#### Scenario: Invalid phone
- **WHEN** the merchant types "88765432"
- **THEN** the phone field shows an error asking for 9 digits starting with 9, and Guardar cambios is disabled

#### Scenario: Name too short
- **WHEN** the merchant leaves the name as "C"
- **THEN** the name field shows an error and Guardar cambios is disabled

#### Scenario: Clearing an existing phone
- **WHEN** the profile had a phone and the merchant empties the field
- **THEN** the phone field shows an error and Guardar cambios is disabled

#### Scenario: No phone yet
- **WHEN** the profile has no phone and the merchant only changes the name
- **THEN** the empty phone field shows no error and the save sends only `fullName`

#### Scenario: Email is never sent
- **WHEN** any save is sent
- **THEN** its body contains no `email` field

#### Scenario: Server rejects the data
- **WHEN** the save fails with `422`
- **THEN** the typed values stay and a message asks the merchant to review the data

#### Scenario: Offline save
- **WHEN** the save fails for lack of connectivity
- **THEN** the typed values stay and a connectivity message is shown

#### Scenario: Change photo not available
- **WHEN** the merchant taps "Cambiar foto"
- **THEN** nothing is uploaded and the action is presented as *Próximamente*
