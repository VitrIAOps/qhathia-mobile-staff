## Context

- **Title.** `InventoryHeader` lays out `Row { Text(title, Modifier.weight(1f).padding(horizontal = SpaceXs)); IconButton(bell) }` inside a `Column` already padded by `SpaceMd`. The `SpaceXs` padding separated the title from the back chevron removed in `fix-low-stock-home-refresh-header-chevron`; on its own it pushes the title ~4 dp right. Inicio's and Solicitudes' titles start at the `SpaceMd` padding.
- **Bells.** `InventoryHeader`, `ProductDetailHeader` and `ProductRegisterHeader` each have `IconButton(onClick = {}) { Icon(Notifications, contentDescription = "Notificaciones") }`. Inicio (`HomeHeader`) and Mi perfil route their bells through a callback the shell wires to `switchTab(ORDER_REQUESTS_ROUTE)` — `navigate(route) { popUpTo(start) { saveState = true }; launchSingleTop = true; restoreState = true }`. Mi perfil's bell is labelled `profile_bell_cd` = "Ver solicitudes".
- **Guards.** Detalle and Registro already route back and cancel through `CancelRequested` → (dirty ? `showDiscardDialog` : `NavigateBackCancelled`) → `ConfirmDiscard` emits `NavigateBackCancelled`; a `BackHandler` covers system back while dirty. The bottom bar is hidden on these pushed screens, so the bell is the only way out to another tab.

## Goals / Non-Goals

**Goals:**
- Every header bell in the app opens Solicitudes, consistently.
- Leaving a form through the bell is exactly as safe as leaving it through back.
- Inventario's title lines up with the other root tabs.

**Non-Goals:**
- A pending-count badge on these bells.
- Changing the Inicio or Mi perfil bells.
- Any dark-theme work on the headers.

## Decisions

### 1. Remove the padding, nothing else

Delete `.padding(horizontal = Dimens.SpaceXs)` from the title modifier; keep `weight(1f)`. No spacer: the header's own `SpaceMd` padding is where Inicio and Solicitudes start.

### 2. A navigation callback per graph, wired by the shell

Each of `inventoryGraph`, `productDetailGraph` and `productRegisterGraph` gains `onNavigateToOrderRequests: () -> Unit`, passed down to its screen and header. Feature modules keep not knowing about each other or about routes: only `MainShell` knows `ORDER_REQUESTS_ROUTE`.

- **Inventario** (a root tab): `onNavigateToOrderRequests = { switchTab(ORDER_REQUESTS_ROUTE) }` — identical to Inicio's bell; Inventario's state is saved and restored by the tab switch.
- **Detalle / Registro** (pushed on top of Inventario): `{ navController.popBackStack(); switchTab(ORDER_REQUESTS_ROUTE) }`. Popping first closes the form, so the saved Inventario stack is just the list: coming back to the Inventario tab never resurrects a form the merchant chose to abandon.

### 3. The bell reuses the discard guard with a different exit

In both form ViewModels, add `UiEvent.OrderRequestsRequested` and a private `pendingExit` (`Exit.Back` / `Exit.OrderRequests`):

```
CancelRequested         -> leave(Exit.Back)
OrderRequestsRequested  -> leave(Exit.OrderRequests)
leave(exit): if (isSaving) ignore
             else if (isDirty) { pendingExit = exit; showDiscardDialog = true }
             else emit(exit.effect)
ConfirmDiscard          -> showDiscardDialog = false; emit(pendingExit.effect)
DismissDiscard          -> showDiscardDialog = false; pendingExit = Exit.Back
```

`Exit.Back.effect` is the existing `NavigateBackCancelled`; `Exit.OrderRequests.effect` is a new `NavigateToOrderRequests` effect, handled in the screen by calling `onNavigateToOrderRequests`. The dialog text is unchanged ("discard changes?") — it is about losing the edits, which is the same whichever way the merchant leaves. The bell is `enabled = !isSaving`.

### 4. One label everywhere

The three bells use "Ver solicitudes" / "View requests". Each feature module owns its own string (feature modules do not share resources); the text matches `profile_bell_cd`. The old `*_notifications_cd` strings are removed.

## Risks / Trade-offs

- **Tab switch from a pushed screen** is new in this app → covered by Decision 2's pop-then-switch, and by a manual check that returning to Inventario shows the list.
- **More callbacks through the graph APIs** → three parameters, each with one caller (`MainShell`); acceptable for keeping features decoupled.
- **No badge** → Inicio's bell shows a count and these do not. Accepted; the Solicitudes tab badge is visible as soon as the merchant lands there.
