package com.qhathia.mobile.staff.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.qhathia.mobile.staff.R
import com.qhathia.mobile.staff.feature.productinventory.navigation.INVENTORY_ROUTE
import com.qhathia.mobile.staff.feature.productinventory.navigation.inventoryGraph
import com.qhathia.mobile.staff.ui.home.HomePlaceholderScreen
import com.qhathia.mobile.staff.ui.home.PlaceholderScreen

private const val HOME_ROUTE = "home"
private const val REQUESTS_ROUTE = "requests"
private const val DEVICES_ROUTE = "devices"
private const val PROFILE_ROUTE = "profile"

/** A bottom-navigation destination. */
private data class TopLevelDestination(
    val route: String,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
)

private val topLevelDestinations = listOf(
    TopLevelDestination(HOME_ROUTE, R.string.nav_home, Icons.Filled.Home),
    TopLevelDestination(INVENTORY_ROUTE, R.string.nav_inventory, Icons.AutoMirrored.Filled.List),
    TopLevelDestination(REQUESTS_ROUTE, R.string.nav_requests, Icons.Filled.Notifications),
    TopLevelDestination(DEVICES_ROUTE, R.string.nav_devices, Icons.Filled.PhoneAndroid),
    TopLevelDestination(PROFILE_ROUTE, R.string.nav_profile, Icons.Filled.Person),
)

/**
 * Post-login app shell: a bottom navigation bar over an inner [NavHost] of the top-level
 * destinations. Inventario is the start destination and the only fully-implemented tab; the others
 * are placeholders for this change.
 */
@Composable
fun MainShell() {
    val navController = rememberNavController()
    val currentDestination by navController.currentBackStackEntryAsState()
    val currentRoute = currentDestination?.destination

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                topLevelDestinations.forEach { destination ->
                    val selected = currentRoute?.hierarchy?.any { it.route == destination.route } == true
                    NavigationBarItem(
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
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = null,
                            )
                        },
                        label = { Text(stringResource(destination.labelRes)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.secondary,
                            selectedTextColor = MaterialTheme.colorScheme.secondary,
                            unselectedIconColor = MaterialTheme.colorScheme.tertiary,
                            unselectedTextColor = MaterialTheme.colorScheme.tertiary,
                            indicatorColor = Color.Transparent,
                        ),
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
            inventoryGraph()
            composable(REQUESTS_ROUTE) {
                PlaceholderScreen(title = stringResource(R.string.nav_requests))
            }
            composable(DEVICES_ROUTE) {
                PlaceholderScreen(title = stringResource(R.string.nav_devices))
            }
            composable(PROFILE_ROUTE) {
                PlaceholderScreen(title = stringResource(R.string.nav_profile))
            }
        }
    }
}
