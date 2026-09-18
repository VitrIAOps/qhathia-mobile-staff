## Why

The Perfil tab is still a placeholder: a "coming soon" label, a link-devices button and sign out. Backend phases 18–23 already serve everything a merchant needs to manage their account, store and tablets from the phone: profile, subscription, devices and remote commands, sessions and password, notification preferences, and store language and IGV. The ClickUp task [Perfil de comercio y configuracion](https://app.clickup.com/t/86e38gtxw) asks to replace the placeholder with the eight screens designed in Figma `Qhatia-DS`.

## What Changes

- **Mi perfil** (node `2086-3671`) replaces the placeholder behind the Perfil tab. **BREAKING** for `app/ui/profile`, which is deleted.
  - Gradient header with the notifications bell.
  - Identity card: initials avatar, name, email, and role · store count. Its edit icon opens Datos personales.
  - Subscription card: plan badge, status, monthly price, renewal date and period progress. It opens Mi suscripción.
  - Menu: Datos personales · Notificaciones (`N de M alertas activas`) · Seguridad · Configuración · Dispositivos (`N tablets conectadas`) · Ayuda.
  - "Cerrar sesión" at the bottom, keeping the existing sign-out flow.
- **Datos personales** (`2269-4227`): **read-only** view of name, email and phone (`+51` formatted), with the initials avatar. "Cambiar foto" shows as *Próximamente*.
- **Notificaciones** (`2292-6906`): the seven preference toggles in the Pedidos / Tablet / Tu negocio groups. Each change is saved on its own with an optimistic update, and rolls back if the save fails.
- **Seguridad** (`2086-7140`):
  - **Cambiar contraseña**: a row showing when the password was last updated. It opens a change-password form built from design-system components, since there is no Figma node for it.
  - **Ingresar con huella**: uses the biometrics already enrolled on the phone. If none are enrolled, a message tells the merchant to set them up in the device settings.
  - **Sesiones activas**: this phone first. Any other session can be closed, and "Cerrar sesión en otros equipos" closes them all.
- **Biometric unlock**: when fingerprint sign-in is on, opening the app with a stored session asks for the device biometric before showing the shell. Choosing to use the password signs out to the login screen.
- **Configuración** (`2293-7282`):
  - **Idioma** (Español / English): saved on the device, and the device value always wins. It switches the **app language** through Android per-app locales and is also sent to the store (`language`).
  - **Precios con IGV incluido**: the store's `pricesIncludeIgv`. Only OWNER and ADMIN can change it.
  - **English string resources** are added to every module.
- **Dispositivos** (`2293-7459`) replaces the current "Dispositivos vinculados" list with one card per tablet:
  - Connection status and visible catalog (count and last sync).
  - Battery, connection, store, and "Clientes hoy" (*Próximamente*).
  - **Actualizar catálogo** and **Reiniciar**, with the remote-command status (Enviado / Listo / Falló / No se pudo entregar).
  - "Vincular otro dispositivo" reuses the existing pairing dialog.
- **Ayuda** (`2294-9087`): FAQ accordion with local static answers, quick guides (*Próximamente*), and the app version with "Términos y privacidad" (*Próximamente*).
- **Mi suscripción** (`2294-9694`): plan card with price, features derived from the plan flags, and period progress. Payment method, payment history and "Cancelar mi suscripción" show as *Próximamente*.
- **Navigation**: every sub-screen is pushed over Perfil without the bottom bar, and back returns to Mi perfil.
- **Role gating**: SELLERs do not see the subscription card or Dispositivos, and see the IGV toggle read-only, because those endpoints are OWNER/ADMIN only.
- **Login** sends a `deviceLabel` (the phone model). The session keeps the returned `sessionId` so Seguridad can mark "este equipo".
- Figma elements with no backend support are listed in project memory (`profile-backend-gaps`) so backend can be notified.

## Capabilities

### New Capabilities
- `merchant-profile`: the Mi perfil hub (identity, subscription summary, menu with live subtitles, sign out entry), read-only Datos personales, Mi suscripción, and Ayuda, including role gating and *Próximamente* treatments.
- `notification-preferences`: viewing and toggling the seven push-notification preferences with optimistic, per-toggle saves.
- `account-security`: change password, active-session listing and revocation, and the fingerprint sign-in setting, which depends on the device's enrolled biometrics.
- `biometric-unlock`: gating app entry behind the device biometric when fingerprint sign-in is enabled, with a password fallback.
- `app-settings`: the language selector (device-owned, applied as the app locale, mirrored to the store) and the IGV display setting.

### Modified Capabilities
- `device-linking`: the Profile entry point becomes the Dispositivos row. The linked-devices list becomes per-tablet status cards with remote commands, and pairing moves from the floating + button to "Vincular otro dispositivo".
- `session-management`: the persisted session also stores the server `sessionId`, which survives token refresh. Sign out is reached from the bottom of Mi perfil.
- `merchant-auth`: login sends the phone model as `deviceLabel` and hands the returned `sessionId` to the session layer.

## Impact

- **New module:** `:features:profile` (Android library, Compose + Hilt). It holds the hub, Datos personales, Notificaciones, Seguridad, Cambiar contraseña, Configuración, Ayuda and Mi suscripción. It is registered in `settings.gradle.kts` and consumed by `:app`.
- **`:features:device-linking`:** its screen is rebuilt as Dispositivos; the pairing dialog is kept.
- **`:features:auth`:** new biometric unlock screen; login passes the device label.
- **`:core:domain`:**
  - New models: `UserProfile`, `Subscription`, `NotificationPreferences`, `ActiveSession`, `StoreSettings`, `AppLanguage`, `DeviceCommand`.
  - `Device` is extended with name, battery, network, store name and last sync.
  - `Session` gains `sessionId`.
  - New repository ports and use cases for each area.
- **`:core:network`:** new `UserApiService`, `SubscriptionApiService`, `StoreApiService` and `CatalogApiService`. `AuthApiService` gains sessions and change-password; `NotificationApiService` gains preferences; `DeviceApiService` gains the extended list and commands.
- **`:core:data`:** repository implementations and mappers. A new app-preferences DataStore holds language and the biometric flag. The session persistence gains `sessionId`.
- **`:core:ui`:** shared settings components (gradient top bar with back, settings group card, navigation row, toggle row, *Próximamente* tag) and a biometric prompt helper. It adds `androidx.biometric`.
- **`:core:testing`:** new fakes and fixtures.
- **`:app`:**
  - `MainActivity` moves to `AppCompatActivity` (needed by per-app locales and `BiometricPrompt`), with an AppCompat DayNight theme.
  - `locales_config.xml` and the `AppLocalesMetadataHolderService` manifest entry are added.
  - `MainShell` wires the profile graph; the biometric gate sits in the root.
  - `app/ui/profile` is deleted.
- **Dependencies:** `androidx.appcompat`, `androidx.biometric` (version catalog).
- **Every module with strings:** new `values-en/strings.xml`.
- **APIs consumed (`api-contract.md` §2–5, §18–23):**
  - `GET /users/me` and `GET /subscriptions/current`.
  - `GET/PUT /notifications/preferences`.
  - `GET /auth/sessions`, `DELETE /auth/sessions/{id}`, `POST /auth/sessions/revoke-others` and `POST /auth/change-password`.
  - `GET/PUT /stores/{storeId}`.
  - `GET /devices`, `GET /catalog/count`, and `POST/GET /devices/{id}/commands`.
  - `POST /auth/login` with `deviceLabel`.
