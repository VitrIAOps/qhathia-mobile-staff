## Context

`OrderDetailDialog` (`:features:order-requests`) renders each order line with a 48dp box holding a static `Icons.Filled.Image`. The KDoc on `OrderItemRow` states why: the order-detail contract carried no product image, and fetching one per item would be an N+1 on dialog open.

Backend Fase 24 (`docs/android/api-contract.md` §24, "Miniatura en el detalle del pedido") adds `productImageUrl` to every entry of `items` in `GET /api/v1/orders/{id}`:

- It is the product's **current primary image**, not a snapshot — same criterion as `productName`.
- It is `null` when the product has no images; the app paints the placeholder.
- The backend resolves it with one query per order, so the app gets it for free with the request it already makes.

The field travels the existing vertical slice unchanged in shape:

```
OrderApiService.getOrder()            :core:network   OrderItemDto
        ↓
OrderItemDto.toDomain()               :core:data      OrderMappers.kt
        ↓
OrderItem                             :core:domain
        ↓
OrderDetailUiState.order.items        :features:order-requests
        ↓
OrderItemRow → thumbnail              OrderDetailDialog.kt
```

All four modules exist and are registered in `settings.gradle.kts`. No new module, no new dependency edge: `features/* → core:domain`, `core:data → core:domain + core:network` stay as they are.

## Goals / Non-Goals

**Goals:**

- Show the product photo in each item row of the order detail dialog.
- Keep a visible placeholder for `null`, blank and failed images.
- Add zero requests against the backend API.

**Non-Goals:**

- Thumbnails on the order **list** cards — `GET /api/v1/orders` rows carry no `items[]`, so there is nothing to show there.
- Tapping the thumbnail (zoom, navigation to the product detail).
- Extracting a shared thumbnail composable into `:core:ui` (see Decision 4).
- The other Fase 24 fields (`confirmedOrders`, `needsAttention.totalCount`, `X-Kiosk-Session`).

## Decisions

### 1. Nullable field with a default, end to end

`OrderItemDto.productImageUrl: String? = null` and `OrderItem.productImageUrl: String?`.

The default on the DTO is what keeps the change additive: `OrderDetailDto` is also the body of the confirm/reject responses, and a backend that has not deployed Fase 24 simply omits the field. Both keep deserializing and the UI degrades to the placeholder.

*Alternative considered:* a non-null `String` defaulting to `""`. Rejected — it pushes the "is there an image?" check into every consumer, and the rest of the domain (`Product.primaryImageUrl`, `TopSellingProduct.imageUrl`) already models an absent image as `null`.

### 2. Blank is normalized to `null` in the mapper

`OrderItemDto.toDomain()` maps `productImageUrl?.takeIf { it.isNotBlank() }`, the same idiom `OrderMappers.kt` already uses for `customerName`, `orderCode` and `rejectionReason`.

The domain then has a single "no image" value, so the composable needs one check and never hands Coil an empty string.

### 3. Coil `AsyncImage` over the existing placeholder box

The item row keeps its 48dp `surfaceVariant` box and its `Icons.Filled.Image` icon; when `productImageUrl` is non-null an `AsyncImage` is drawn on top with `ContentScale.Crop`, extracted into a private `OrderItemThumbnail(imageUrl: String?)` composable in `OrderDetailDialog.kt`. The icon is shown whenever the image is not successfully displayed — URL absent, still loading, or failed — and hidden once it is, so a photo with transparency never shows the icon through it.

- The request sets an explicit `size` matching the slot, converted from dp to px, per the performance rule in `android-frontend-standards.md` ("always specify `size` constraints").
- `contentDescription = null`: the product name sits beside the thumbnail, so a description would be read twice.
- No ViewModel, `UiState` or `UiEvent` change: the URL rides inside the `OrderDetail` already held by `OrderDetailUiState`.

`:features:order-requests` adds `implementation(libs.coil.compose)` and `implementation(libs.coil.network.okhttp)`, both already in the version catalog and already used the same way by `:features:home`.

*Alternative considered:* `SubcomposeAsyncImage` with `loading`/`error` slots. Rejected — subcomposition per row for a 48dp thumbnail is heavier than tracking the load state, and no other screen in the app uses it.

### 4. Local composable, not a shared `:core:ui` component

`:features:home` (`RestockProductCard`), `:features:sales-statistics` (`RankingProductRow`) and `:features:product-inventory` (`ProductRow`) each own a private thumbnail composable with a different size and fallback. This change follows that established pattern.

*Alternative considered:* promote one `QhathiaProductThumbnail` to `:core:ui` and migrate all four call sites. It is a reasonable cleanup, but it touches three unrelated features and their previews for a bug fix in a fourth; it should be its own change.

### 5. No caching of the URL beyond the detail

The dialog always renders from a fresh `GET /orders/{id}` and the state is dropped on dismiss, so "current image, not a snapshot" holds without extra work. Nothing is persisted. Coil's own bitmap cache is keyed by URL, so a new primary image (new URL) is fetched on the next open.

## Risks / Trade-offs

- **A slow or failing image CDN** → The dialog never waits on images: rows, total and actions render from the detail response alone, and the placeholder stays in place.
- **Backend not yet on Fase 24 in some environment** → The field is absent, the DTO default yields `null`, every row shows today's placeholder. No regression.
- **Unreachable image host shows placeholders that look like "no photo"** → Accepted. A broken-image state distinct from "no image" adds visual noise to a dialog whose job is confirm/reject.
- **Fourth copy of a thumbnail composable** → Accepted for this fix (Decision 4); worth a follow-up consolidation change.
- **Coil added to one more module** → No APK impact: the artifacts are already on the app's classpath through `:features:home`.

## Migration Plan

Additive and backward compatible; no migration. Rollback is reverting the commit — the placeholder behaviour is the previous behaviour.
