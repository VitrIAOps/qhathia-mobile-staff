## 1. Domain and data

- [x] 1.1 Add `expiresAt: Instant?` to `DeviceCommand` in `:core:domain` (after `requestedAt`) and update its KDoc
- [x] 1.2 Map `expiresAt` with `parseIsoInstant()` in `DeviceCommandDto.toDomain()` in `:core:data`
- [x] 1.3 Add an `expiresAt` parameter (default `NOW + 10 min`) to `TestFixtures.deviceCommand` in `:core:testing` and fix any positional constructor calls
- [x] 1.4 Extend `DeviceMappersTest` to assert `expiresAt` is parsed, and is `null` when absent

## 2. UI in `:features:device-linking`

- [x] 2.1 Add the string `device_command_sent_retry_at` = "Enviado · Si la tablet no responde, podrás reintentar a las %1$s" to `values/strings.xml`, and "Sent · If the tablet doesn't respond, you can retry at %1$s" to `values-en/strings.xml`
- [x] 2.2 Add an `internal` formatter `Instant.toShortLocalTime()` (localized SHORT time, `ZoneId.systemDefault()`) next to `DeviceCard.kt`
- [x] 2.3 In `DeviceCommand.statusLabel()`, use `device_command_sent_retry_at` for `PENDING` / `DELIVERED` when `expiresAt` is present; keep `device_command_sent` otherwise
- [x] 2.4 Add a `DeviceCard` preview with an in-flight `REFRESH_CATALOG` that has `expiresAt`, and check the line wraps cleanly at phone width

## 3. "Actualizado" under the product count

- [x] 3.1 In `DeviceCard`, compute `updatedAt = listOfNotNull(device.lastSyncAt, device.lastPingAt).maxOrNull()`; change `CatalogBox` to take `updatedAt` instead of `lastSyncAt`, and show "Actualizado <relative time>" (`device_catalog_updated`) under the product count whenever it is not null, connected or not
- [x] 3.2 Remove the "Último reporte" caption block from `DeviceCard` (the `!connected && device.lastPingAt != null` branch)
- [x] 3.3 Remove the now-unused `device_last_report` string from `values/strings.xml` and `values-en/strings.xml`
- [x] 3.4 Check the existing previews in `DeviceLinkingContent.kt`: the connected tablet (ping 2 min, sync 4 min ago) shows "Actualizado hace 2 min", the disconnected one (ping 2 h, no sync) "Actualizado hace 2 horas", and no "Último reporte" text remains

## 4. Verification

- [x] 4.1 Run `./gradlew :core:data:testDebugUnitTest :features:device-linking:testDebugUnitTest` and fix failures
- [x] 4.2 Run `./gradlew compileDebugKotlin lintDebug`
- [x] 4.3 Check the spec scenarios against the preview: retry time shown in flight, "Enviado" alone without `expiresAt`, retry time hidden once `ACKED` / `FAILED` / `EXPIRED`, and "Actualizado" under the product count for connected, disconnected and never-reported tablets
