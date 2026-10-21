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

/** The product was registered, but the discount chosen on the form could not be created. */
const val INVENTORY_REGISTER_RESULT_DISCOUNT_FAILED = "discount_failed"
const val INVENTORY_RESULT_PRODUCT_UPDATED = "product_updated"

/** The edit screen deleted the product; "already" when the server said it was gone (404). */
const val INVENTORY_RESULT_PRODUCT_DELETED = "product_deleted"
const val INVENTORY_RESULT_PRODUCT_ALREADY_DELETED = "product_already_deleted"

/**
 * Id of the product a registration just created, set alongside a success or discount-failed result
 * (and before it, so it is there when the result is observed) so inventory can scroll to its row.
 */
const val INVENTORY_REGISTERED_PRODUCT_ID_KEY = "inventory_registered_product_id"

/**
 * Registers the inventory destination. [onNavigateToRegister] opens the registration screen and
 * [onNavigateToProductDetail] the detail/edit screen of the tapped product, and
 * [onNavigateToOrderRequests] switches to the Solicitudes tab (the header bell); the destination observes
 * its own `SavedStateHandle` for a result set by either on the way back (success/updated → refresh +
 * snackbar, plan-limit → message). A registration also hands back the new product's id.
 */
fun NavGraphBuilder.inventoryGraph(
    onNavigateToRegister: () -> Unit,
    onNavigateToProductDetail: (String) -> Unit,
    onNavigateToOrderRequests: () -> Unit,
) {
    composable(route = INVENTORY_ROUTE) { entry ->
        val registerResult by entry.savedStateHandle
            .getStateFlow<String?>(INVENTORY_REGISTER_RESULT_KEY, null)
            .collectAsStateWithLifecycle()
        InventoryScreen(
            onNavigateToRegister = onNavigateToRegister,
            onNavigateToProductDetail = onNavigateToProductDetail,
            onNavigateToOrderRequests = onNavigateToOrderRequests,
            registerResult = registerResult,
            registeredProductId = entry.savedStateHandle[INVENTORY_REGISTERED_PRODUCT_ID_KEY],
            onRegisterResultConsumed = {
                entry.savedStateHandle[INVENTORY_REGISTER_RESULT_KEY] = null
                entry.savedStateHandle[INVENTORY_REGISTERED_PRODUCT_ID_KEY] = null
            },
        )
    }
}
