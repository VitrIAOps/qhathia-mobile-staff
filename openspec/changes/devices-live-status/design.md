## Context

`DeviceLinkingViewModel`:
- reads `GET /devices` on `init` and after pairing (and, since `pairing-flow-v2`, on each later `ScreenStarted`);
- `ensurePolling()` runs only while a command is `PENDING`/`DELIVERED`, re-reads only commands (never devices), and *merges* each answer into what it had;
- `latestPerType` drops a finished command whose `requestedAt` is older than 10 minutes. A command expires exactly at `requestedAt + 10 min`, so an `EXPIRED` is born already filtered out: the merge then keeps the stale `PENDING`, and a reload empties the line.

`DeviceCommand` has no `deliveredAt`/`ackedAt`, although the DTO decodes them. `DeviceCard` shows `ACKED` as "Listo" for a restart. Yet the tablet acknowledges before the new process reports: only a heartbeat after the ack proves the tablet is back.

`TabletSettingsViewModel` loads once. Its unlink failure path says "Inténtalo de nuevo" for everything. Since `pairing-flow-v2`, `unlinkDevice` fails with `DeviceSettingsException`: `404 → NotFound`, `Network`, `Unknown`.

The connection tile shows the raw `networkStatus` for anything not WIFI/MOBILE/ONLINE/LIMITED, always with the Wi-Fi icon.

Backend facts (§35, §37):
- the tablet beats every 60 s;
- `GET /devices` carries `lastPingAt`, `lastSyncAt`, battery, network, model and name;
- commands expire after 10 min and are never retried;
- unlinking expires the in-flight commands;
- there is no "restart completed" state.

Modules (no new module; graph unchanged):

```
features:device-linking ──► core:domain ◄── core:data          (DeviceCommand.ackedAt/deliveredAt)
app (MainShell) ──────────┘  core:ui (QhathiaComingSoonTag)     features:profile (plurals)
core:testing: TestFixtures.deviceCommand
```

## Goals / Non-Goals

**Goals:**
- What the cards and Ajustes show is never older than ~30 s while the screen is open, and nothing is read when it is not.
- Every command line tells the truth about the outcome, including a restart the tablet never came back from, and an expired command.
- Unlinking a tablet that is already gone ends cleanly.

**Non-Goals:**
- Push or WebSocket updates for devices (the heartbeat model is deliberate, handoff §1).
- `settingsVersion`-based "changed elsewhere" notice, and renaming "Actualizado" (both optional in the handoff).
- Any backend change: there is no 409 on a late ack and no restart-completed event.

## Decisions

### 1. One refresh loop for Dispositivos (P-2)

`ensurePolling()` keeps a single job while the screen is started. It ticks every 15 s and decides on each tick, so a command sent mid-wait is followed at the fast pace at once:

```kotlin
var ticks = 0
while (isActive) {
    delay(COMMAND_POLL_INTERVAL)                                              // 15 s
    ticks++
    if (state.needsFastPolling || ticks % TICKS_PER_DEVICES_REFRESH == 0) refreshSilently()   // else every 30 s
}
```

(Choosing the delay before waiting, as first drafted, left a command sent during a 30 s wait unfollowed until that wait ended.)

`refreshSilently()` re-reads `GET /devices` (no `isLoading`, failures keep the cards) and then, in parallel, the commands of `deviceIdsNeedingCommandPoll` only. Each tablet's commands are **replaced** by the new `latestPerType`, never merged.
- `ScreenStarted` starts the loop. When it is not the first start and the last load is at least 5 s old, it refreshes at once; this replaces `pairing-flow-v2`'s plain reload. `ScreenStopped` cancels the loop.
- `DeviceUnlinked` and pairing completion call `refreshSilently()` too.
- `loadDevices()` (the visible one, on init and retry) stamps `lastDevicesLoadAt`, so the first start does not double it.
- *Why 30 s:* the tablet beats every 60 s, so a 30 s read shows a beat at most ~30 s late, which is what QA expects ("hace un momento" ≤ 30 s). One small GET per 30 s, foreground only.

### 2. Restart phases from the command and the tablet's reports (F-09)

```kotlin
enum class RestartPhase { SENT, RESTARTING, BACK, NOT_BACK }
fun restartPhase(device: Device): RestartPhase?
```

The phase comes from the latest `RESTART_APP` command:
- `PENDING` → `SENT`;
- `DELIVERED` → `RESTARTING`;
- `ACKED` → `BACK` if `lastPingAt` is after `ackedAt`, `NOT_BACK` if `now − ackedAt ≥ 3 min`, otherwise `RESTARTING`. An `ACKED` without `ackedAt` (older backend) → `BACK`, which is today's behaviour;
- `FAILED`/`EXPIRED` → null; the card shows the generic text.

