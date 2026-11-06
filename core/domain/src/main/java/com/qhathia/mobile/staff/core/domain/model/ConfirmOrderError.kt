package com.qhathia.mobile.staff.core.domain.model

/**
 * Domain-level classification of why confirming an order failed, decoupled from HTTP status codes
 * so the presentation layer can choose user-facing copy without knowing about the network.
 *
 * Both conflict cases mean the merchant acted on a stale list, so the screen resolves them the same
 * way: show the message and re-query.
 */
sealed interface ConfirmOrderError {
    /**
     * Another sale consumed the stock between the dialog opening and the tap
     * (`422 STOCK_INSUFFICIENT`). The order stays `PENDING` — the backend rolls the whole
     * decrement back rather than confirming partially.
     */
    data class StockInsufficient(
        val productName: String?,
        val available: Int?,
        val requested: Int?,
    ) : ConfirmOrderError

    /**
     * The order is already in a terminal state (`422 INVALID_STATE_TRANSITION`) — another seller
     * confirmed or rejected it, or the TTL job expired it.
     */
    data object AlreadyHandled : ConfirmOrderError

    /**
     * A product of the order can no longer be sold — typically it was deleted (`409
     * PRODUCT_NOT_SELLABLE`). The order stays `PENDING`; the merchant should reject it.
     */
    data class ProductNotSellable(val productName: String?, val reason: String?) : ConfirmOrderError

    /** The order does not exist, or belongs to another store (HTTP 404). */
    data object NotFound : ConfirmOrderError

    /** No connectivity, timeout, or other transport failure — the action can be retried. */
    data object Network : ConfirmOrderError

    /** Any other failure, carrying an optional diagnostic message. */
    data class Unknown(val message: String?) : ConfirmOrderError
}

/** Carries a [ConfirmOrderError] through a [Result] failure so callers can branch on the cause. */
class ConfirmOrderException(val error: ConfirmOrderError) : Exception()
