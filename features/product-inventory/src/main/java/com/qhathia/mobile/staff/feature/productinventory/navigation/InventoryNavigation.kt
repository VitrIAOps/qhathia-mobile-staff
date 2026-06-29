package com.qhathia.mobile.staff.feature.productinventory.navigation

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.qhathia.mobile.staff.feature.productinventory.ui.InventoryScreen

/** Stable route id for the inventory destination. */
const val INVENTORY_ROUTE = "inventory"

/** Key/values for the registration result passed back to inventory via `SavedStateHandle`. */
const val INVENTORY_REGISTER_RESULT_KEY = "inventory_register_result"
const val INVENTORY_REGISTER_RESULT_SUCCESS = "success"
const val INVENTORY_REGISTER_RESULT_PLAN_LIMIT = "plan_limit"

/**
 * Registers the inventory destination. [onNavigateToRegister] opens the registration screen; the
 * destination observes its own `SavedStateHandle` for a result set by the registration screen on the
 * way back (success → refresh + snackbar, plan-limit → message).
 */
fun NavGraphBuilder.inventoryGraph(onNavigateToRegister: () -> Unit) {
    composable(route = INVENTORY_ROUTE) { entry ->
        val registerResult by entry.savedStateHandle
            .getStateFlow<String?>(INVENTORY_REGISTER_RESULT_KEY, null)
            .collectAsStateWithLifecycle()
        InventoryScreen(
            onNavigateToRegister = onNavigateToRegister,
            registerResult = registerResult,
            onRegisterResultConsumed = {
                entry.savedStateHandle[INVENTORY_REGISTER_RESULT_KEY] = null
            },
        )
    }
}
