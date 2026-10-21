## Context

- **Screen.** `PersonalDataScreen` renders a `LoadState<UserProfile>`: loading pane, error pane with retry, or a body with the avatar card and three private `ReadOnlyField`s (a bordered `Box` that looks like an input). `PersonalDataViewModel` only loads, via `GetUserProfileUseCase`.
- **Data.** `UserApiService.getMe()` → `UserProfileDto` → `UserProfile`. `UserProfileRepository` has only `getProfile()`; `UserProfileRepositoryImpl` maps with the shared `asDomainResult`, which turns every HTTP error into a plain `IllegalStateException` (the status code is lost).
- **Contract.** `PUT /api/v1/users/me` takes `fullName` (2–150) and `phone` (`^9\d{8}$`), partial: absent fields are not touched. Invalid → `422`. `email` in the body is ignored. Response = same shape as `GET`.
- **Name elsewhere.** Inicio's greeting reads `Session.user.fullName` (persisted at login). Mi perfil reloads the profile on every `ON_RESUME`, so it picks up a change on its own; the session does not.
- **Precedent.** Cambiar contraseña (`ChangePasswordViewModel`) is the app's form pattern: field values in the UI state, field errors as `@StringRes`, a banner for errors that belong to no field, `canSubmit` derived in the state, a domain error type carried by an exception (`ChangePasswordException`), a one-shot effect on success. `QhathiaTextField` provides `enabled`, `isError`, `errorText`, `helperText`, `keyboardOptions`.

Modules involved are all existing: `core:network` → `core:data` → `core:domain` ← `features:profile`. No new module, no dependency change.

## Goals / Non-Goals

**Goals:**
- Name and phone editable with the backend's own rules, checked before sending.
- Only changed fields sent; the email never.
- After a save, every place that shows the name agrees.

**Non-Goals:**
- Email change, avatar upload, store data.
- A confirmation prompt for unsaved changes on back (changes are simply discarded, like any unsaved form in the app today).
- An explicit "edit mode" toggle.

## Decisions

### 1. Always-editable form with a dirty-aware save button

The fields are editable as soon as the profile loads; **Guardar cambios** is enabled only when `isDirty && isValid && !isSaving`. Alternatives considered: an "Editar" toggle (one more tap for no safety gain on two fields) and auto-save on blur (surprising, and a failed save has nowhere obvious to report). The button sits at the bottom of the fields card, full width, like Cambiar contraseña's submit.

### 2. Form state in the ViewModel, validated as you type

```kotlin
data class PersonalDataUiState(
    val profile: LoadState<UserProfile> = LoadState.Loading,  // the last saved values
    val fullName: String = "",
    val phone: String = "",          // digits only, ≤ 9
    val isSaving: Boolean = false,
    @StringRes val bannerErrorRes: Int? = null,
) {
    val nameErrorRes: Int?    // null when valid
    val phoneErrorRes: Int?
    val isDirty: Boolean      // trimmed name != profile.fullName || phone != (profile.phone ?: "")
    val canSave: Boolean
}
```

Errors are derived, not stored, so they always match the current text. A field shows its error only once it differs from the loaded value — the screen does not open in red for a legacy profile whose stored values break today's rules. When the profile loads (first load or after a save), the fields are reset from it. The phone input filters to digits and caps at 9 (`filter(Char::isDigit).take(9)`), with `KeyboardType.Phone`; pasting "+51 999 888 777" therefore yields "519998887" — acceptable, and the error makes it obvious. The name uses `KeyboardCapitalization.Words`.

Validation rules live in one internal `PersonalDataValidation` object in the feature (name 2..150 after trim; phone `^9\d{8}$`; empty phone valid only when the profile had none), unit-tested on its own.

### 3. A typed update error; `422` is the only status that matters

`UserProfileRepositoryImpl.updateProfile` maps the `NetworkResult` itself instead of `asDomainResult`, so it can read the code:

| Result | Domain |
|---|---|
| Success | `Result.success(dto.toDomain())` |
| `HttpError(422)` | `UpdateProfileException(UpdateProfileError.Validation)` |
| `NetworkError` | `UpdateProfileException(UpdateProfileError.Network)` |
| any other `HttpError` | `UpdateProfileException(UpdateProfileError.Unknown("HTTP $code"))` |

The client validates first, so a `422` should be rare (rules drifting on the server); it maps to a banner asking to review the data rather than to a field, because the problem detail does not say which field failed.

### 4. The session's name is updated by the use case

`UpdateUserProfileUseCase(userProfileRepository, sessionRepository)`: on success, if a session exists, it saves `session.copy(user = session.user.copy(fullName = saved.fullName))`. Doing it in the domain use case (not the ViewModel) keeps "the session reflects the profile" a business rule any caller gets. Only the name is copied: `User` has no phone. A failure to persist the session does not fail the save — the server already has the new name and the next login refreshes it — so the use case ignores it.

### 5. Request body built from non-null arguments

`UserApiService.updateMe(fullName: String?, phone: String?)` builds the JSON with `buildJsonObject`, adding only non-null fields — the same approach as `StoreApiService.updateStore`. No `email` parameter exists, so it cannot be sent by mistake.

## Risks / Trade-offs

- **Phone cannot be removed** → a limitation of the partial-update contract, surfaced as a validation error instead of a silent no-op. If the backend adds explicit `null` support, the empty-phone rule is the only thing to change.
- **Unsaved edits are lost on back** → accepted for two short fields; noted as a non-goal.
- **Legacy values breaking today's rules** (e.g. a 1-character name) → not flagged until the merchant edits that field (Decision 2), so the screen never opens in an error state the merchant did not cause.
- **Session write after save** races nothing: `saveSession` replaces the stored session, and `observeSession()` consumers (Inicio) simply re-emit.
