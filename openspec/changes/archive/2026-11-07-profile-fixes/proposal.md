## Why

The 2026-11-03 QA regression (`docs/handoff/android-regresion-2026-11-03.md` §3.1 and §3.2) finds four problems in Perfil. The backend already does its part (Fase 36 and 42):

- **F-70** — Datos personales and Cambiar contraseña leave without asking. With the phone changed, or the three password fields typed, Atrás or the chevron goes straight back and the typing is lost (S:PER-030, S:LOG-077).
- **F-67** — A saved phone cannot be removed. Emptying the field shows "Ingresa un celular de 9 dígitos…" and disables Guardar, although `PUT /users/me {"phone": ""}` now answers `200` with `phone: null` (S:EXP-021).
- **F-68** — Notificaciones promises what nothing sends. "Pedido por expirar" says "Aviso cuando queden 2 min" (the backend warns 10 minutes before), five toggles have no sender yet but show a working switch, "Reporte semanal" says "Cada lunes por WhatsApp", and the hub reads "7 de 7 alertas activas" (S:PER-045). The backend now says which preferences it supports and how early it warns.
- **F-75 (Perfil part)** — Mi perfil loads twice on opening (once in `init`, once on the first resume) and reloads all four blocks on every return, even from Ayuda (S:PER-004). Inicio's duplicate `GET /orders?…size=1` (the other part of F-75) moves to change 9, `home-stats-fixes`, because it sits on the pending-count code of change 3.

## What Changes

- **"¿Descartar cambios?" (F-70).** With unsaved changes, Atrás and the top bar's back in Datos personales and Cambiar contraseña ask "¿Descartar cambios?". "Seguir editando" keeps the text; "Descartar" goes back. With no changes they go back at once; while saving, back does nothing. The dialog moves to `core:ui` as a shared component (Detalle and Registro keep theirs for now).
- **Remove the phone (F-67).** An empty phone is valid. Emptying a saved phone enables Guardar, shows "Se quitará tu celular al guardar" under the field, and sends `"phone": ""`. The hint reads "Celular de 9 dígitos (opcional)".
- **Only what the backend sends (F-68).** The preferences read `supported` and `orderExpiringWarningMinutes`. "Pedido por expirar" reads "Aviso 10 minutos antes de que venza" from that value. A preference the backend does not support yet shows "Próximamente" with no switch and no subtitle, and is announced as "…, próximamente", disabled. The hub counts only supported alerts: "2 de 2 alertas activas". Values of unsupported preferences are still stored and sent, so one turns on by itself when the backend supports it.
- **Mi perfil loads once (F-75).** The first resume loads; `init` no longer does. A resume within 30 seconds of the last load does nothing, except when returning from a screen that changes what the hub shows (Datos personales, Notificaciones, Dispositivos), which always reloads. Retry always reloads.

## Capabilities

### New Capabilities

_None._

### Modified Capabilities

- `merchant-profile`: "Edit personal data" (removing the phone; the discard dialog; Done saves, as change 6 implemented), "Menu rows show live subtitles" (supported alerts only), "Mi perfil hub behind the Perfil tab" (loads once, reloads on return from a screen that changes it).
- `notification-preferences`: "Show the seven notification toggles" (supported / "Próximamente", warning minutes), "Keep the hub count consistent" (supported count), "Accessible toggles" (the "Próximamente" announcement).
- `account-security`: "Change password" (the discard dialog). This delta builds on the version in `auth-session-hardening`, which must be archived first.

## Impact

- **`core:network`** — `NotificationPreferencesDto` gains `supported`, `supportedCount`, `supportedActiveCount`, `orderExpiringWarningMinutes`.
- **`core:domain`** — `NotificationPreferences` gains `supported` and `orderExpiringWarningMinutes`, with `supportedActiveCount` / `supportedCount` derived.
- **`core:data`** — `ProfileMappers` (supported set, defaulting to new order and order expiring).
- **`core:ui`** — `QhathiaDiscardChangesDialog` and its strings; `QhathiaSwitchRow(comingSoon)`.
- **`core:testing`** — `TestFixtures.notificationPreferences` (supported, minutes).
- **`features:profile`** — Datos personales, Cambiar contraseña, Notificaciones, Mi perfil hub; strings `values` / `values-en`; tests.
- **No new endpoint.** `GET /notifications/preferences` and `PUT /users/me` as in Fase 42 and 36.
- **Branch:** cut from `feature/auth-session-hardening` (change 6). `QhathiaSettingsRows` and the profile strings also changed in `feature/devices-live-status` (change 5), on other lines.
