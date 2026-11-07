## 1. Shared UI (`core:ui`)

- [x] 1.1 `QhathiaDiscardChangesDialog(onConfirm, onDismiss)` with `discard_changes_title`, `discard_changes_message`, `discard_changes_confirm`, `discard_changes_dismiss` in `values` / `values-en` (same texts as Detalle) and a preview (design decision 1)
- [x] 1.2 `QhathiaSwitchRow(comingSoon: Boolean = false)`: no `Switch`, no `toggleable`, `RowTexts(comingSoon = true)`, `clearAndSetSemantics { contentDescription = coming_soon_cd(title); disabled() }`; KDoc (design decision 4)

## 2. Notification preferences data (`core:network`, `core:domain`, `core:data`, `core:testing`) — F-68

- [x] 2.1 `NotificationPreferencesDto`: `supported: Map<String, Boolean> = emptyMap()`, `supportedCount: Int? = null`, `supportedActiveCount: Int? = null`, `orderExpiringWarningMinutes: Int = 10`
- [x] 2.2 `NotificationPreferences`: `supported: Set<NotificationPreference> = DEFAULT_SUPPORTED`, `orderExpiringWarningMinutes: Int = 10`, `isSupported()`, derived `supportedActiveCount` / `supportedCount`, `DEFAULT_SUPPORTED = {NEW_ORDER, ORDER_EXPIRING}` (design decision 3)
- [x] 2.3 `ProfileMappers`: supported set from `supported[fieldName] == true`, empty → `DEFAULT_SUPPORTED`; minutes
- [x] 2.4 `TestFixtures.notificationPreferences`: `supported` and `orderExpiringWarningMinutes` parameters
- [x] 2.5 Mapper tests (in `core:data`): `supported` with `lowBattery` true gives the three; an empty map gives `DEFAULT_SUPPORTED`; minutes are read

## 3. Notificaciones and the hub count (`features:profile`) — F-68

- [x] 3.1 `NotificationPreferencesScreen.ToggleGroupSection`: `comingSoon = !isSupported`; no subtitle when unsupported; the expiring subtitle from the plural `notifications_order_expiring_subtitle` with `orderExpiringWarningMinutes`; update the preview
- [x] 3.2 Strings `values` / `values-en`: replace `notifications_order_expiring_subtitle` with a plural ("Aviso %1$d minuto(s) antes de que venza" / "Alert %1$d minute(s) before it expires")
- [x] 3.3 `ProfileHubViewModel.loadAlertCounts`: `AlertCounts(prefs.supportedActiveCount, prefs.supportedCount)`
- [x] 3.4 Tests: `NotificationPreferencesViewModelTest` keeps `supported` and minutes from the load; `ProfileHubViewModelTest` "alert counts use supported only" ("2 de 2", then "1 de 2" with `NEW_ORDER` off)

## 4. Datos personales (`features:profile`) — F-67, F-70

- [x] 4.1 `PersonalDataValidation.isValidPhone(phone)`: empty or a 9-digit mobile starting with 9; drop `storedPhone`; KDoc (design decision 2)
- [x] 4.2 `PersonalDataUiState`: `isRemovingPhone`, `isDiscardDialogVisible`; events `BackRequested`, `DiscardConfirmed`, `DiscardDismissed`; effect `NavigateBack` (design decision 1)
- [x] 4.3 `PersonalDataScreen`: `BackHandler(enabled = isDirty || isSaving)` and the top bar's back → `BackRequested`; `NavigateBack` → `onBack()`; the dialog; helper text "Se quitará tu celular al guardar" when `isRemovingPhone`
- [x] 4.4 Strings `values` / `values-en`: `personal_phone_hint` "Celular de 9 dígitos (opcional)" / "9-digit mobile (optional)", `personal_phone_will_remove` "Se quitará tu celular al guardar" / "Your mobile will be removed when you save"
- [x] 4.5 Tests:
  - `PersonalDataValidationTest`: an empty phone is valid;
  - `PersonalDataViewModelTest`: replace "an existing phone cannot be cleared" with "clearing a stored phone enables save and sends an empty string" (`Update(fullName = null, phone = "")`, then the field stays empty after `phone = null`);
  - "back when dirty shows the discard dialog"; "keep editing keeps the values"; "discard navigates back"; "back with no changes navigates back"; "back while saving does nothing"

## 5. Cambiar contraseña (`features:profile`) — F-70

- [x] 5.1 `ChangePasswordUiState`: `isDirty` (any field non-empty), `isDiscardDialogVisible`; events `BackRequested`, `DiscardConfirmed`, `DiscardDismissed`; effect `NavigateBack`; back ignored while `isSubmitting`
- [x] 5.2 `ChangePasswordScreen`: `BackHandler(enabled = isDirty || isSubmitting)`, top bar's back → `BackRequested`, `NavigateBack` → `onBack()`, the dialog
- [x] 5.3 `ChangePasswordViewModelTest`: "back when empty navigates back"; "back when typed shows the dialog"; "keep editing keeps the typed values"; "back while submitting does nothing"

## 6. Mi perfil loads once (`features:profile`) — F-75 (Perfil)

- [x] 6.1 `ProfileHubUiEvent`: `Refresh` → `Resumed` and `Retry`; the error panes send `Retry`, `LifecycleResumeEffect` sends `Resumed`
- [x] 6.2 `ProfileHubViewModel`: no refresh in `init`; `lastRefreshedAt`, `lastOpened`; `Resumed` reloads after `PERSONAL_DATA` / `NOTIFICATIONS` / `DEVICES` or when 30 s have passed (or never loaded); `Retry` always reloads; KDoc (design decision 5)
- [x] 6.3 `ProfileHubViewModelTest`: existing tests send `Resumed` first; "nothing loads before the first resume"; "a second resume within 30 s loads nothing"; "a resume after 30 s reloads"; "back from Notificaciones reloads at once"; "back from Ayuda within 30 s does not"; "retry always reloads"

## 7. Verification

- [x] 7.1 Run, one at a time: `:core:ui:testDebugUnitTest`, `:core:data:testDebugUnitTest`, `:features:profile:testDebugUnitTest`, then `testDebugUnitTest`
- [x] 7.2 Run `./gradlew lintDebug` and `./gradlew assembleDebug`

## 8. Manual — QA

- [ ] 8.1 Manual — S:PER-030 / S:LOG-077: type in Datos personales and in Cambiar contraseña → Atrás (system and chevron) → "¿Descartar cambios?"; "Seguir editando" keeps the text, "Descartar" goes back; untouched forms go back at once
- [ ] 8.2 Manual — S:EXP-021: empty the phone → "Se quitará tu celular al guardar", Guardar enabled → `PUT /users/me` with `"phone":""` → 200; re-entering shows it empty and `GET /users/me` gives `phone: null`. S:PER-026: with no phone, changing only the name sends only `fullName`
- [ ] 8.3 Manual — S:PER-045: "Aviso 10 minutos antes de que venza"; the five other rows "Próximamente" with no switch, announced "Tablet desconectada, próximamente" disabled; the hub "2 de 2 alertas activas", "1 de 2" after turning off "Nuevo pedido"
- [ ] 8.4 Manual — S:PER-004: opening Perfil sends one `GET /users/me`; back from Ayuda sends nothing; back from Notificaciones after a toggle reloads
