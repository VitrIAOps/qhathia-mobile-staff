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

    /** Reject reaches here from both the card action and the dialog; no reason is sent. */
    data class RejectOrder(val orderId: String) : OrderRequestsUiEvent
}
