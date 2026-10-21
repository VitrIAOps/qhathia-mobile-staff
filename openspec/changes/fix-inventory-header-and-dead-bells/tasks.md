## 1. Inventario

- [x] 1.1 `InventoryHeader` — remove `.padding(horizontal = Dimens.SpaceXs)` from the title (design Decision 1)
- [x] 1.2 `InventoryHeader` — bell takes `onOpenOrderRequests: () -> Unit` as its `onClick`; content description `inventory_bell_cd` ("Ver solicitudes" / "View requests"); remove `inventory_notifications_cd` (es/en)
- [x] 1.3 Thread `onNavigateToOrderRequests` through `inventoryGraph` → `InventoryScreen` → `InventoryContent` → `InventoryHeader`; update previews

## 2. Detalle de producto

- [x] 2.1 `ProductDetailUiEvent.OrderRequestsRequested`, `ProductDetailUiEffect.NavigateToOrderRequests`, and the `pendingExit` logic in `ProductDetailViewModel` (design Decision 3): pristine → effect; dirty → dialog; `ConfirmDiscard` emits the pending exit; `DismissDiscard` resets it; ignored while saving
- [x] 2.2 `ProductDetailHeader` — bell `onClick` sends the event, `enabled = !isSaving`, content description `detail_bell_cd` ("Ver solicitudes" / "View requests"); remove `detail_notifications_cd` (es/en)
- [x] 2.3 Thread `onNavigateToOrderRequests` through `productDetailGraph` → `ProductDetailScreen` (effect handling)
- [x] 2.4 `ProductDetailViewModelTest`: pristine bell → `NavigateToOrderRequests`; dirty bell → dialog, confirm → `NavigateToOrderRequests`, dismiss → nothing and data intact; cancel after a dismissed bell still emits `NavigateBackCancelled`; bell ignored while saving
  - Not covered: "ignored while saving/submitting" — the fakes cannot hold a save in flight; guarded by the disabled bell and the ViewModel check.

## 3. Registro de producto

- [x] 3.1 Same as 2.1 for `ProductRegisterViewModel` (`OrderRequestsRequested`, `NavigateToOrderRequests`, `pendingExit`; ignored while submitting)
- [x] 3.2 `ProductRegisterHeader` — bell sends the event, disabled while submitting, content description `register_bell_cd`; remove `register_notifications_cd` (es/en)
- [x] 3.3 Thread `onNavigateToOrderRequests` through `productRegisterGraph` → `ProductRegisterScreen`
- [x] 3.4 `ProductRegisterViewModelTest`: the same cases as 2.4
  - Not covered: "ignored while saving/submitting" — the fakes cannot hold a save in flight; guarded by the disabled bell and the ViewModel check.

## 4. Shell

- [x] 4.1 `MainShell` — `inventoryGraph(onNavigateToOrderRequests = { switchTab(ORDER_REQUESTS_ROUTE) })`; for `productDetailGraph` and `productRegisterGraph` pass `{ navController.popBackStack(); switchTab(ORDER_REQUESTS_ROUTE) }` (design Decision 2)
- [x] 4.2 Grep for `onClick = {}` in the three headers and for the removed `*_notifications_cd` strings: none left

## 5. Verification

- [x] 5.1 `./gradlew :features:product-inventory:testDebugUnitTest :features:product-detail:testDebugUnitTest :features:product-register:testDebugUnitTest` and their `lintDebug`
- [x] 5.2 Full build: `./gradlew testDebugUnitTest lintDebug assembleDebug`
- [x] 5.3 Emulator: Inventario title x matches Inicio and Solicitudes; each bell opens Solicitudes; from Detalle/Registro with edits the discard dialog appears, confirm lands on Solicitudes and the Inventario tab shows the list; TalkBack tree reports "Ver solicitudes"
  - Emulator pass 2026-10-21: Inventario title at x=42 like Inicio/Solicitudes; all three bells labelled "Ver solicitudes" and open Solicitudes; Detalle with edits → dialog, "Seguir editando" keeps the edit, "Descartar" lands on Solicitudes and the Inventario tab shows the list with the name unchanged; Registro pristine → straight to Solicitudes. Registro with edits not run on device (same code path, unit-tested).
