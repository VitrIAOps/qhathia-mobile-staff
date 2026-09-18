package com.qhathia.mobile.staff.core.data.repository

import com.qhathia.mobile.staff.core.network.NetworkResult

/**
 * Maps a transport [NetworkResult] to a domain [Result] at the data boundary: a success is
 * [transform]ed (a mapping failure becomes a failed result), an HTTP error becomes an
 * [IllegalStateException] naming the status, and a transport error keeps its cause.
 */
internal fun <T, R> NetworkResult<T>.asDomainResult(transform: (T) -> R): Result<R> = when (this) {
    is NetworkResult.Success -> runCatching { transform(data) }
    is NetworkResult.HttpError -> Result.failure(IllegalStateException("HTTP $code: $message"))
    is NetworkResult.NetworkError -> Result.failure(cause)
}
