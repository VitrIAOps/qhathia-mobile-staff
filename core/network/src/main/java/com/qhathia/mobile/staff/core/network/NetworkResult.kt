package com.qhathia.mobile.staff.core.network

import io.ktor.client.plugins.ResponseException

/**
 * The three outcomes of any network call. The data layer maps this to a domain [Result] at the
 * repository boundary so that domain and presentation never see Ktor types.
 */
sealed class NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class HttpError(val code: Int, val message: String) : NetworkResult<Nothing>()
    data class NetworkError(val cause: Throwable) : NetworkResult<Nothing>()
}

/** Convert a [Result] (typically from `runCatching { … }`) into a [NetworkResult]. */
fun <T> Result<T>.toNetworkResult(): NetworkResult<T> = fold(
    onSuccess = { NetworkResult.Success(it) },
    onFailure = { throwable ->
        when (throwable) {
            is ResponseException -> NetworkResult.HttpError(
                code = throwable.response.status.value,
                message = throwable.response.status.description,
            )
            else -> NetworkResult.NetworkError(throwable)
        }
    },
)
