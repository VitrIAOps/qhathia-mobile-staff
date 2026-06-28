package com.qhathia.mobile.staff.core.domain.model

/**
 * Domain-level classification of why a login attempt failed, decoupled from HTTP status codes
 * so the presentation layer can choose user-facing copy without knowing about the network.
 */
sealed interface LoginError {
    /** Email/password rejected by the server (HTTP 401). */
    data object InvalidCredentials : LoginError

    /** The server rejected the request payload (HTTP 400). */
    data object Validation : LoginError

    /** No connectivity, timeout, or other transport failure. */
    data object Network : LoginError

    /** Any other failure, carrying an optional diagnostic message. */
    data class Unknown(val message: String?) : LoginError
}

/** Carries a [LoginError] through a [Result] failure so callers can branch on the cause. */
class LoginException(val error: LoginError) : Exception()
