package com.qhathia.mobile.staff.feature.home.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.qhathia.mobile.staff.core.domain.model.SalesPeriod
import com.qhathia.mobile.staff.feature.home.ui.HomeScreen

/** Stable route id for the Inicio destination. */
const val HOME_ROUTE = "home"

/**
 * Registers the Inicio destination.
 *
 * [onNavigateToOrderRequests] is raised by the header bell. The shell must fulfil it with the same
 * options the bottom bar uses for a tab switch — the bell means "go to Solicitudes", not "stack a
 * Solicitudes screen on top of Inicio".
 *
 * [onNavigateToSalesStatistics] is raised by `Ver más` with the period the card shows. It is a plain
 * push, so back returns to Inicio. This feature cannot see the statistics route; the shell builds it.
 *
 * [onNavigateToProductRegister] (`Agregar`, with the searched name) and [onNavigateToProductDetail]
 * (`Reabastecer`, with the product id) should open over Inventario, so saving lands on its list and
 * snackbar rather than back on Inicio.
 */
fun NavGraphBuilder.homeGraph(
    onNavigateToOrderRequests: () -> Unit,
    onNavigateToSalesStatistics: (SalesPeriod) -> Unit,
    onNavigateToProductRegister: (prefillName: String) -> Unit,
    onNavigateToProductDetail: (productId: String) -> Unit,
) {
    composable(route = HOME_ROUTE) {
        HomeScreen(
            onNavigateToOrderRequests = onNavigateToOrderRequests,
            onNavigateToSalesStatistics = onNavigateToSalesStatistics,
            onNavigateToProductRegister = onNavigateToProductRegister,
            onNavigateToProductDetail = onNavigateToProductDetail,
        )
    }
}
