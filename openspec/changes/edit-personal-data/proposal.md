## Why

Datos personales draws its three fields like inputs — label, bordered box, value — but none of them can be edited and there is no save action (`docs/handoff/android-pendientes.md` §5). It was built read-only on purpose while the backend had no way to change a profile; that has changed: `PUT /api/v1/users/me` accepts `fullName` and `phone` as a partial update and has been in production since Phase 18 (`docs/android/api-contract.md` §3, §18). Today a merchant who mistyped their name at sign-up, or changed phone number, has no way to fix it from the app, and a screen full of inputs that do not respond reads as broken — the same "control that lies" pattern as the rest of the triage.

## What Changes

- **Nombre completo** and **Teléfono** become editable fields, with a **Guardar cambios** button that is enabled only when something changed and the values are valid.
- **Correo** stays visible but disabled, with a line explaining why: it is the login credential, and the backend ignores it in the body. It is not sent.
- Client-side validation mirrors the backend: name 2–150 characters after trimming; phone exactly 9 digits starting with 9 (the `+51` is shown by the app, not typed). The phone input accepts digits only.
- A phone that is already set cannot be cleared: the update is partial, so an absent field is "unchanged", and there is no way to remove it. Emptying the field shows a validation error instead of silently keeping the old number.
- Saving sends only the fields that changed. On success the screen shows the saved values and confirms with a snackbar; the stored session's name is updated too, so Inicio's greeting and the Mi perfil header show the new name without signing in again.
- On failure, the typed values stay; a `422` reads as "check your data", a connectivity failure and anything else as their own messages.
- "Cambiar foto" stays *Próximamente* (there is still no avatar upload).

## Capabilities

### New Capabilities

_None._

### Modified Capabilities

- `merchant-profile`: the "Datos personales is read-only" requirement is replaced by one where name and phone are editable and saved through `PUT /api/v1/users/me`.

## Impact

- **`core:network`** — `UserApiService.updateMe(fullName, phone)` (`PUT /api/v1/users/me`, only non-null fields in the body); a request DTO.
- **`core:domain`** — `UserProfileRepository.updateProfile(fullName, phone)`; `UpdateProfileError` / `UpdateProfileException` (Validation, Network, Unknown); `UpdateUserProfileUseCase`, which also writes the new name into the stored session via `SessionRepository`. `UserProfile`'s "read-only in the app" KDoc is corrected.
- **`core:data`** — `UserProfileRepositoryImpl.updateProfile`, mapping `422` to Validation; tests.
- **`core:testing`** — `FakeUserProfileRepository` records updates and can fail.
- **`features:profile`** — `PersonalDataViewModel` (form state, validation, save), `PersonalDataScreen` (`QhathiaTextField`s, disabled email, save button, snackbar), strings (es/en), previews, tests.
- **No backend change.**
- **Archive order:** the `merchant-profile` main spec is created by the archive of `add-merchant-profile-settings`, on the `fix/inventory-snackbar-and-notification-badge` branch. Archive this change after that branch is merged.
- **Not in scope:** changing the email (needs a verified flow the backend does not have), avatar upload, store fields (name, WhatsApp, address — `PUT /stores/{id}`, OWNER only), and an "unsaved changes" prompt on back.
