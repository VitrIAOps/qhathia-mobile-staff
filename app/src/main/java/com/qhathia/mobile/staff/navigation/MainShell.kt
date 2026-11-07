package com.qhathia.mobile.staff.navigation

import com.qhathia.mobile.staff.feature.devicelinking.navigation.TABLET_UNLINKED_ALREADY_GONE_KEY
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.annotation.DrawableRes
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.qhathia.mobile.staff.core.ui.component.QhathiaBottomBar
import com.qhathia.mobile.staff.core.ui.component.QhathiaBottomBarItem
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.qhathia.mobile.staff.R
import com.qhathia.mobile.staff.core.common.navigation.DESTINATION_ORDER_REQUESTS
import com.qhathia.mobile.staff.core.ui.R as CoreUiR
import com.qhathia.mobile.staff.feature.productinventory.navigation.INVENTORY_REGISTERED_PRODUCT_ID_KEY
import com.qhathia.mobile.staff.feature.productinventory.navigation.INVENTORY_REGISTER_RESULT_KEY
import com.qhathia.mobile.staff.feature.productinventory.navigation.INVENTORY_REGISTER_RESULT_DISCOUNT_FAILED
import com.qhathia.mobile.staff.feature.productinventory.navigation.INVENTORY_REGISTER_RESULT_PLAN_LIMIT
import com.qhathia.mobile.staff.feature.productinventory.navigation.INVENTORY_REGISTER_RESULT_SUCCESS
import com.qhathia.mobile.staff.feature.productinventory.navigation.INVENTORY_RESULT_PRODUCT_ALREADY_DELETED
import com.qhathia.mobile.staff.feature.productinventory.navigation.INVENTORY_RESULT_PRODUCT_DELETED
import com.qhathia.mobile.staff.feature.productinventory.navigation.INVENTORY_RESULT_PRODUCT_UPDATED
import com.qhathia.mobile.staff.feature.productinventory.navigation.INVENTORY_ROUTE
import com.qhathia.mobile.staff.feature.productinventory.navigation.inventoryGraph
import com.qhathia.mobile.staff.feature.productdetail.navigation.PRODUCT_DETAIL_ROUTE
import com.qhathia.mobile.staff.feature.productdetail.navigation.productDetailGraph
import com.qhathia.mobile.staff.feature.productdetail.navigation.productDetailRoute
import com.qhathia.mobile.staff.feature.productregister.navigation.PRODUCT_REGISTER_ROUTE
import com.qhathia.mobile.staff.feature.productregister.navigation.productRegisterGraph
import com.qhathia.mobile.staff.feature.productregister.navigation.productRegisterRoute
import com.qhathia.mobile.staff.feature.devicelinking.navigation.DEVICE_LINKING_ROUTE
import com.qhathia.mobile.staff.feature.devicelinking.navigation.TABLET_SETTINGS_ROUTE
import com.qhathia.mobile.staff.feature.devicelinking.navigation.TABLET_UNLINKED_RESULT_KEY
import com.qhathia.mobile.staff.feature.devicelinking.navigation.deviceLinkingGraph
import com.qhathia.mobile.staff.feature.devicelinking.navigation.tabletSettingsRoute
import com.qhathia.mobile.staff.feature.orderrequests.navigation.ORDER_REQUESTS_ROUTE
import com.qhathia.mobile.staff.feature.orderrequests.navigation.orderRequestsGraph
import com.qhathia.mobile.staff.feature.home.navigation.HOME_ROUTE
import com.qhathia.mobile.staff.feature.home.navigation.homeGraph
import com.qhathia.mobile.staff.feature.salesstatistics.navigation.SALES_STATISTICS_ROUTE
import com.qhathia.mobile.staff.feature.salesstatistics.navigation.salesStatisticsGraph
import com.qhathia.mobile.staff.feature.salesstatistics.navigation.salesStatisticsRoute
import com.qhathia.mobile.staff.feature.profile.navigation.PROFILE_ROUTE
import com.qhathia.mobile.staff.feature.profile.navigation.profileGraph

/**
 * Requests `POST_NOTIFICATIONS` once on first entry to the authenticated shell (API 33+ only). The
 * result is ignored — order-token registration proceeds regardless; a denial only suppresses the
 * visible tray notification.
 */
@Composable
private fun RequestNotificationPermissionOnce() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* result intentionally ignored */ }
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

/** A bottom-navigation destination. */
private data class TopLevelDestination(
    val route: String,
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val iconRes: Int,
)

