package com.qhathia.mobile.staff.navigation

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
import androidx.compose.ui.res.stringResource
import com.qhathia.mobile.staff.core.ui.component.QhathiaBottomBar
import com.qhathia.mobile.staff.core.ui.component.QhathiaBottomBarItem
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.qhathia.mobile.staff.R
import com.qhathia.mobile.staff.core.common.navigation.DESTINATION_ORDER_REQUESTS
import com.qhathia.mobile.staff.core.ui.R as CoreUiR
import com.qhathia.mobile.staff.feature.productinventory.navigation.INVENTORY_REGISTER_RESULT_KEY
import com.qhathia.mobile.staff.feature.productinventory.navigation.INVENTORY_REGISTER_RESULT_PLAN_LIMIT
import com.qhathia.mobile.staff.feature.productinventory.navigation.INVENTORY_REGISTER_RESULT_SUCCESS
import com.qhathia.mobile.staff.feature.productinventory.navigation.INVENTORY_RESULT_PRODUCT_UPDATED
import com.qhathia.mobile.staff.feature.productinventory.navigation.INVENTORY_ROUTE
import com.qhathia.mobile.staff.feature.productinventory.navigation.inventoryGraph
import com.qhathia.mobile.staff.feature.productdetail.navigation.productDetailGraph
import com.qhathia.mobile.staff.feature.productdetail.navigation.productDetailRoute
import com.qhathia.mobile.staff.feature.productregister.navigation.PRODUCT_REGISTER_ROUTE
import com.qhathia.mobile.staff.feature.productregister.navigation.productRegisterGraph
import com.qhathia.mobile.staff.feature.devicelinking.navigation.DEVICE_LINKING_ROUTE
import com.qhathia.mobile.staff.feature.devicelinking.navigation.deviceLinkingGraph
import com.qhathia.mobile.staff.feature.orderrequests.navigation.ORDER_REQUESTS_ROUTE
import com.qhathia.mobile.staff.feature.orderrequests.navigation.orderRequestsGraph
import com.qhathia.mobile.staff.ui.home.HomePlaceholderScreen
import com.qhathia.mobile.staff.ui.profile.ProfileScreen

private const val HOME_ROUTE = "home"
private const val PROFILE_ROUTE = "profile"

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
 * destinations. Inventario is the start destination; Inventario, Solicitudes and Perfil are
 * implemented, Inicio is still a placeholder. Terminal pairing is reached from Perfil, so the DS
 * bottom bar carries only these four tabs.
 *
 * [pendingDestination] is a section requested by a notification tap. The inner NavHost lives here,
 * so this is the only place that can honour it; it does so as a normal tab switch and then reports
 * back through [onPendingDestinationConsumed].
 */
@Composable
fun MainShell(
    pendingDestination: String? = null,
    onPendingDestinationConsumed: () -> Unit = {},
) {
    RequestNotificationPermissionOnce()

    val navController = rememberNavController()
    val currentDestination by navController.currentBackStackEntryAsState()
    val currentRoute = currentDestination?.destination

    LaunchedEffect(pendingDestination) {
        if (pendingDestination != DESTINATION_ORDER_REQUESTS) return@LaunchedEffect
        navController.navigate(ORDER_REQUESTS_ROUTE) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
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
                    QhathiaBottomBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        iconRes = destination.iconRes,
                        label = stringResource(destination.labelRes),
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = INVENTORY_ROUTE,
            modifier = Modifier.padding(padding),
        ) {
            composable(HOME_ROUTE) { HomePlaceholderScreen() }
            inventoryGraph(
                onNavigateToRegister = { navController.navigate(PRODUCT_REGISTER_ROUTE) },
                onNavigateToProductDetail = { id -> navController.navigate(productDetailRoute(id)) },
            )
            productDetailGraph(
                onSaved = {
                    navController.previousBackStackEntry?.savedStateHandle
                        ?.set(INVENTORY_REGISTER_RESULT_KEY, INVENTORY_RESULT_PRODUCT_UPDATED)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() },
            )
            productRegisterGraph(
                onRegistered = {
                    navController.previousBackStackEntry?.savedStateHandle
                        ?.set(INVENTORY_REGISTER_RESULT_KEY, INVENTORY_REGISTER_RESULT_SUCCESS)
                    navController.popBackStack()
                },
                onPlanLimit = {
                    navController.previousBackStackEntry?.savedStateHandle
                        ?.set(INVENTORY_REGISTER_RESULT_KEY, INVENTORY_REGISTER_RESULT_PLAN_LIMIT)
                    navController.popBackStack()
                },
                onCancelled = { navController.popBackStack() },
            )
            orderRequestsGraph()
            composable(PROFILE_ROUTE) {
                ProfileScreen(
                    onNavigateToLinkDevices = { navController.navigate(DEVICE_LINKING_ROUTE) },
                )
            }
            deviceLinkingGraph(onBack = { navController.popBackStack() })
        }
    }
}
