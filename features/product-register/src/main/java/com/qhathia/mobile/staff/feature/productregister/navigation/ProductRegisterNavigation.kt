package com.qhathia.mobile.staff.feature.productregister.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.qhathia.mobile.staff.feature.productregister.ui.ProductRegisterScreen

/** Stable route id for the product registration destination. */
const val PRODUCT_REGISTER_ROUTE = "product_register"

/**
 * Registers the product registration destination. [onRegistered] and [onPlanLimit] fire on a
 * successful registration and a plan-limit rejection respectively; [onDiscountFailed] when the
 * product was registered but its discount could not be applied; [onCancelled] on discard/cancel.
 * [onRegistered] and [onDiscountFailed] receive the id of the product that was created.
 */
fun NavGraphBuilder.productRegisterGraph(
    onRegistered: (String) -> Unit,
    onPlanLimit: () -> Unit,
    onDiscountFailed: (String) -> Unit,
    onCancelled: () -> Unit,
) {
    composable(route = PRODUCT_REGISTER_ROUTE) {
        ProductRegisterScreen(
            onRegistered = onRegistered,
            onPlanLimit = onPlanLimit,
            onDiscountFailed = onDiscountFailed,
            onCancelled = onCancelled,
        )
    }
}

/** Convenience navigation to the product registration destination. */
fun NavController.navigateToProductRegister() = navigate(PRODUCT_REGISTER_ROUTE)
