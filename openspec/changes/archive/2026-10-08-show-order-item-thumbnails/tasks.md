> No new Gradle module is required: every module touched below already exists and is registered in `settings.gradle.kts`. No code is added as packages inside `:app`.

## 1. Network layer (`:core:network`)

- [x] 1.1 In `dto/OrderDtos.kt`, add `val productImageUrl: String? = null` to `OrderItemDto`.
- [x] 1.2 Extend the `OrderItemDto` KDoc: `productImageUrl` is the product's current primary image (not a snapshot), added in Fase 24, and `null` when the product has no images.

## 2. Domain layer (`:core:domain`)

- [x] 2.1 In `model/OrderItem.kt`, add `val productImageUrl: String?` to `OrderItem`.
- [x] 2.2 Extend the `OrderItem` KDoc: the URL is a live value like `productStock`, and `null` means the placeholder is shown.

## 3. Data layer (`:core:data`)

- [x] 3.1 In `mapper/OrderMappers.kt`, map `productImageUrl = productImageUrl?.takeIf { it.isNotBlank() }` in `OrderItemDto.toDomain()`.
- [x] 3.2 In `OrderMappersTest`, add a `productImageUrl` parameter to the `itemDto()` helper and cover three cases: a URL is mapped as-is, `null` stays `null`, and a blank string becomes `null`.

## 4. Test utilities (`:core:testing`)

- [x] 4.1 In `TestFixtures.orderItem()`, add `productImageUrl: String? = null` and pass it to `OrderItem`.

## 5. Order detail dialog (`:features:order-requests`)

- [x] 5.1 In `features/order-requests/build.gradle.kts`, add `implementation(libs.coil.compose)` and `implementation(libs.coil.network.okhttp)`, as `:features:home` does.
- [x] 5.2 In `ui/OrderDetailDialog.kt`, extract the 48dp thumbnail box of `OrderItemRow` into a private `OrderItemThumbnail(imageUrl: String?, modifier: Modifier = Modifier)` composable that keeps the `surfaceVariant` background and the `Icons.Filled.Image` placeholder.
- [x] 5.3 In `OrderItemThumbnail`, when `imageUrl` is non-null draw a Coil `AsyncImage` with `ContentScale.Crop`, `contentDescription = null` and an `ImageRequest` whose `size` is the 48dp slot converted to px; show the placeholder icon whenever the image is not successfully displayed (absent, loading or failed) and hide it once it is.
- [x] 5.4 Call `OrderItemThumbnail(imageUrl = item.productImageUrl)` from `OrderItemRow`, and rewrite the `OrderItemRow` KDoc that says the contract carries no product image.
- [x] 5.5 In `ui/OrderPreviewData.kt`, add `productImageUrl: String? = null` to `previewOrderItem()` and pass it to `OrderItem`, so the existing light/dark dialog previews keep rendering the placeholder state.

## 6. Documentation

- [ ] 6.1 Include the Fase 24 update of `docs/android/api-contract.md` (already modified in the working tree) in this change's commit.
- [x] 6.2 In the `GET /api/v1/orders/{id}` section of `docs/android/api-contract.md`, check that the response example and field notes mention `productImageUrl`; if they do not, add it so the endpoint section agrees with §24.

## 7. Verification

- [x] 7.1 Run `./gradlew :core:data:testDebugUnitTest :features:order-requests:testDebugUnitTest` and confirm all tests pass.
- [x] 7.2 Run `./gradlew :features:order-requests:lintDebug` and `./gradlew assembleDebug` and confirm both succeed.
- [x] 7.3 Check the `OrderDetailDialog` light and dark `@Preview`s render with the placeholder.
- [x] 7.4 On a device against a Fase 24 backend, open the detail of an order with a product that has a photo and one that has none: the first shows the photo, the second the placeholder, and only `GET /orders/{id}` is issued.
