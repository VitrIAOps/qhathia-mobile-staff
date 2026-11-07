package com.qhathia.mobile.staff.feature.productdetail.model

import androidx.annotation.StringRes

/** One-shot signals the product detail/edit screen turns into navigation or a snackbar. */
sealed interface ProductDetailUiEffect {
    /** The edit was persisted; the caller pops back and refreshes the inventory list. */
    data object NavigateBackSaved : ProductDetailUiEffect

    /**
     * The product was deleted; the caller pops back and inventory confirms it. [alreadyDeleted] when
     * the server said it was already gone (404).
     */
    data class NavigateBackDeleted(val alreadyDeleted: Boolean) : ProductDetailUiEffect

    /** The merchant discarded or never edited; the caller just pops back. */
    data object NavigateBackCancelled : ProductDetailUiEffect

    /** Leave the form and switch to the Solicitudes tab (header bell). */
    data object NavigateToOrderRequests : ProductDetailUiEffect

    /** [long] keeps the message up longer, for a partial save the merchant has to act on. */
    data class ShowError(@param:StringRes val messageRes: Int, val long: Boolean = false) : ProductDetailUiEffect
}
