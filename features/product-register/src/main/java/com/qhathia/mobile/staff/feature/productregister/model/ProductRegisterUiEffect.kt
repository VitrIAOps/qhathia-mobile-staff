package com.qhathia.mobile.staff.feature.productregister.model

import androidx.annotation.StringRes

/** One-shot side effects delivered via SharedFlow (never replayed). */
sealed interface ProductRegisterUiEffect {
    /**
     * Registration succeeded — return to inventory and show the success snackbar there.
     * [productId] is the new product's id, so inventory can locate its row.
     */
    data class NavigateBackWithSuccess(val productId: String) : ProductRegisterUiEffect

    /**
     * The product was registered but its discount could not be created — return to inventory, where
     * the product now exists as [productId], with a message saying so.
     */
    data class NavigateBackWithDiscountFailed(val productId: String) : ProductRegisterUiEffect

    /** Create was rejected for exceeding the plan limit — return to inventory with that message. */
    data object NavigateBackWithPlanLimit : ProductRegisterUiEffect

    /** Cancelled / discarded — return to inventory without a result. */
    data object NavigateBackCancelled : ProductRegisterUiEffect

    /** Leave the form and switch to the Solicitudes tab (header bell). */
    data object NavigateToOrderRequests : ProductRegisterUiEffect

    /** Show an error snackbar on the registration screen. */
    data class ShowError(@param:StringRes val messageRes: Int) : ProductRegisterUiEffect
}