private val topLevelDestinations = listOf(
    TopLevelDestination(HOME_ROUTE, R.string.nav_home, CoreUiR.drawable.ic_nav_home),
    TopLevelDestination(INVENTORY_ROUTE, R.string.nav_inventory, CoreUiR.drawable.ic_nav_inventory),
    TopLevelDestination(ORDER_REQUESTS_ROUTE, R.string.nav_requests, CoreUiR.drawable.ic_nav_requests),
    TopLevelDestination(PROFILE_ROUTE, R.string.nav_profile, CoreUiR.drawable.ic_nav_profile),
)

/**
 * Post-login app shell: a bottom navigation bar over an inner [NavHost] of the top-level
 * destinations. Inicio is the start destination — entering the app, whether from login or from a
 * restored session, lands on the store summary. Terminal pairing is reached from Perfil, so the DS
 * bottom bar carries only these four tabs.
 *
 * The Solicitudes tab carries the app-wide pending-order count, read from [MainShellViewModel], so
 * it can never disagree with the Inicio bell or the `Pendientes (N)` filter label.
 *
 * [pendingDestination] is a section requested by a notification tap. The inner NavHost lives here,
 * so this is the only place that can honour it; it does so as a normal tab switch and then reports
 * back through [onPendingDestinationConsumed].
 */
