package com.qhathia.mobile.staff.feature.home.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.qhathia.mobile.staff.feature.home.ui.HomeScreen

/** Stable route id for the Inicio destination. */
const val HOME_ROUTE = "home"

/**
 * Registers the Inicio destination.
 *
 * [onNavigateToOrderRequests] is raised by the header bell. The shell must fulfil it with the same
 * options the bottom bar uses for a tab switch — the bell means "go to Solicitudes", not "stack a
 * Solicitudes screen on top of Inicio".
 */
fun NavGraphBuilder.homeGraph(onNavigateToOrderRequests: () -> Unit) {
    composable(route = HOME_ROUTE) {
        HomeScreen(onNavigateToOrderRequests = onNavigateToOrderRequests)
    }
}
