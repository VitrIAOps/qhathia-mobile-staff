package com.qhathia.mobile.staff.feature.productinventory.model

import androidx.annotation.StringRes

/** One-shot side effects delivered via SharedFlow (never replayed). */
sealed interface InventoryUiEffect {
    data class ShowError(@param:StringRes val messageRes: Int) : InventoryUiEffect

    /** Confirms an action that completed on this screen, e.g. a saved discount. */
    data class ShowMessage(@param:StringRes val messageRes: Int) : InventoryUiEffect

    /** Navigate to the product registration screen (from the registration FAB). */
    data object NavigateToRegister : InventoryUiEffect

    /** Navigate to the detail/edit screen of the tapped product. Only the id crosses the boundary. */
    data class NavigateToProductDetail(val productId: String) : InventoryUiEffect
}
