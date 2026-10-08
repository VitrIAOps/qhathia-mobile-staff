package com.qhathia.mobile.staff.core.domain.model

/**
 * Domain-level classification of why reading or changing a product's discount failed, decoupled
 * from HTTP status codes. Mirrors [UpdateProductError].
 */
sealed interface DiscountError {
    /** The product or the discount no longer exists (HTTP 404). */
    data object NotFound : DiscountError

    /** The server rejected the discount (HTTP 400/422). */
    data object InvalidInput : DiscountError

    /** No connectivity, timeout, or other transport failure. */
    data object Network : DiscountError

    /** Any other failure, carrying an optional diagnostic message. */
    data class Unknown(val message: String?) : DiscountError
}

/** Carries a [DiscountError] through a [Result] failure so callers can branch on the cause. */
class DiscountException(val error: DiscountError) : Exception()