@Composable
fun MainShell(
    pendingDestination: String? = null,
    onPendingDestinationConsumed: () -> Unit = {},
    viewModel: MainShellViewModel = hiltViewModel(),
) {
    RequestNotificationPermissionOnce()

    // Also fires when the shell first appears (and after unlocking), since a new observer of a
    // started lifecycle receives ON_START.
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.onForegrounded() }

    val pendingOrderCount by viewModel.pendingOrderCount.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val currentDestination by navController.currentBackStackEntryAsState()
    val currentRoute = currentDestination?.destination

    /**
     * Switching top-level tabs: one copy of each destination, restoring whatever state it had. The
     * bell on Inicio and a notification tap both go through this, so they land on Solicitudes
     * exactly as tapping the tab does instead of stacking a second copy on top.
     */
    fun switchTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    /**
     * The bell on a form pushed over Inventario (product detail, registration). The form is popped
     * first, so the Inventario stack saved by the tab switch is just the list: coming back to the tab
     * never brings back a form the merchant chose to leave.
     */
    fun leaveFormForOrderRequests() {
        navController.popBackStack()
        switchTab(ORDER_REQUESTS_ROUTE)
    }

    LaunchedEffect(pendingDestination) {
        if (pendingDestination != DESTINATION_ORDER_REQUESTS) return@LaunchedEffect
        switchTab(ORDER_REQUESTS_ROUTE)
        // Solicitudes may already be open, so the switch alone would not show the new order.
        viewModel.onOrdersDeepLink()
        onPendingDestinationConsumed()
    }

    // Hide the bottom navigation on non-top-level destinations (e.g. product registration).
    val showBottomBar = topLevelDestinations.any { destination ->
        currentRoute?.hierarchy?.any { it.route == destination.route } == true
    }

    Scaffold(
        bottomBar = {
            if (!showBottomBar) return@Scaffold
            QhathiaBottomBar {
                topLevelDestinations.forEach { destination ->
                    val selected = currentRoute?.hierarchy?.any { it.route == destination.route } == true
                    val badged = destination.route == ORDER_REQUESTS_ROUTE && pendingOrderCount > 0
                    QhathiaBottomBarItem(
                        selected = selected,
                        onClick = { switchTab(destination.route) },
                        iconRes = destination.iconRes,
                        label = stringResource(destination.labelRes),
                        badgeCount = if (badged) pendingOrderCount else null,
                        badgeContentDescription = if (badged) {
                            pluralStringResource(
                                R.plurals.nav_requests_pending_cd,
                                pendingOrderCount,
                                pendingOrderCount,
                            )
                        } else {
                            null
                        },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = HOME_ROUTE,
            modifier = Modifier.padding(padding),
        ) {
            homeGraph(
                onNavigateToOrderRequests = { switchTab(ORDER_REQUESTS_ROUTE) },
                // A push, not a tab switch: back returns to Inicio. Not a top-level route, so the
                // bottom bar hides itself while it is shown.
                onNavigateToSalesStatistics = { period ->
                    navController.navigateOnce(salesStatisticsRoute(period))
                },
                // Through Inventario, so onSaved / onRegistered (which write Inventario's result
                // keys to the previous entry) land on its list and its snackbar shows.
                onNavigateToProductRegister = { name ->
                    switchTab(INVENTORY_ROUTE)
                    navController.navigate(productRegisterRoute(name)) { launchSingleTop = true }
                },
                onNavigateToProductDetail = { id ->
                    switchTab(INVENTORY_ROUTE)
                    navController.navigate(productDetailRoute(id)) { launchSingleTop = true }
                },
            )
            salesStatisticsGraph(onBack = { navController.popIfOn(SALES_STATISTICS_ROUTE) })
            inventoryGraph(
                onNavigateToRegister = { navController.navigateOnce(productRegisterRoute()) },
                onNavigateToProductDetail = { id -> navController.navigateOnce(productDetailRoute(id)) },
                onNavigateToOrderRequests = { switchTab(ORDER_REQUESTS_ROUTE) },
            )
            productDetailGraph(
                onSaved = {
                    navController.popIfOn(PRODUCT_DETAIL_ROUTE) {
                        navController.previousBackStackEntry?.savedStateHandle
                            ?.set(INVENTORY_REGISTER_RESULT_KEY, INVENTORY_RESULT_PRODUCT_UPDATED)
                    }
                },
                onDeleted = { alreadyDeleted ->
                    navController.popIfOn(PRODUCT_DETAIL_ROUTE) {
                        navController.previousBackStackEntry?.savedStateHandle?.set(
                            INVENTORY_REGISTER_RESULT_KEY,
                            if (alreadyDeleted) INVENTORY_RESULT_PRODUCT_ALREADY_DELETED else INVENTORY_RESULT_PRODUCT_DELETED,
                        )
                    }
                },
                onBack = { navController.popIfOn(PRODUCT_DETAIL_ROUTE) },
                onNavigateToOrderRequests = ::leaveFormForOrderRequests,
            )
            productRegisterGraph(
                onRegistered = { productId ->
                    navController.popIfOn(PRODUCT_REGISTER_ROUTE) {
                        navController.previousBackStackEntry?.savedStateHandle?.let { handle ->
                            handle[INVENTORY_REGISTERED_PRODUCT_ID_KEY] = productId
                            handle[INVENTORY_REGISTER_RESULT_KEY] = INVENTORY_REGISTER_RESULT_SUCCESS
                        }
                    }
                },
                onPlanLimit = {
                    navController.popIfOn(PRODUCT_REGISTER_ROUTE) {
                        navController.previousBackStackEntry?.savedStateHandle
                            ?.set(INVENTORY_REGISTER_RESULT_KEY, INVENTORY_REGISTER_RESULT_PLAN_LIMIT)
                    }
                },
                onDiscountFailed = { productId ->
                    navController.popIfOn(PRODUCT_REGISTER_ROUTE) {
                        navController.previousBackStackEntry?.savedStateHandle?.let { handle ->
                            handle[INVENTORY_REGISTERED_PRODUCT_ID_KEY] = productId
                            handle[INVENTORY_REGISTER_RESULT_KEY] = INVENTORY_REGISTER_RESULT_DISCOUNT_FAILED
                        }
                    }
                },
                onCancelled = { navController.popIfOn(PRODUCT_REGISTER_ROUTE) },
                onNavigateToOrderRequests = ::leaveFormForOrderRequests,
            )
            orderRequestsGraph()
            profileGraph(
                navController = navController,
                // Pushed over Mi perfil like its other sections, so back returns there.
                onNavigateToDevices = { navController.navigateOnce(DEVICE_LINKING_ROUTE) },
                onNavigateToOrderRequests = { switchTab(ORDER_REQUESTS_ROUTE) },
            )
            deviceLinkingGraph(
                // Shared by both screens of the graph; each pop still needs one of them on top.
                onBack = { navController.popIfOn(DEVICE_LINKING_ROUTE, TABLET_SETTINGS_ROUTE) },
                onOpenTabletSettings = { id -> navController.navigateOnce(tabletSettingsRoute(id)) },
                onTabletUnlinked = { id, alreadyGone ->
                    navController.popIfOn(TABLET_SETTINGS_ROUTE) {
                        navController.previousBackStackEntry?.savedStateHandle?.let { handle ->
                            // The flag first: Dispositivos reacts to the id, and must already see it.
                            handle[TABLET_UNLINKED_ALREADY_GONE_KEY] = alreadyGone
                            handle[TABLET_UNLINKED_RESULT_KEY] = id
                        }
                    }
                },
            )
        }
    }
}

/**
 * Pops only while one of [routes] (the graph's route patterns, `{args}` included) is on top, after
 * running [beforePop] (where a result is written to the entry below). A second tap on back, which
 * arrives once the screen already left, would otherwise pop Inicio or Inventario and leave the
 * `NavHost` with nothing to show.
 */
private inline fun NavController.popIfOn(vararg routes: String, beforePop: () -> Unit = {}) {
    if (currentDestination?.route !in routes) return
    beforePop()
    popBackStack()
}

/**
 * Navigates only from a screen that is still resumed, as a single top: a second tap during the
 * transition would stack the same screen twice.
 */
private fun NavController.navigateOnce(route: String) {
    if (currentBackStackEntry?.lifecycle?.currentState != Lifecycle.State.RESUMED) return
    navigate(route) { launchSingleTop = true }
}
