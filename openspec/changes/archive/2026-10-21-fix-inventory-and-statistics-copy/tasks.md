## 1. Inventario count

- [x] 1.1 `features:product-inventory` — turn `inventory_product_count` into a `<plurals>` (es: "%1$d producto" / "%1$d productos"; en: "%1$d product" / "%1$d products")
- [x] 1.2 `ProductCountAndFilterBar` — read it with `pluralStringResource(R.plurals.inventory_product_count, productCount, productCount)`
- [x] 1.3 Add a preview (or adjust an existing one) showing a single product, light and dark

## 2. Empty Ranking cards

- [x] 2.1 `RankingTab.RankingCard` — draw the summary row (trend icon, total, caption) only when `!isEmpty` (design Decision 2); update the KDoc
- [x] 2.2 Check the existing empty-ranking preview (`SalesStatisticsPreviewData.emptyRanking`) now shows only titles and empty messages, light and dark

## 3. Datos personales subtitle

- [x] 3.1 `features:profile` — `menu_personal_subtitle` = "Nombre, correo y teléfono" / "Name, email and phone"

## 4. Verification

- [x] 4.1 `./gradlew :features:product-inventory:testDebugUnitTest :features:product-inventory:lintDebug :features:sales-statistics:testDebugUnitTest :features:sales-statistics:lintDebug :features:profile:lintDebug`
- [x] 4.2 Full build: `./gradlew testDebugUnitTest lintDebug assembleDebug`

## QA (manual, run by QA)

- Inventario: a search leaving one product reads "1 producto"; many products read "N productos" (es and en)
- Ranking with no sales in the period: "Lo que más vende" shows only its title and "Aún no hay ventas en este periodo."; same for "Lo que necesita atención" when nothing is stalled
- Mi perfil: the Datos personales row reads "Nombre, correo y teléfono"
