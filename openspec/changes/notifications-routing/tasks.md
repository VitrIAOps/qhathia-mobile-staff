## 1. Destination behind the fingerprint gate (`app`) — F-52

- [x] 1.1 `MainViewModel`: keep `observeBiometricEnabled` in a `private val`; add `biometricEnabledNow: StateFlow<Boolean?>` (`stateIn(Eagerly, null)`), declared before `authState`; leave `authState`'s `combine` as it is (design decision 1)
- [x] 1.2 `MainViewModel.isLockDue()`: unread → due; fingerprint off → not due; `!unlocked` → due; `backgroundedAt` older than `RELOCK_AFTER` → due
- [x] 1.3 `onPendingDestinationConsumed()` returns without clearing while `isLockDue()`; `onDeepLink` sets `unlocked = false` when `backgroundedAt` is older than `RELOCK_AFTER` (design decision 2); update the class KDoc
- [x] 1.4 `MainViewModelTest`:
  - `deepLink_whileRelockDue_survivesConsumeUntilUnlocked`: background, advance 6 min, `onDeepLink(DESTINATION_ORDER_REQUESTS)` → `authState` is `Locked`; consume → still pending; `onUnlocked()`, consume → `null`;
  - a cold start with fingerprint on: consume before unlocking keeps the destination;
  - fingerprint off: consume clears the destination at once;
  - a short background (2 min) with fingerprint on: consume clears it

## 2. Group summary (`core:notifications`) — F-54

- [x] 2.1 `OrderSummary` (internal, pure): `TrayEntry(tag: String?, id: Int, isSummary: Boolean)` and `childKeys(entries, including, excluding): Set<String>` (key = tag, or `#<id>` when untagged; summaries skipped) (design decision 4)
- [x] 2.2 `OrderNotificationPresenter`: constants `GROUP_KEY`, `SUMMARY_TAG = "orders_summary"`, `SUMMARY_ID = 2`; `setGroup(GROUP_KEY)` on children; `post()` calls `refreshSummary(including = key)` after `notify`, inside the same `try`
- [x] 2.3 `refreshSummary(including, excluding)`: no children → cancel the summary; otherwise, if allowed, post it (`setGroupSummary(true)`, `GROUP_ALERT_CHILDREN`, `InboxStyle`, `setAutoCancel`, plural title, `openAppIntent(SUMMARY_TAG)`); catch `SecurityException` (design decisions 3 and 5)
- [x] 2.4 `cancel(orderId)` → `refreshSummary(excluding = orderId)`; `cancelUnmatched()` skips summaries and ends with `refreshSummary()`; `activeOrderIds()` returns only tagged children; update the class KDoc
- [x] 2.5 Plural `notif_orders_summary_title` in `values` ("%d orden por atender" / "%d órdenes por atender") and `values-en` ("%d order to handle" / "%d orders to handle")
- [x] 2.6 `OrderSummaryTest` (JUnit 5): summaries (ours and the system's) are not children; untagged children count by id; `including` adds a key the tray does not show yet; `excluding` removes one it still shows; an empty tray with `excluding` gives no children

## 3. Verification

- [x] 3.1 Run, one at a time: `:core:notifications:testDebugUnitTest`, `:app:testDebugUnitTest`, then `testDebugUnitTest`
- [x] 3.2 Run `./gradlew lintDebug` and `./gradlew assembleDebug`

## 4. Manual — QA

- [ ] 4.1 Manual — S:LOG-052: fingerprint on, more than 5 min in the background → push → tap it → fingerprint → Solicitudes · Pendientes; nginx shows `GET /orders?status=PENDING&…&size=50` after the unlock
- [ ] 4.2 Manual — S:SOL-060: fingerprint off → tap a push → Solicitudes · Pendientes
- [ ] 4.3 Manual — S:EXP-002 / X:XPE-022: app in the background on Perfil → Configuración, create 2–3 orders → the tray shows "Qhathia Staff · 3 órdenes por atender"; tapping it opens Solicitudes · Pendientes with the 3 orders; `adb shell dumpsys notification --noredact | grep -i orders_summary` finds the summary
- [ ] 4.4 Manual — same with the app in the foreground on Solicitudes: tapping the group re-queries the list (`GET …size=50`)
- [ ] 4.5 Manual — reject 2 of 3 orders by curl: the summary reads "1 orden por atender"; reject the last: the group is gone
