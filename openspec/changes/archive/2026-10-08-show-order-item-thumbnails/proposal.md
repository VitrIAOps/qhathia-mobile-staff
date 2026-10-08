## Why

Every item in the "Detalle de solicitud" dialog shows a generic image icon instead of the product photo. That was a deliberate stopgap: the order-detail contract carried no image, and fetching one per item would have been an N+1 on dialog open. Backend Fase 24 (2026-10-03) closes the gap — `GET /api/v1/orders/{id}` now returns `items[].productImageUrl` — so the merchant can finally recognize the product on the shelf from the photo, which is what the detail is opened for before confirming or rejecting.

## What Changes

- Read the new nullable `items[].productImageUrl` field from `GET /api/v1/orders/{id}` and carry it through the network DTO, the data mapper and the domain `OrderItem`.
- Render the product photo in the 48dp thumbnail slot of each item row in the order detail dialog.
- Keep the current neutral placeholder as the fallback when `productImageUrl` is `null` (product without images), blank, or the image fails to load — never a blank hole.
- No additional requests: the URL arrives with the detail response the dialog already fetches.
- Ship the Fase 24 update of `docs/android/api-contract.md` (currently uncommitted in the working tree) together with this change.

Out of scope: the other Fase 24 contract additions (`confirmedOrders` on `/dashboard/activity`, `needsAttention.totalCount` on `/dashboard/ranking`, the `X-Kiosk-Session` header). They belong to the statistics screens, not to Solicitudes.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `order-requests`: the order detail dialog gains a requirement to show each item's product photo from `productImageUrl`, with the placeholder as the fallback.

## Impact

- **`:core:network`** — `OrderItemDto` gains `productImageUrl: String? = null`.
- **`:core:domain`** — `OrderItem` gains `productImageUrl: String?`.
- **`:core:data`** — `OrderItemDto.toDomain()` maps the field (blank → `null`); `OrderMappersTest` covers it.
- **`:core:testing`** — `TestFixtures.orderItem()` gains the parameter.
- **`:features:order-requests`** — `OrderDetailDialog` item row renders the image; preview data updated; module adds the Coil dependencies already present in the version catalog (`coil-compose`, `coil-network-okhttp`).
- **API** — additive, backward compatible: the field defaults to `null`, so the confirm/reject responses that share `OrderDetailDto` and any older backend keep deserializing.
- **No new Gradle module**, no manifest change, no new permission.
