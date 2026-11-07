package com.qhathia.mobile.staff.feature.productregister.navigation

import android.net.Uri
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.qhathia.mobile.staff.feature.productregister.ui.ProductRegisterScreen

/** Optional name the form starts with ("Agregar" on a missing-product suggestion). */
const val PRODUCT_REGISTER_ARG_NAME = "name"

private const val PRODUCT_REGISTER_BASE = "product_register"

/**
 * Route pattern of the product registration destination, as `currentDestination.route` reports it.
 * Navigate with [productRegisterRoute], never with this pattern: it would send the literal `{name}`.
 */
const val PRODUCT_REGISTER_ROUTE = "$PRODUCT_REGISTER_BASE?$PRODUCT_REGISTER_ARG_NAME={$PRODUCT_REGISTER_ARG_NAME}"

/** The route to open the form, empty or with `Nombre` set to [prefillName]. */
fun productRegisterRoute(prefillName: String? = null): String =
    if (prefillName.isNullOrBlank()) {
        PRODUCT_REGISTER_BASE
    } else {
        "$PRODUCT_REGISTER_BASE?$PRODUCT_REGISTER_ARG_NAME=${Uri.encode(prefillName)}"
    }

/**
 * Registers the product registration destination. [onRegistered] and [onPlanLimit] fire on a
 * successful registration and a plan-limit rejection respectively; [onDiscountFailed] when the
 * product was registered but its discount could not be applied; [onCancelled] on discard/cancel,
 * and [onNavigateToOrderRequests] when the header bell leaves for Solicitudes.
 * [onRegistered] and [onDiscountFailed] receive the id of the product that was created.
 */
fun NavGraphBuilder.productRegisterGraph(
    onRegistered: (String) -> Unit,
    onPlanLimit: () -> Unit,
    onDiscountFailed: (String) -> Unit,
    onCancelled: () -> Unit,
    onNavigateToOrderRequests: () -> Unit,
) {
    composable(
        route = PRODUCT_REGISTER_ROUTE,
        arguments = listOf(
            navArgument(PRODUCT_REGISTER_ARG_NAME) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
        ),
    ) {
        ProductRegisterScreen(
            onRegistered = onRegistered,
            onPlanLimit = onPlanLimit,
            onDiscountFailed = onDiscountFailed,
            onCancelled = onCancelled,
            onNavigateToOrderRequests = onNavigateToOrderRequests,
        )
    }
}

/** Convenience navigation to the product registration destination. */
fun NavController.navigateToProductRegister(prefillName: String? = null) =
    navigate(productRegisterRoute(prefillName))
