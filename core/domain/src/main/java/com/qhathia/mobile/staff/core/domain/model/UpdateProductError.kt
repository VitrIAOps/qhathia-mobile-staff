package com.qhathia.mobile.staff.core.domain.model

/**
 * Domain-level classification of why loading or saving a product detail failed, decoupled from HTTP
 * status codes so the presentation layer can choose user-facing copy without knowing about the
 * network. Mirrors [RegisterProductError] minus the create-only cases.
 */
sealed interface UpdateProductError {
    /** The product no longer exists — it was deleted or deactivated elsewhere (HTTP 404). */
    data object NotFound : UpdateProductError

    /** The server rejected the request payload (HTTP 400/422). */
    data object InvalidInput : UpdateProductError

    /** No connectivity, timeout, or other transport failure. */
    data object Network : UpdateProductError

    /** Any other failure, carrying an optional diagnostic message. */
    data class Unknown(val message: String?) : UpdateProductError
}

/** Carries an [UpdateProductError] through a [Result] failure so callers can branch on the cause. */
class UpdateProductException(val error: UpdateProductError) : Exception()
