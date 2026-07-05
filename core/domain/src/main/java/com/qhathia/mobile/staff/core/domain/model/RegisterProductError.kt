package com.qhathia.mobile.staff.core.domain.model

/**
 * Domain-level classification of why a product registration failed, decoupled from HTTP status
 * codes so the presentation layer can choose user-facing copy without knowing about the network.
 */
sealed interface RegisterProductError {
    /** The store's plan product limit was reached (HTTP 403 on create). */
    data object PlanLimitReached : RegisterProductError

    /** The server rejected the request payload (HTTP 400/422). */
    data object InvalidInput : RegisterProductError

    /** A selected image was not a valid JPG/PNG or could not be compressed to the size limit. */
    data object ImageTooLargeOrInvalid : RegisterProductError

    /** No connectivity, timeout, or other transport failure (API or R2). */
    data object Network : RegisterProductError

    /** Any other failure, carrying an optional diagnostic message. */
    data class Unknown(val message: String?) : RegisterProductError
}

/** Carries a [RegisterProductError] through a [Result] failure so callers can branch on the cause. */
class RegisterProductException(val error: RegisterProductError) : Exception()
