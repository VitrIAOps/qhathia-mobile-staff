package com.qhathia.mobile.staff.feature.orderrequests.model

import androidx.annotation.StringRes

/** Whether a snackbar reads as a completed action or as a problem. */
enum class SnackbarSeverity { SUCCESS, ERROR }

/**
 * One-shot side effects delivered via SharedFlow (never replayed). Each carries its [severity] so
 * the screen colours the snackbar without re-deriving the meaning of the effect — green for a
 * confirmation, red for a rejection or an error, per the design.
 */
sealed interface OrderRequestsUiEffect {

    val severity: SnackbarSeverity

    data object ShowConfirmedSnackbar : OrderRequestsUiEffect {
        override val severity = SnackbarSeverity.SUCCESS
    }

    data object ShowRejectedSnackbar : OrderRequestsUiEffect {
        override val severity = SnackbarSeverity.ERROR
    }

    /**
     * [productName] is only set by the out-of-stock conflict, whose message names the product that
     * ran out; every other message is a plain string resource.
     */
    data class ShowError(
        @param:StringRes val messageRes: Int,
        val productName: String? = null,
    ) : OrderRequestsUiEffect {
        override val severity = SnackbarSeverity.ERROR
    }
}
