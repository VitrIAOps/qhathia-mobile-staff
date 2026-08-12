package com.qhathia.mobile.staff.feature.productinventory.navigation

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.qhathia.mobile.staff.feature.productinventory.ui.InventoryScreen

/** Stable route id for the inventory destination. */
const val INVENTORY_ROUTE = "inventory"

/**
 * Key/values for the result a pushed screen passes back to inventory via `SavedStateHandle`. The
 * registration and detail screens share one channel because inventory reacts to all of them the same
 * way — refresh and confirm.
 */
const val INVENTORY_REGISTER_RESULT_KEY = "inventory_register_result"
const val INVENTORY_REGISTER_RESULT_SUCCESS = "success"
const val INVENTORY_REGISTER_RESULT_PLAN_LIMIT = "plan_limit"
const val INVENTORY_RESULT_PRODUCT_UPDATED = "product_updated"

/**
 * Registers the inventory destination. [onNavigateToRegister] opens the registration screen and
 * [onNavigateToProductDetail] the detail/edit screen of the tapped product; the destination observes
 * its own `SavedStateHandle` for a result set by either on the way back (success/updated → refresh +
 * snackbar, plan-limit → message).
 */
fun NavGraphBuilder.inventoryGraph(
    onNavigateToRegister: () -> Unit,
    onNavigateToProductDetail: (String) -> Unit,
) {
    composable(route = INVENTORY_ROUTE) { entry ->
        val registerResult by entry.savedStateHandle
            .getStateFlow<String?>(INVENTORY_REGISTER_RESULT_KEY, null)
            .collectAsStateWithLifecycle()
        InventoryScreen(
            onNavigateToRegister = onNavigateToRegister,
            onNavigateToProductDetail = onNavigateToProductDetail,
            registerResult = registerResult,
            onRegisterResultConsumed = {
                entry.savedStateHandle[INVENTORY_REGISTER_RESULT_KEY] = null
            },
        )
    }
}
