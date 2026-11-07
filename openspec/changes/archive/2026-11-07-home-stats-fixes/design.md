## Context

**F-56.** `SuggestionSection` already hands the tapped suggestion to `onAdd` / `onRestock`; `HomeContent` passes no-op lambdas. `homeGraph` exposes only the bell and `Ver más`. `PRODUCT_REGISTER_ROUTE = "product_register"` takes no argument; `ProductRegisterViewModel` already receives a `SavedStateHandle` (for its idempotency key). Detalle and Registro report back through `previousBackStackEntry.savedStateHandle` with Inventario's keys (`MainShell`).

**F-89.** `SalesStatisticsUiEvent.Share -> Unit`; the top bar's share button is always enabled; the only effect is `NavigateBack`.

**F-64.** `SalesStatisticsViewModel` emits `NavigateBack` on every tap and the shell calls `popBackStack()` each time; the second pop removes `home`, the `NavHost`'s start, and nothing is left. Detalle (`onBack`, `onSaved`, `onDeleted`), Registro and Dispositivos pop the same way; the `navigate` calls have no guard against a second tap.

**F-100 / F-95.** `getRanking` sends no `limit` (backend default 3, max 50). `TopSellersDto` has `totalRevenue` and `items`; Fase 42 adds `periodRevenue`, `productsSold`, `items[].active` and `asOf`. The ranking caption counts listed products; the needs-attention caption says "sin ventas". The ViewModel never reloads a tab already loaded for the same filters, and the screen has no `ON_START` hook.

**F-76.** Inicio (`HomeFormatters.toLocalTimeLabel`) and Solicitudes (`OrderRequestsHeader`) use `ofLocalizedTime(SHORT)`, which follows the locale (`H:mm` in Spanish), not the phone's 12/24-hour setting. The statistics hour range uses a fixed `HH:mm`.

**F-57.** The bell's `IconButton` receives `clearAndSetSemantics` through its `modifier`, which sits before the button's own `clickable`, so click action and role are wiped. `QhathiaBottomBarItem` does the same on a badged tab.

**F-75 (Inicio).** `HomeViewModel.load` and `MainShellViewModel`'s reconcile both call `refreshPendingOrderCount()` when the shell starts; `OrderRepositoryImpl` does not coalesce concurrent calls. Change 3 added a push path (`HandleOrderPushUseCase`) and `refreshCountAfterSettling` after confirm/reject, both of which must see the order that triggered them.

**Unmet searches.** `UnmetSearchGroupDto` lacks `variants` and `personCountEstimated` (Fase 41).

The project's unit tests are JUnit 5 without Robolectric, and there are no instrumented tests in CI.

Modules (no new module; graph unchanged):

```
app (MainShell) ──► features:home, features:sales-statistics, features:product-register, features:order-requests
features:* ──► core:ui (QhathiaBottomBar, TimeOfDay) ──► core:domain ◄── core:data ──► core:network
```

## Goals / Non-Goals

**Goals:**
- No control on Inicio or Resumen de ventas looks active and does nothing.
- No double tap can leave the shell without a screen.
- Ranking and its captions say exactly what the backend measures.
- Times follow the phone's 12/24-hour setting.
- One count request per need, never a stale count after a push or a decision.

**Non-Goals:**
- F-94 and the Dispositivos card's time format (`devices-catalog-count`).
- Sharing an image or a PDF.
- A ViewModel-level exit guard for Detalle and Registro (their exits pass through the discard dialog; the shell guard covers the double tap).

## Decisions

1. **Agregar / Reabastecer go through Inventario.** New `HomeUiEvent.AddMissingProduct(name)` / `RestockProduct(productId)` emit `HomeUiEffect.NavigateToProductRegister(prefillName)` / `NavigateToProductDetail(productId)`; `homeGraph` gains the two callbacks. The shell switches to the Inventario tab first, then pushes the screen, so `onSaved` / `onRegistered` write to Inventario's entry and its snackbar shows.
   - *Why not push over Inicio?* The result would land on Inicio's entry, which has no snackbar for it.
   - Registro's route becomes `product_register?name={name}` (nullable, default null), with `productRegisterRoute(prefillName)` building it with `Uri.encode`. Inventario's "+" uses `productRegisterRoute()`, since navigating to the pattern would send the literal `{name}`. The ViewModel reads the argument once into `name` and into the dirty baseline, so an untouched prefill leaves without the discard dialog.

2. **Share text is built in the UI from what is loaded.** `SalesStatisticsUiState.isShareEnabled` is true when the tab on screen has data and is not loading. `Share` emits `SalesStatisticsUiEffect.Share(tab)` only then. The screen builds the text with a pure `shareText(...)` in `StatisticsShare.kt` (the ViewModel has no `Resources`), and opens `ShareCompat.IntentBuilder(...).setType("text/plain").startChooser()`. `shareText` takes the resolved strings and formatters as parameters so it is unit-testable on the JVM.