`RESTARTING` counts as busy for the Reiniciar button. It adds the device to `deviceIdsNeedingCommandPoll` and triggers `needsFastPolling`, so the loop reads every 15 s until the tablet reports or 3 minutes pass. The card's restart line takes the phase and renders `SENT` with today's "Enviado…" text, `RESTARTING` neutral, `BACK` "Listo" in green, and `NOT_BACK` the error text in red. `REFRESH_CATALOG` keeps its current mapping: its ack arrives after it ran.

### 3. Finished commands live 10 minutes from when they finished (F-61)

```kotlin
val finishedAt: Instant? get() = when (status) {
    ACKED, FAILED -> ackedAt
    EXPIRED -> expiresAt
    else -> null
}
```

`latestPerType` keeps a command when it is in flight, or when `(finishedAt ?: requestedAt)` is within the last 10 minutes.

Local safety net: `DeviceCommand.isOverdue(now) = isInFlight && expiresAt != null && now > expiresAt + 30 s`. The state treats an overdue command as expired: not busy, no fast polling, and the card shows the expired text. The 30 s margin covers server-side expiry latency. The expired copy becomes the explanatory one ("…la tablet no se conectó en 10 min…").

### 4. Ajustes refreshes silently (F-65)

`TabletSettingsUiEvent.ScreenStarted` / `ScreenStopped`, sent from `TabletSettingsScreen` with `LifecycleStartEffect`, as Dispositivos does.
- While started, a job re-reads settings and devices every 30 s. A later `ScreenStarted` also re-reads at once.
- A re-read applies only when `saving.isEmpty()`, `pinDialog == null`, `renameDialog == null`, `!isUnlinkConfirmVisible` and `!isUnlinking`, so it never overwrites what the owner is editing.
- A `404` from the settings goes to `NOT_FOUND`; other failures are ignored.
- `load()` stays the visible first load.

### 5. Unlinking a tablet that is gone (F-62)

The `DeviceSettingsException` from `pairing-flow-v2` is reused rather than adding the handoff's `UnlinkDeviceError`: it already carries `NotFound`, `Network` and `Unknown` for the same endpoint family.
- `TabletSettingsViewModel.unlink()` maps them:
  - `NotFound` → `Unlinked(deviceId, alreadyGone = true)`;
  - `Network` → `ShowMessage(device_unlink_error_network)`;
  - anything else → the current message.
- `TabletSettingsUiEffect.Unlinked` gains `alreadyGone`, and `TabletSettingsScreen.onUnlinked(deviceId, alreadyGone)` passes it on.
- The navigation graph's `onTabletUnlinked(deviceId, alreadyGone)` stores both: `TABLET_UNLINKED_RESULT_KEY` and the new `TABLET_UNLINKED_ALREADY_GONE_KEY`.
- `MainShell` sets them on the previous entry. The Dispositivos destination reads both and sends `DeviceUnlinked(deviceId, alreadyGone)`.
- The ViewModel emits `ShowUnlinked(alreadyGone)`, which shows `tablet_settings_not_found` or `device_unlinked`, and then calls `refreshSilently()`.

### 6. Connection tile (F-63)

`networkPresentation(status, connected): Pair<ImageVector, String>`, mapped as in the spec table, with icons from `material-icons-extended`, already a dependency of the module. The tile uses both values. `device_network_wifi` becomes "Wi-Fi", and new strings are added for Cable, Sin conexión and Otra red.

### 7. Visual details (F-66)

- `QhathiaComingSoonTag`: `maxLines = 1`, `softWrap = false`.
- `TileCell` shows the tag on its own line under the label, not next to the value.
- `CommandActions`: when `LocalDensity.current.fontScale > 1.15f`, the two buttons stack in a `Column` at full width; otherwise they stay in the `Row` (weights 1.4 / 1). The labels keep `maxLines = 1` with ellipsis as a last resort.
- `tablet_settings_info_serial` → "Número de serie": the app's font draws "º" like "o".
- `features:profile` plurals → "%d vinculada / vinculadas", "%d activa / activas" (the English ones are already neutral).

## Risks / Trade-offs

- [Tests and an endless loop] → A visible screen refreshes forever, and `runTest` drains the shared test scheduler at the end, so the Dispositivos and Ajustes tests wrap `runTest` in a `screenTest` that stops every screen in a `finally`.

- [More requests] → One `GET /devices` per 30 s, or per 15 s while waiting, plus commands only for the tablets that need it. Foreground only, cancelled on stop. With 1–3 tablets it is negligible.
- [A silent refresh fighting the owner's edit in Ajustes] → It never applies while anything is being saved or a dialog is open.
- [Restart judged "not back" too early on a slow tablet] → 3 minutes is three heartbeats; the button is enabled again so the owner can retry, and a later heartbeat turns it into "Listo".
- [Clock skew between phone and server for the local expiry] → The 30 s margin, and the server's own `EXPIRED` normally arrives first.

## Migration Plan

Single merge, no flag. Rollback is reverting the commit.

## Open Questions

_None._
