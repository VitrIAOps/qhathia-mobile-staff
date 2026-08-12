package com.qhathia.mobile.staff.feature.productdetail.model

import androidx.annotation.StringRes

/** One-shot signals the product detail/edit screen turns into navigation or a snackbar. */
sealed interface ProductDetailUiEffect {
    /** The edit was persisted; the caller pops back and refreshes the inventory list. */
    data object NavigateBackSaved : ProductDetailUiEffect

    /** The merchant discarded or never edited; the caller just pops back. */
    data object NavigateBackCancelled : ProductDetailUiEffect

    data class ShowError(@param:StringRes val messageRes: Int) : ProductDetailUiEffect
}