3. **Guarded pop and navigate in the shell.** `NavController.popIfOn(route)` pops only while the current destination's route is the given pattern; `navigateOnce(route)` navigates only from a `RESUMED` entry, with `launchSingleTop`. They replace the plain calls for Resumen de ventas, Detalle, Registro, Dispositivos and Ajustes de la tablet's open. `SalesStatisticsViewModel` also keeps `isLeaving`, so its effect is emitted once.
   - *Why compare with the route pattern?* `currentDestination.route` is the pattern with `{args}`, which is what each graph exports.

4. **Ranking fields are optional and fall back.** DTO: `periodRevenue: Double? = null`, `productsSold: Int? = null`, `active: Boolean = true`, `asOf: String? = null`. Domain: `SalesRanking.periodRevenue: BigDecimal?`, `productsSold: Int?`, `asOf: Instant?`, `TopSellingProduct.active`. `getRanking(granularity, categoryId, limit = 5)`. The caption is "Top N de M productos · S/ X de S/ Y" when both new fields arrive, and the current caption otherwise. A deleted product's row adds "· Eliminado" in `onSurfaceVariant`, also in its accessible text; it is never filtered out.

5. **Resumen de ventas reloads on return.** `SalesStatisticsUiEvent.ScreenStarted` from `LifecycleEventEffect(ON_START)`; the first is ignored (init already loads), later ones reload the tab on screen with `force = true` and keep its data (the query is unchanged, so `keepData` holds). Other tabs reload when shown, because `loadedQueries` is cleared.

6. **One time-of-day formatter.** `core:ui` `component/time/TimeOfDay.kt`: `shortTimeFormatter(locale, is24Hour)` uses `DateFormat.getBestDateTimePattern(locale, "Hm" | "hm")`; `rememberShortTimeFormatter()` reads the app locale from `LocalConfiguration` and `DateFormat.is24HourFormat(context)`. Inicio and Solicitudes format with it. `periodRangeLabel(..., timeFormatter = HOUR_FORMAT)` takes the formatter, so existing tests keep `HH:mm` and the screen passes the phone's.
   - *Testing:* `getBestDateTimePattern` is Android code; without Robolectric it is covered by QA, and `periodRangeLabel` is tested on the JVM with a 12-hour formatter.

7. **Semantics stated once, after the click.** The bell sets `contentDescription`, `role = Button` and `onClick` in its own `clearAndSetSemantics`. `QhathiaBottomBarItem` uses `selectable(selected, role = Role.Tab, onClick)` for every tab, and, with a badge, a `clearAndSetSemantics` that restates `selected`, `role`, `contentDescription` and `onClick`.
   - *Testing:* a Compose UI test needs an instrumented run, which the project does not have; QA checks with `inspect_screen` and TalkBack.

8. **Single-flight count with a freshness opt-out.** `OrderRepository.refreshPendingOrderCount(reuseInFlight: Boolean = true)`. The implementation holds a `Mutex` and a generation counter bumped on each successful fetch. A caller records the generation, takes the lock, and if the generation moved while it waited and `reuseInFlight` is true, returns the count just fetched; otherwise it fetches. `HandleOrderPushUseCase` and `refreshCountAfterSettling` pass `false`, so they wait for any request in flight and then send their own. Inicio and the reconcile keep the default and share one request.
   - *Why not the handoff's version as is?* It lets a push reuse a request that left before the order existed, and the badge would miss it until the next refresh.
   - A failed fetch does not bump the generation, so a waiter retries instead of inheriting the failure.

9. **Variants and estimated counts in the domain.** `MissingProductSuggestion.variants: List<String>` (terms other than the group's own, at most 3) and `personCountEstimated: Boolean`. The card shows "También: a, b" as a line under the example (or under the name when there is none), and "~N" with the plural "búsquedas" when estimated; the accessible text says "aproximadamente N búsquedas".

## Risks / Trade-offs

- [A push waits behind a slow count request] → one extra request in the worst case; the badge is right either way.
- [Older backend without the Fase 42 fields] → every new field is optional and the current captions stay.
- [Navigating to Inventario first flashes its list] → the push follows in the same frame; it is the path the merchant would take by hand.
- [Untested Android formatting] → kept to one small function; QA checks 12 h, 24 h and past midnight.
- [Archive order] → `home-missing-products-examples-and-other-requests` and `refresh-on-return` must be archived before this change.

## Migration Plan

Ships with the app, no data migration. Rollback is reverting the change.

## Open Questions

_None._
