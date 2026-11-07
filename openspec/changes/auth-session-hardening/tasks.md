## 1. Network and token refresh (`core:network`, `core:data`)

- [x] 1.1 `KtorClientFactory`: `reAuthorizeOnResponse` that skips `401` with `errorCode` `CURRENT_PASSWORD_INCORRECT` (design decision 1); KDoc
- [x] 1.2 `AccountSecurityRepositoryImpl.toChangePasswordError` on the `HttpError`, by `errorCode` first
- [x] 1.3 `SessionAuthTokenProvider.refreshTokens` keeps the session when the refresh fails with `LoginError.Network` (design decision 2)
- [x] 1.4 Tests:
  - a MockEngine client built by `KtorClientFactory` answering `401 CURRENT_PASSWORD_INCORRECT` gets no refresh call and one request;
  - a plain `401` still refreshes;
  - repository maps `CURRENT_PASSWORD_INCORRECT` to `WrongCurrent` and another `401` to `Unknown`;
  - the provider keeps the session on a network failure and clears it on a rejected refresh

## 2. Push registration (`core:domain`, `core:data`, `core:testing`, `app`)

- [x] 2.1 `NetworkMonitor` port in `core:domain` (`isOnline: Flow<Boolean>`)
- [x] 2.2 `ConnectivityNetworkMonitor` in `core:data` (`callbackFlow`, default network callback, initial value from the active network), bound in `DataModule`
- [x] 2.3 `RegisterPushTokenUseCase`: inject `SessionRepository`; skip without a session; return `Result<Unit>` (design decision 5); update its tests
- [x] 2.4 `FakeNetworkMonitor` in `core:testing`
- [x] 2.5 `MainViewModel.ensurePushRegistered()` with up to 10 attempts, waiting for the network, cancelled on sign-out; update `MainViewModelTest` (registration once on sign-in; a failure is retried when the monitor reports online; sign-out cancels)

## 3. Shared UI (`core:ui`)

- [x] 3.1 `QhathiaTextField` eye description from `password_show_cd` / `password_hide_cd` (`values`, `values-en`)

## 4. Sign-in and fingerprint (`features:auth`)

- [x] 4.1 `LoginContent`: Done on the password hides the keyboard and submits
- [x] 4.2 `BiometricUnlockViewModel`: `BiometricGone` only flags; `signOut()` logs out, then clears the preference when gone (design decision 3); KDoc
- [x] 4.3 `BiometricUnlockViewModelTest`: replace "a removed enrollment clears the preference…" with "gone keeps the preference until continuing" and "continuing when gone signs out, then clears the preference" (order checked)

## 5. Seguridad, Cambiar contraseña, Datos personales (`features:profile`)

- [x] 5.1 `SecurityModels`: `SessionGroup`, `currentSession`, `otherGroups`, `pendingRevokeGroup`; events `RevokeGroupRequested`, `RevokeGroupDismissed`, `RevokeGroupConfirmed`
- [x] 5.2 `SecurityViewModel`: revoke a group one by one, then reload; error message on any failure; `onResumed` emits `security_biometric_disabled_gone` when it clears the preference
- [x] 5.3 `SecurityContent`: current session, then groups (`SessionGroupRow` with "Cerrar todas"); group confirmation dialog
- [x] 5.4 `ChangePasswordScreen`: `LabelledPasswordField(onImeAction)`; Done on the confirmation submits when `canSubmit`
- [x] 5.5 `PersonalDataScreen`: Done on the last field saves when `canSave`
- [x] 5.6 Strings `values` / `values-en`: `security_session_group` (`%1$s · %2$d sesiones`), `security_session_close_all`, `security_revoke_group_title` (`%1$d`, `%2$s`), `security_revoke_group_message`, `security_biometric_disabled_gone`
- [x] 5.7 `SecurityViewModelTest`: groups by label with the current first and newest groups first; a group of one stays a plain row; "Cerrar todas" revokes every id then reloads; a failure shows the error; the silent clear emits the message

## 6. Verification

- [x] 6.1 Run, one at a time: `:core:network:testDebugUnitTest`, `:core:domain:test`, `:core:data:testDebugUnitTest`, `:features:auth:testDebugUnitTest`, `:features:profile:testDebugUnitTest`, `:app:testDebugUnitTest`, then `testDebugUnitTest`
- [x] 6.2 Run `./gradlew lintDebug` and `./gradlew assembleDebug`

## 7. Manual — QA

- [ ] 7.1 Manual — S:LOG-071: wrong current password → nginx shows one `POST /auth/change-password 401` and no `POST /auth/refresh`; "La contraseña actual no es correcta" on field 1; Inventario still loads
- [ ] 7.2 Manual — S:LOG-049: fingerprint on → remove it in system settings → cold start → the notice with "Continuar" → Login; after signing in, Seguridad shows the switch off
- [ ] 7.3 Manual — X:XCO-040: several helper sessions → "Este equipo" and "qa-sim-tablet · N sesiones" with "Cerrar todas"; confirming sends N `DELETE /auth/sessions/{id}`, the group goes, the phone's session stays
- [ ] 7.4 Manual — S:EXP-001: `pm clear` → open → no `POST /notifications/tokens 401`; S:INI-050: force-stop → airplane mode → open → turn it off → `POST /notifications/tokens 204` within 30 s
- [ ] 7.5 Manual — S:LOG-017: credentials typed, keyboard ✓ → `POST /auth/login` and Inicio; Done also submits Cambiar contraseña and saves Datos personales
- [ ] 7.6 Manual — S:LOG-090: TalkBack on the eye in Login and Cambiar contraseña reads "Mostrar contraseña", then "Ocultar contraseña"
