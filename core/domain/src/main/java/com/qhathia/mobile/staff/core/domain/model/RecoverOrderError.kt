package com.qhathia.mobile.staff.core.domain.model

/** Why bringing an expired order back to `PENDING` failed. */
sealed interface RecoverOrderError {
    /**
     * The order is not `EXPIRED` (`422 INVALID_STATE_TRANSITION`): it was rejected by hand, which
     * cannot be undone, or someone already recovered it.
     */
    data object NotRecoverable : RecoverOrderError

    /** The order does not exist, or belongs to another store (HTTP 404). */
    data object NotFound : RecoverOrderError

    /** No connectivity, timeout, or other transport failure — the action can be retried. */
    data object Network : RecoverOrderError

    /** Any other failure, carrying an optional diagnostic message. */
    data class Unknown(val message: String?) : RecoverOrderError
}

/** Carries a [RecoverOrderError] through a [Result] failure so callers can branch on the cause. */
class RecoverOrderException(val error: RecoverOrderError) : Exception()
