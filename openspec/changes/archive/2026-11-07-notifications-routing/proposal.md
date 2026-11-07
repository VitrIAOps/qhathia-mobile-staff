## Why

The 2026-11-03 QA regression (`docs/handoff/android-regresion-2026-11-03.md` §3.1 and §3.2) finds two ways a tap on an order notification lands somewhere other than Solicitudes:

- **F-52** — With fingerprint sign-in on and the app more than 5 minutes in the background, tapping "Nueva orden de compra" and passing the fingerprint opens **Inicio** (S:LOG-052). The old shell, still composed for a moment, switches to Solicitudes and consumes the destination; then the app locks, that shell is thrown away, and the new one after the fingerprint starts on Inicio with nothing pending.
- **F-54** — With two or more order notifications, Android builds its own group summary, which carries no app intent. Tapping the group clears the notifications and goes back to whatever screen was open (S:EXP-002, X:XPE-022).

## What Changes

- **The destination survives the lock (F-52).** While the lock is due (fingerprint on and the app not yet unlocked, or more than 5 minutes in the background), the shell's "consumed" call keeps the destination. After the fingerprint, the new shell opens Solicitudes · Pendientes and consumes it then. A notification tap after more than 5 minutes in the background locks at once, without depending on whether the tap or the return to the foreground is seen first. Without fingerprint nothing changes.
- **Our own group summary (F-54).** Order notifications go into one group with an app-owned summary, "N órdenes por atender" ("N orders to handle"), whose tap opens Solicitudes · Pendientes. It follows the tray: it updates as orders arrive and leave, and goes away with the last one. When Solicitudes is already open, the tap re-queries the list (the signal from change 3, `refresh-on-return`). The reconcile never takes the summary for an order.

## Capabilities

### New Capabilities

_None._

### Modified Capabilities

- `order-notifications`: "Display a local notification for each order push" (grouped under a summary that opens Solicitudes). This delta builds on the version in `refresh-on-return`, which must be archived first.
- `biometric-unlock`: "Biometric gate on app entry" (a notification tap while the lock is due still lands on Solicitudes after the fingerprint).

## Impact

- **`app`** — `MainViewModel` (`onPendingDestinationConsumed` keeps the destination while the lock is due; `onDeepLink` locks right away after a long background), `MainViewModelTest`.
- **`core:notifications`** — `OrderNotificationPresenter` (group key, summary, `activeOrderIds` without the summary), a small pure helper for the summary count with JUnit tests, a new plural in `values` and `values-en`.
- **No backend or API change.** Pushes stay data-only, one per order (§9.6).
- **Branch:** cut from `feature/refresh-on-return` (change 3), which supplies `onOrdersDeepLink`. `MainViewModel` was also changed in `feature/auth-session-hardening` (change 6), in other functions; expect a small merge in `MainViewModelTest`.
