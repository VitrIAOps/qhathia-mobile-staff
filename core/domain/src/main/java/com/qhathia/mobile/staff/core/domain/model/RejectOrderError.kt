package com.qhathia.mobile.staff.core.domain.model

/**
 * Domain-level classification of why rejecting an order failed. Mirrors [ConfirmOrderError] minus
 * the stock case: rejecting never touches stock, so it can never fail with `STOCK_INSUFFICIENT`.
 */
sealed interface RejectOrderError {
    /**
     * The order is already in a terminal state (`422 INVALID_STATE_TRANSITION`) — another seller
     * confirmed or rejected it, or the TTL job expired it.
     */
    data object AlreadyHandled : RejectOrderError

    /** The order does not exist, or belongs to another store (HTTP 404). */
    data object NotFound : RejectOrderError

    /** No connectivity, timeout, or other transport failure — the action can be retried. */
    data object Network : RejectOrderError

    /** Any other failure, carrying an optional diagnostic message. */
    data class Unknown(val message: String?) : RejectOrderError
}

/** Carries a [RejectOrderError] through a [Result] failure so callers can branch on the cause. */
class RejectOrderException(val error: RejectOrderError) : Exception()
