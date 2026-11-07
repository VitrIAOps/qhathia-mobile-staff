## Context

`CatalogCountDto` reads only `activeProducts`; `DeviceRepositoryImpl.getCatalogCount()` returns it as an `Int` and `DeviceLinkingViewModel` reads it once, in `loadDevices()`. The silent re-read (`refreshSilently`, on the poll and on `ScreenStarted`) refreshes devices and commands but not the count. Fase 42 adds `visibleProducts` (same predicate as `/catalog/by-category`) and `hidden { outOfStock, lowStock, consult, noActiveCategory }`.

The card formats `expiresAt` with `DeviceFormatters.toShortLocalTime()` (`ofLocalizedTime(SHORT)`), which follows the locale and not the phone's 12/24-hour setting. `home-stats-fixes` added `core:ui` `TimeOfDay.kt` for the same problem on Inicio; this branch does not have it.

## Goals / Non-Goals

**Goals:**
- The card's number is what the tablet shows, and says how many are hidden.
- The number follows changes without restarting the app.
- The retry time follows the phone's setting.

**Non-Goals:**
- Showing the hidden breakdown by reason (the line names the three reasons together).
- Changing the poll cadence: the count is re-read on return and after a catalog refresh, not on every tick.

## Decisions

1. **A `CatalogCount` model.** `CatalogCount(active: Int, visible: Int?)` with `hidden: Int` derived (`active − visible`, at least 0, 0 when `visible` is null) and `shown: Int` (`visible ?: active`). The `hidden` object of the DTO is parsed (so a future screen can use it) but not carried into the domain.
   - *Why nullable `visible`?* An older backend omits it, and the card must then keep showing `activeProducts` with no "no se muestran" line.

2. **Re-read the count with the devices on return, and after an ACKED catalog refresh.** `refreshSilently(includeCatalog)` reads the count in parallel when asked. `ScreenStarted` asks for it; the poll does not, except when the re-read finds a `REFRESH_CATALOG` command whose status turned `ACKED` since the last state, which triggers one count read. A failed count read keeps the number shown.
   - *Why not every poll tick?* The count changes only when products change; one more request every 15–30 s would be noise.

3. **Same `TimeOfDay.kt` as `home-stats-fixes`.** Byte-identical, so merging both branches adds one file without conflict. `DeviceCard` formats `expiresAt` with `rememberShortTimeFormatter()`; `DeviceFormatters.kt` goes.

## Risks / Trade-offs

- [The two copies of `TimeOfDay.kt` drift before merging] → they are committed identical; any later edit goes to one branch after both merge.
- [A product created elsewhere while Dispositivos stays open] → shown on the next return or catalog refresh, as before for devices.
- [Archive order] → `pairing-flow-v2` and `devices-live-status` must be archived before this change.

## Migration Plan

Ships with the app, no data migration. Rollback is reverting the change.

## Open Questions

_None._
