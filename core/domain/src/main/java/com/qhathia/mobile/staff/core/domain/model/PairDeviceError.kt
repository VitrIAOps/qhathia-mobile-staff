package com.qhathia.mobile.staff.core.domain.model

/**
 * Domain-level classification of why pairing a device failed, decoupled from HTTP status codes so
 * the presentation layer can choose user-facing copy without knowing about the network.
 */
sealed interface PairDeviceError {
    /** The pairing code was invalid, expired, or already used (HTTP 400). */
    data object InvalidCode : PairDeviceError

    /** The store catalog is not ready to accept a device yet (HTTP 409/503). */
    data object CatalogNotReady : PairDeviceError

    /** No connectivity, timeout, or other transport failure. */
    data object Network : PairDeviceError

    /** Any other failure, carrying an optional diagnostic message. */
    data class Unknown(val message: String?) : PairDeviceError
}

/** Carries a [PairDeviceError] through a [Result] failure so callers can branch on the cause. */
class PairDeviceException(val error: PairDeviceError) : Exception()
