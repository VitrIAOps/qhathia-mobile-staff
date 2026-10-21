package com.qhathia.mobile.staff.feature.productdetail.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.qhathia.mobile.staff.feature.productdetail.ui.ProductDetailScreen

/** Route argument carrying the id of the product being viewed. */
const val PRODUCT_DETAIL_ARG_ID = "productId"

/** Stable route pattern for the product detail destination. */
const val PRODUCT_DETAIL_ROUTE = "product_detail/{$PRODUCT_DETAIL_ARG_ID}"

/** Builds the concrete route for [productId]. Only the id travels — never the product itself. */
fun productDetailRoute(productId: String): String = "product_detail/$productId"

/**
 * Registers the product detail/edit destination. [onSaved] fires once the edit is persisted, so the
 * caller can hand a refresh signal back to the inventory list; [onDeleted] once the product was
 * deleted (`alreadyDeleted` when the server said it was already gone); [onBack] on discard, cancel
 * or a plain back press; [onNavigateToOrderRequests] when the header bell leaves for Solicitudes.
 */
fun NavGraphBuilder.productDetailGraph(
    onSaved: () -> Unit,
    onDeleted: (alreadyDeleted: Boolean) -> Unit,
    onBack: () -> Unit,
    onNavigateToOrderRequests: () -> Unit,
) {
    composable(
        route = PRODUCT_DETAIL_ROUTE,
        arguments = listOf(navArgument(PRODUCT_DETAIL_ARG_ID) { type = NavType.StringType }),
    ) {
        ProductDetailScreen(
            onSaved = onSaved,
            onDeleted = onDeleted,
            onBack = onBack,
            onNavigateToOrderRequests = onNavigateToOrderRequests,
        )
    }
}
