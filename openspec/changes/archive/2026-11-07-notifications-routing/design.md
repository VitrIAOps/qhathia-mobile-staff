## Context

**F-52.** `MainActivity` hands a notification's destination to `MainViewModel.onDeepLink`, from `onCreate` or `onNewIntent`. `MainShell` has a `LaunchedEffect(pendingDestination)` that switches to Solicitudes, calls `onOrdersDeepLink()` (change 3), and then `onPendingDestinationConsumed()`, which sets the destination to `null`. `authState` is `stateIn(WhileSubscribed(5_000))`, so after more than 5 minutes in the background the root briefly still renders the cached `Authenticated`, and the old shell runs that effect. Then `onAppForegrounded` sets `unlocked = false`, the root switches to `BiometricUnlockScreen` and drops that shell. After the fingerprint a fresh `QhathiaNavHost` starts on Inicio, and the destination is already gone.

**F-54.** `OrderNotificationPresenter.post` builds each order notification with the order id as tag and the fixed id `1`, but without `setGroup`. With two or more notifications Android auto-groups them under a system summary (`g:Aggregate_AlertingSection`) with no content intent, so a tap only clears them. `activeOrderIds()` returns every tagged notification on the `orders` channel, and the reconcile (`ReconcileOrderNotificationsUseCase`) cancels the ones that are not pending.

`core:notifications` tests run on JUnit 5 with MockK; the project has no Robolectric.

Modules (no new module; graph unchanged):

```
app (MainViewModel, MainShell) ──► core:domain (OrderNotificationTray port)
core:notifications (OrderNotificationPresenter, OrderSummary helper) ──► core:domain, core:common
```

## Goals / Non-Goals

**Goals:**
- A notification tap always ends on Solicitudes · Pendientes, also behind the fingerprint gate and also from the group.
- One summary per group, owned by the app, that follows the tray.
- No regression without fingerprint (S:SOL-060) or while signed out (the destination still survives login).

**Non-Goals:**
- Changing the notification icon or color (F-76, `home-stats-fixes`).
- Changing `MainShell` or the root's `Unauthenticated` branch.
- Showing order details in the summary (pushes are PII-free, §9.6).

## Decisions

1. **Keep the destination while the lock is due, in `MainViewModel`.** `onPendingDestinationConsumed()` returns without clearing when `isLockDue()`. `isLockDue()` reads a separate `biometricEnabledNow: StateFlow<Boolean?>` (`stateIn(Eagerly, null)`): an unread value counts as due (do not consume); fingerprint off is never due; otherwise it is due when `!unlocked` or when `backgroundedAt` is more than `RELOCK_AFTER` ago. `observeBiometricEnabled` is stored in a `private val` and `biometricEnabledNow` is declared before `authState`, so it subscribes first.
   - *Why not change `authState`'s `combine`?* An `initialValue = false` for the fingerprint would show the shell for a frame before the gate on a cold start.
   - *Why not fix it in `MainShell`?* The shell cannot know it is about to be dropped; the ViewModel owns the lock state. `MainShell` stays as it is: after unlocking, the new shell sees the destination still pending and consumes it for real.

2. **`onDeepLink` locks at once after a long background.** If `backgroundedAt` is set and more than `RELOCK_AFTER` ago, `onDeepLink` sets `unlocked = false` itself. `onNewIntent` and the process `ON_START` can arrive in either order; this way the lock is due before the old shell runs its effect. `backgroundedAt` is left for `onAppForegrounded` to clear.

3. **An app-owned summary, recomputed from the tray.** Children get `setGroup(GROUP_KEY)`. After each post or cancel, `refreshSummary(including, excluding)` reads the tray, counts the child notifications, and posts or cancels the summary (tag `orders_summary`, id `2`, `setGroupSummary(true)`, `GROUP_ALERT_CHILDREN`, `InboxStyle`, `setAutoCancel`, the same Solicitudes intent with its own request code). `including` / `excluding` correct for the tray lagging behind a `notify` / `cancel` just issued. `cancelUnmatched()` refreshes the summary at the end.
   - *Why recompute instead of keeping a counter?* The tray is the only state that survives process death and the user swiping notifications away.
   - *Why `GROUP_ALERT_CHILDREN`?* Only the order notification itself should sound; updating the summary is silent.

4. **Children are counted by key, the summary is never an order.** A tray entry counts as a child when it lacks `FLAG_GROUP_SUMMARY` (ours or the system's). Its key is its tag, or `#<id>` for a notification posted without an order id, so those count too. `activeOrderIds()` returns only the tagged children, so the reconcile never "cancels" the summary as an unknown order. The counting is a small pure function (`OrderSummary.childKeys(entries, including, excluding)` over `TrayEntry(tag, id, isSummary)`) so it can be tested on JUnit 5 without Robolectric.
   - *Why not add Robolectric for an end-to-end tray test?* It needs JUnit 4 and the vintage engine, a new test stack for one class. The Android calls left in the presenter are thin; QA covers them with `dumpsys notification` (task 6).

5. **The summary is not cancelled while a child remains.** Cancelling a group summary cancels its children. With one child left the summary stays, reading "1 orden por atender"; it goes only when the count reaches zero.

## Risks / Trade-offs

- [The fingerprint value is still unread when the shell consumes the destination] → it counts as due, so the destination is kept; the next shell (after the read, or after unlocking) consumes it. `biometricEnabledNow` subscribes before `authState`, so in practice it is read first.
- [A consume is refused and no lock follows] → only possible if the lock was due but the gate never showed, which `authState` rules out (same inputs). The destination then waits for the next shell, which consumes it.
- [Some OEMs throw while reading `activeNotifications`] → `orderNotifications()` already returns an empty list; the summary is then computed from `including` alone, or left untouched on a cancel.
- [Permission revoked between post and summary] → `notificationsAllowed()` is checked and `SecurityException` is caught, as for children.
- [Merge with change 6] → `MainViewModel` gains a constructor-stored use case and two functions here; change 6 changed push registration. `MainViewModelTest` will need a hand merge of its factory.

## Migration Plan

Ships with the app, no data migration. Notifications left in the tray by an older version have no group; the first new post groups the new ones and the reconcile on the next foreground clears the stale ones as before. Rollback is reverting the change.

## Open Questions

_None._
