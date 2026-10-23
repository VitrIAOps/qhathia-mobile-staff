## 1. Theme (`core:ui`)

- [x] 1.1 Register `urbanist(FontWeight.Light)` in `UrbanistFamily` (`core/ui/src/main/java/com/qhathia/mobile/staff/core/ui/theme/Type.kt`) and mention the Light weight in the family's KDoc

## 2. Product row (`features:product-inventory`)

- [x] 2.1 In `ProductRow.kt` → `ProductPrice`, add `fontWeight = FontWeight.Light` to the struck-through original price text; leave order and the plain price unchanged
- [x] 2.2 Remove the `FontWeight.Bold` override from the discounted price so it uses the plain price style (`bodyMedium`, `onSurface`)
- [x] 2.3 Update the `ProductPrice` KDoc to say the original price is struck through in a light weight

## 3. Verification

- [x] 3.1 Check the discounted row in the `InventoryContent.kt` previews, light and dark: the struck price is visibly lighter than before and still legible, and the discounted price looks like a plain price (not bold)
- [x] 3.2 Run `./gradlew :core:ui:assembleDebug :features:product-inventory:testDebugUnitTest` and `./gradlew lintDebug`; all pass
