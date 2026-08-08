package com.qhathia.mobile.staff.feature.orderrequests.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.qhathia.mobile.staff.feature.orderrequests.ui.OrderRequestsScreen

/** Stable route id for the Solicitudes destination. */
const val ORDER_REQUESTS_ROUTE = "order_requests"

/**
 * Registers the Solicitudes destination. The screen takes no arguments and hands nothing back — the
 * detail dialog lives inside it, so there is no result channel to wire.
 */
fun NavGraphBuilder.orderRequestsGraph() {
    composable(route = ORDER_REQUESTS_ROUTE) {
        OrderRequestsScreen()
    }
}
