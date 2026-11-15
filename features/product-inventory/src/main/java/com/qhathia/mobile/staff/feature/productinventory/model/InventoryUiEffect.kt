package com.qhathia.mobile.staff.feature.productinventory.model

import androidx.annotation.StringRes

/** One-shot side effects delivered via SharedFlow (never replayed). */
sealed interface InventoryUiEffect {
    data class ShowError(@param:StringRes val messageRes: Int) : InventoryUiEffect

    /** Confirms an action that completed on this screen, e.g. a saved discount. */
    data class ShowMessage(@param:StringRes val messageRes: Int) : InventoryUiEffect

    /** Navigate to the product registration screen (from the registration FAB). */
    /** Open the registration form, with Nombre set to [prefillName] when it comes from a search. */
    data class NavigateToRegister(val prefillName: String? = null) : InventoryUiEffect

    /** Navigate to the detail/edit screen of the tapped product. Only the id crosses the boundary. */
    data class NavigateToProductDetail(val productId: String) : InventoryUiEffect

    /** Scroll the product list to the row at [index] of the visible products. */
    data class ScrollToProduct(val index: Int) : InventoryUiEffect
}
