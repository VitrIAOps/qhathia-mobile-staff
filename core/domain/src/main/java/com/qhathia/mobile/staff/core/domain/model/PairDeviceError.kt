package com.qhathia.mobile.staff.core.domain.model

/**
 * Domain-level classification of why approving or rejecting a pairing code failed. The four code
 * errors share HTTP 400 and are told apart by the backend's `errorCode`, so the screen can say what
 * to do on the tablet.
 */
sealed interface PairDeviceError {
    /** No such code, or not a valid one (`INVALID_PAIRING_TOKEN`). */
    data object InvalidCode : PairDeviceError

    /** The code's window passed (`PAIRING_TOKEN_EXPIRED`). */
    data object ExpiredCode : PairDeviceError

    /** The code was already approved or rejected (`PAIRING_TOKEN_ALREADY_USED`). */
    data object AlreadyUsedCode : PairDeviceError

    /** The tablet replaced or dropped this code (`PAIRING_TOKEN_CANCELLED`). */
    data object CancelledCode : PairDeviceError

    /** The store catalog does not meet the threshold to accept a device (HTTP 409). */
    data class CatalogNotReady(val detail: String?) : PairDeviceError

    /** The catalog could not be checked; the backend fails closed (HTTP 503). */
    data object CatalogCheckUnavailable : PairDeviceError

    /** No connectivity, timeout, or other transport failure. */
    data object Network : PairDeviceError

    /** Any other failure, carrying an optional diagnostic message. */
    data class Unknown(val message: String?) : PairDeviceError
}

/** Carries a [PairDeviceError] through a [Result] failure so callers can branch on the cause. */
class PairDeviceException(val error: PairDeviceError) : Exception()
