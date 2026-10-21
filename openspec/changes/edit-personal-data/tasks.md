## 1. Network

- [x] 1.1 `core:network` — add `UserApiService.updateMe(fullName: String?, phone: String?)`: `PUT api/v1/users/me` with a `buildJsonObject` body holding only the non-null arguments, returning `NetworkResult<UserProfileDto>`; update the class KDoc
- [x] 1.2 Update the `UserProfileDto` KDoc to cover `PUT` (same shape as `GET`)

## 2. Domain

- [x] 2.1 `core:domain` — add `UpdateProfileError` (`Validation`, `Network`, `Unknown(message)`) and `UpdateProfileException`, following `ChangePasswordError`
- [x] 2.2 Add `updateProfile(fullName: String?, phone: String?): Result<UserProfile>` to `UserProfileRepository`, documented as a partial update (null = unchanged; the email is not part of it)
- [x] 2.3 Add `UpdateUserProfileUseCase(userProfileRepository, sessionRepository)`: update, then on success copy the saved `fullName` into the stored session's `user`, ignoring a failure to persist it (design Decision 4)
- [x] 2.4 Fix `UserProfile`'s KDoc ("Read-only in the app" → name and phone are editable)
- [x] 2.5 `core:domain` tests for the use case: success updates the session name; failure leaves the session untouched; no session → still succeeds

## 3. Data and testing

- [x] 3.1 `core:data` — implement `UserProfileRepositoryImpl.updateProfile` with the mapping table of design Decision 3 (`422` → Validation, network → Network, other HTTP → Unknown)
- [x] 3.2 `core:data` tests: success maps the response; only non-null fields reach the service; `422`, network error and `500` map to their errors
- [x] 3.3 `core:testing` — `FakeUserProfileRepository`: record `updateProfile` calls, configurable `updateResult`, and on success make `profileResult` the updated profile
- [x] 3.4 `./gradlew :core:network:testDebugUnitTest :core:data:testDebugUnitTest :core:domain:test`

## 4. Datos personales

- [x] 4.1 `features:profile` — add `PersonalDataValidation` (name 2..150 after trim; phone `^9\d{8}$`; empty phone valid only when the profile had none) and its unit tests
- [x] 4.2 `PersonalDataViewModel` — form state, derived errors (shown only once a field differs from the loaded value), `isDirty`, `canSave`, events `NameChanged` / `PhoneChanged` (digits only, max 9) / `Save` / `Retry`, `Saved` effect; reset the fields from the profile on load and after a save; map `UpdateProfileError` to banner strings (design Decision 2)
- [x] 4.3 Strings (es/en): save button, saved snackbar, email note, phone helper, name error, phone error, banner errors (review data / connectivity / generic)
- [x] 4.4 `PersonalDataScreen` — replace `ReadOnlyField` with `QhathiaTextField`s (name: `KeyboardCapitalization.Words`; email: `enabled = false` with the note; phone: `KeyboardType.Phone`, "+51" shown as part of the field, digits only), the banner, a full-width **Guardar cambios** button with progress, and a snackbar on `Saved`; delete `ReadOnlyField`; update the KDoc
- [x] 4.5 Previews: loaded, dirty with a field error, saving, banner error — light and dark for the loaded state; keep loading and error previews
- [x] 4.6 `PersonalDataViewModelTest`: loads into the fields; save disabled until dirty and valid; sends only the changed field, name trimmed; email never sent; success emits `Saved` and resets the form to the saved profile; `422` / network / unknown keep the typed values and set the right banner; clearing an existing phone blocks save; empty phone with no stored phone is fine
- [x] 4.7 `./gradlew :features:profile:testDebugUnitTest :features:profile:lintDebug`

## 5. Docs, memory and verification

- [x] 5.1 Mark the "Editar datos personales" row of §5 as done in `docs/handoff/android-pendientes.md`
- [x] 5.2 Update the `profile-backend-gaps` memory: Datos personales is no longer read-only by decision
- [x] 5.3 Full build: `./gradlew testDebugUnitTest lintDebug assembleDebug`
- [ ] 5.4 Manual — against production: change the name and the phone, see the snackbar, go back: Mi perfil and the Inicio greeting show the new name; invalid phone and short name block the button; email cannot be edited; airplane mode shows the connectivity message and keeps the typed values; light and dark, large font
