## Context

A remote command reaches the tablet on its heartbeat. The button stays disabled while the command is `PENDING` / `DELIVERED`, and the backend moves an uncollected command to `EXPIRED` 10 minutes after it was requested (`api-contract.md`, "Expiración: 10 minutos, sin reintentos"). The screen polls every 15 s while a command is in flight.

`DeviceCommandDto` (`:core:network`) already parses `expiresAt`, but `DeviceCommandDto.toDomain()` (`:core:data`) drops it, and `DeviceCommand` (`:core:domain`) has no field for it.

No new module is needed. The change touches existing modules along the existing dependency direction:

```
:core:network (DTO, unchanged) → :core:data (mapper) → :core:domain (model)
                                                          ↑
                                     :features:device-linking (UI) ─┘   :core:testing (fixture)
```

## Goals / Non-Goals

**Goals:**
- Tell the owner when they can retry an in-flight command if the tablet doesn't respond.
- Use only data the backend states, with no client-side guesses.

**Non-Goals:**
- A per-second countdown.
- Estimating the next heartbeat from `lastPingAt`.
- Changing when the button re-enables (still driven by status, via polling).
- Backend or contract changes.

## Decisions

**1. Show `expiresAt`, not an estimated heartbeat time.**
The contract defines no heartbeat interval, and a heartbeat doesn't re-enable the button: the ack or the expiry does. A time estimated from the heartbeat would pass with the button still disabled whenever the tablet is off. `expiresAt` is the latest moment the button can stay disabled, so the text is always true.
_Alternative:_ `lastPingAt + interval`. Rejected: the interval is unknown and the estimate is wrong exactly when the owner is waiting (the tablet is offline).

**2. Fixed text, no ticker.**
A formatted time doesn't change, so it needs no recomposition every second and no extra coroutine. The existing 15 s poll already moves the card to its final state.
_Alternative:_ a "9:32" countdown driven by a shared `now` ticker. It works and is cheap even with many tablets, but it adds a per-second loop for no extra information.

**3. No fallback to `requestedAt + 10 min`.**
Hardcoding the server TTL in the client would duplicate a backend rule and drift if it changes. If `expiresAt` is missing, the line stays "Enviado".

**4. Formatting in the UI layer.**
`expiresAt` stays an `Instant` in the domain. The feature formats it with `DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault())`, the same pattern as `HomeFormatters.kt`, as an `internal` function in `:features:device-linking`.

**5. One string with a placeholder.**
Add `device_command_sent_retry_at` = "Enviado · Si la tablet no responde, podrás reintentar a las %1$s". `statusLabel()` picks it over `device_command_sent` when `expiresAt` is present. The existing `device_command_status_line` ("%1$s: %2$s") keeps wrapping it, so the line reads "Catálogo: Enviado · Si la tablet no responde, …".

**6. "Actualizado" in the catalog box uses the most recent of `lastSyncAt` and `lastPingAt`.**
Today the box shows "Actualizado <lastSyncAt>" (last catalog download), and a separate "Último reporte <lastPingAt>" caption appears under the tiles only when the tablet is disconnected. The new layout keeps a single line in the box, "Actualizado <relative time>", where the time is `maxOf` the two non-null instants (`listOfNotNull(lastSyncAt, lastPingAt).maxOrNull()`). It is shown whenever either exists, and the separate caption is dropped. `CatalogBox` receives that single `updatedAt: Instant?`, computed in `DeviceCard`. The existing `device_catalog_updated` string ("Actualizado %1$s" / "Updated %1$s") is reused; `device_last_report` becomes unused and is removed.
_Alternative:_ keep both lines in the box. Rejected because two timestamps next to each other are harder to read, and the request was one line.
_Trade-off:_ the owner can't tell whether "Actualizado" refers to the catalog download or the last report. Both mean "the tablet was in touch at that time", which is what the owner needs to know.

## Risks / Trade-offs

- [Phone and server clocks differ] → The time shown is the server's instant in the phone's time zone. Skew only shifts the shown time by the skew, and the button state still comes from the server status.
- [The time passes but the card still says "Enviado"] → The backend expiry job and the 15 s poll lag behind `expiresAt` by a few seconds. Acceptable; the next poll shows "No se pudo entregar".
- [Long line on narrow phones] → `bodySmall` text wraps onto a second line. Check it in the preview at phone width.
- ["Actualizado" read as "catalog updated"] → When the ping is newer than the sync, the line shows the ping time, even though the catalog was downloaded earlier. If that difference starts to matter, revisit with a follow-up change.
