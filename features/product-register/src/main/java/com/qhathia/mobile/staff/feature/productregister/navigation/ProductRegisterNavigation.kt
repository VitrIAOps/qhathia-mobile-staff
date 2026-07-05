package com.qhathia.mobile.staff.feature.productregister.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.qhathia.mobile.staff.feature.productregister.ui.ProductRegisterScreen

/** Stable route id for the product registration destination. */
const val PRODUCT_REGISTER_ROUTE = "product_register"

/**
 * Registers the product registration destination. [onRegistered] and [onPlanLimit] fire on a
 * successful registration and a plan-limit rejection respectively; [onCancelled] on discard/cancel.
 */
fun NavGraphBuilder.productRegisterGraph(
    onRegistered: () -> Unit,
    onPlanLimit: () -> Unit,
    onCancelled: () -> Unit,
) {
    composable(route = PRODUCT_REGISTER_ROUTE) {
        ProductRegisterScreen(
            onRegistered = onRegistered,
            onPlanLimit = onPlanLimit,
            onCancelled = onCancelled,
        )
    }
}

/** Convenience navigation to the product registration destination. */
fun NavController.navigateToProductRegister() = navigate(PRODUCT_REGISTER_ROUTE)
