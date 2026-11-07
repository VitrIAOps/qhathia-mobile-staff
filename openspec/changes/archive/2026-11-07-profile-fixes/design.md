## Context

**F-70.** `PersonalDataScreen` and `ChangePasswordScreen` pass `onBack` straight to `QhathiaGradientTopBar` and register no `BackHandler`. `PersonalDataUiState.isDirty` exists; `ChangePasswordUiState` has none. Detalle (`ProductDetailScreen.kt:52`, `ProductDetailContent.kt:421-437`) and Registro already guard with their own `DiscardChangesDialog` and strings (`detail_discard_*`, `register_discard_*`).

**F-67.** `PersonalDataValidation.isValidPhone(phone, storedPhone)` rejects an empty phone when one was stored. The ViewModel already sends `""` when the field is emptied and `UserApiService` puts it in the JSON; only the validation blocks it. Backend §36: `""` clears the phone (`200`, `phone: null`), `null` or absence leaves it.

**F-68.** `NotificationPreferencesDto` declares the seven booleans plus `activeCount` / `totalCount`; Fase 42 also returns `supported` (per field), `supportedCount`, `supportedActiveCount` and `orderExpiringWarningMinutes`, which `ignoreUnknownKeys` drops today. `ToggleGroupSection` always draws `QhathiaSwitchRow`, which has no "Próximamente" mode (`QhathiaNavigationRow` does). The hub builds `AlertCounts(activeCount, totalCount)`.

**F-75 (Perfil).** `ProfileHubViewModel.init` calls `refresh()` and `ProfileHubScreen`'s `LifecycleResumeEffect` sends `Refresh` on every resume, so opening Mi perfil loads twice (the in-flight guard catches some, not all) and every return reloads profile, alerts, subscription and tablets. Dispositivos is another module, reached through `onNavigateToDevices`.

Modules (no new module; graph unchanged):

```
features:profile ──► core:ui (QhathiaDiscardChangesDialog, QhathiaSwitchRow)
       │          └► core:domain (NotificationPreferences) ◄── core:data (mapper) ──► core:network (DTO)
       └── tests ──► core:testing (TestFixtures)
```

## Goals / Non-Goals

**Goals:**
- No typing is lost to an accidental back.
- A phone can be removed.
- Notificaciones and the hub promise only what the backend sends, and follow it when that changes.
- Mi perfil makes one round of requests per visit, and reloads only when coming back can have changed something.

**Non-Goals:**
- Moving Detalle and Registro to the shared dialog (they are touched by changes 1 and 2; a later cleanup).
- Inicio's duplicate pending-count query (change 9).
- Senders for the five unsupported preferences (backend, blocked).

## Decisions

1. **One discard dialog in `core:ui`.** `QhathiaDiscardChangesDialog(onConfirm, onDismiss)` with `discard_changes_title`, `_message`, `_confirm`, `_dismiss` in `core:ui` `values` / `values-en`, the same texts as Detalle. Both screens follow Detalle's MVI shape: state `isDiscardDialogVisible`; events `BackRequested`, `DiscardConfirmed`, `DiscardDismissed`; effect `NavigateBack`. `BackRequested` is ignored while saving or submitting, shows the dialog when dirty, and emits `NavigateBack` otherwise. The screen uses `BackHandler(enabled = isDirty || isSaving)` and sends the top bar's back through `BackRequested` too, so both paths behave the same.
   - *Why the ViewModel and not a screen-local flag?* The decision depends on `isDirty` / `isSaving`, which live in the state, and it can be unit-tested.
   - *`isDirty` for Cambiar contraseña* is "any field non-empty": nothing is loaded, so any typing is a change.

2. **An empty phone is always valid.** `isValidPhone(phone)` = empty or a 9-digit mobile starting with 9; the `storedPhone` parameter goes. The state gains `isRemovingPhone` (saved phone present, field empty) to show "Se quitará tu celular al guardar" as the field's helper text in place of the hint. The save keeps sending `""` for an emptied phone and nothing for an unchanged one.

3. **`supported` is a set in the domain, with a safe default.** The mapper takes the fields whose `supported[fieldName] == true`; an empty or missing map falls back to `{NEW_ORDER, ORDER_EXPIRING}`, the two the backend has always sent. `supportedActiveCount` and `supportedCount` are derived from `values` and `supported` in the domain, so an optimistic toggle updates them without the server's counts. `orderExpiringWarningMinutes` defaults to 10.
   - *Why derive instead of using the server's `supportedActiveCount`?* The screen and the hub already work from `values`; deriving keeps a single source after an optimistic toggle.

4. **`QhathiaSwitchRow(comingSoon)`.** With `comingSoon`, the row draws `RowTexts(comingSoon = true)` (the "Próximamente" pill), no `Switch`, no `toggleable`, and `clearAndSetSemantics { contentDescription = coming_soon_cd(title); disabled() }`, as `QhathiaNavigationRow` does. The screen passes `comingSoon = !supported`, drops the subtitle of unsupported rows (so "Cada lunes por WhatsApp" goes), and reads the expiring subtitle from a plural with the minutes.

5. **The hub loads on resume, with a 30-second window, forced after a data screen.** `init` no longer refreshes. `Refresh` becomes `Resumed` (from `LifecycleResumeEffect`) and `Retry` (from the error panes). The ViewModel stores `lastRefreshedAt` (set when a refresh starts) and the destination it last opened (`Open`). On `Resumed`: if the last opened destination is `PERSONAL_DATA`, `NOTIFICATIONS` or `DEVICES`, it reloads and forgets it; otherwise it reloads only when 30 seconds have passed since the last refresh (or there was none). `Retry` always reloads. The in-flight guard stays.
   - *Why not a "changed" flag in the previous back-stack entry, as the handoff suggests?* Dispositivos is another module (also changed by change 5) and would need its own plumbing; the hub already knows where it sent the merchant. Returning from one of those screens without changing anything costs one reload, which is acceptable.
   - *Why `Open` and not the navigation result?* `Open` is the event the hub already handles for every menu row, so no new navigation wiring is needed.

## Risks / Trade-offs

- [The backend drops `supported`] → the default set keeps "Nuevo pedido" and "Pedido por expirar" usable and the rest marked "Próximamente", which is the truth today.
- [A back gesture during the save] → ignored while saving; the result returns the merchant to a clean form, so a later back leaves at once.
- [Data changed elsewhere within 30 s (another device)] → shown on the next visit after the window; the hub is a summary, and each sub-screen loads its own data.
- [Merging with change 5] → `QhathiaSettingsRows.kt` and `features/profile` `strings.xml` changed there on other lines (the pill's `maxLines`, the devices plurals); git should merge them without help.

## Migration Plan

Ships with the app, no data migration. Rollback is reverting the change.

## Open Questions

_None._
