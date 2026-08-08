package com.qhathia.mobile.staff.feature.orderrequests.model

/** User intents from the Solicitudes screen. */
sealed interface OrderRequestsUiEvent {
    data object Load : OrderRequestsUiEvent
    data object Retry : OrderRequestsUiEvent

    /** The header's manual refresh action — re-queries with the active filter and query. */
    data object Refresh : OrderRequestsUiEvent

    data class SelectFilter(val filter: OrderRequestsFilter) : OrderRequestsUiEvent
    data class Search(val query: String) : OrderRequestsUiEvent

    data class OpenDetail(val orderId: String) : OrderRequestsUiEvent

    /** Closes the dialog without acting on the order: no snackbar, no refresh. */
    data object DismissDetail : OrderRequestsUiEvent

    data class ConfirmOrder(val orderId: String) : OrderRequestsUiEvent

    /**
     * Opens the reject form. It arrives from both the card's Rechazar action and the detail
     * dialog's, so every rejection goes through the same motive step; [orderCode] only names the
     * order in the form's subtitle.
     */
    data class StartRejection(val orderId: String, val orderCode: String?) : OrderRequestsUiEvent

    data class SelectRejectionReason(val reason: RejectionReason) : OrderRequestsUiEvent

    /** Free text behind [RejectionReason.OTHER]. */
    data class EditOtherRejectionReason(val text: String) : OrderRequestsUiEvent

    /** Leaves the form: back to the detail it came from, or closes the dialog if it came from a card. */
    data object CancelRejection : OrderRequestsUiEvent

    /** Submits the rejection. [reason] is the resolved motive text sent to the backend. */
    data class ConfirmRejection(val reason: String) : OrderRequestsUiEvent
}
