# app-settings Specification

## Purpose
TBD - created by archiving change add-merchant-profile-settings. Update Purpose after archive.
## Requirements
### Requirement: Configuración screen layout

The Configuración screen SHALL show a gradient top bar with back and the title "Configuración", and a card containing:
- **Idioma**: a two-option selector, "Español" and "English", with the selected option filled in the primary colour.

The screen SHALL NOT show any price or tax setting. The card SHALL NOT depend on the store being loaded: if reading the store fails, the screen SHALL look and behave exactly as when it succeeds, with no error row or retry.

#### Scenario: Screen shown
- **WHEN** the merchant opens Configuración
- **THEN** the Idioma selector is shown and no IGV toggle is shown

#### Scenario: Store fails to load
- **WHEN** `GET /api/v1/stores/{storeId}` fails while Configuración is open
- **THEN** no error or retry is shown and the language selector still works

### Requirement: Language is owned by the device

The language preference SHALL be stored locally on the phone and SHALL be the only source for the selector. The value held by the store in the backend SHALL NOT overwrite it.

When no local value exists, the selector SHALL default to English if the device's system language is English, and to Español otherwise. That default SHALL be persisted the first time the screen is opened or the app starts.

#### Scenario: Local value wins over the backend
- **WHEN** the local preference is English and `GET /api/v1/stores/{storeId}` returns `language` "es"
- **THEN** the selector shows English

#### Scenario: No local value
- **WHEN** no local preference exists and the system language is Spanish
- **THEN** the selector shows Español

### Requirement: Changing the language applies the app locale

Selecting a language SHALL, in order:
1. Persist it locally.
2. Apply it as the app's per-app locale, so every screen of the app renders in that language, including after restart.
3. For OWNER and ADMIN, send `PUT /api/v1/stores/{storeId}` with `{ "language": "<es|en>" }` for the active store.

A failed backend update SHALL NOT revert the local language. The app SHALL retry sending the local value the next time Configuración is opened, whenever it differs from the store's `language`.

For SELLER, no backend request SHALL be sent.

#### Scenario: Owner switches to English
- **WHEN** an OWNER selects English
- **THEN** the app UI switches to English
- **AND** `PUT /api/v1/stores/{storeId}` is sent with `{ "language": "en" }`

#### Scenario: Backend update fails
- **WHEN** the language update request fails
- **THEN** the app remains in the selected language and no error blocks the merchant

#### Scenario: Seller switches language
- **WHEN** a SELLER selects English
- **THEN** the app UI switches to English and no store update is sent

#### Scenario: Preference survives restart
- **WHEN** the merchant selected English and restarts the app
- **THEN** the app starts in English

### Requirement: English translation of the app

Every user-facing string resource in every module SHALL have an English translation, so that selecting English renders no Spanish UI text. Content that comes from the backend, such as product names, SHALL be shown as received.

#### Scenario: Whole app in English
- **WHEN** English is selected
- **THEN** the bottom navigation, Inicio, Inventario, Solicitudes, Perfil and their sub-screens show English labels

