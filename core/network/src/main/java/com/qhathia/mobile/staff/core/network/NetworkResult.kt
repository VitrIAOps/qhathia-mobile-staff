package com.qhathia.mobile.staff.core.network

import com.qhathia.mobile.staff.core.network.dto.ProblemDto
import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException

/**
 * The three outcomes of any network call. The data layer maps this to a domain [Result] at the
 * repository boundary so that domain and presentation never see Ktor types.
 */
sealed class NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>()

    /**
     * A non-2xx answer. [problem] is the backend's ProblemDetail when the call was mapped with
     * [toNetworkResult] (`json` overload) and the body was one; `null` means the body was not read,
     * was empty or was not a ProblemDetail. Read `problem?.errorCode` before [code] when two errors
     * share a status.
     */
    data class HttpError(
        val code: Int,
        val message: String,
        val problem: ProblemDto? = null,
    ) : NetworkResult<Nothing>()

    data class NetworkError(val cause: Throwable) : NetworkResult<Nothing>()
}

/**
 * Convert a [Result] (typically from `runCatching { … }`) into a [NetworkResult]. It does not read
 * the error body; use the `json` overload when the caller needs the ProblemDetail.
 */
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

/**
 * Like [toNetworkResult], but on an HTTP error it also reads the ProblemDetail body (best effort)
 * into [NetworkResult.HttpError.problem].
 */
suspend fun <T> Result<T>.toNetworkResult(json: Json): NetworkResult<T> = fold(
    onSuccess = { NetworkResult.Success(it) },
    onFailure = { throwable ->
        when (throwable) {
            is ResponseException -> NetworkResult.HttpError(
                code = throwable.response.status.value,
                message = throwable.response.status.description,
                problem = throwable.response.readProblem(json),
            )
            else -> NetworkResult.NetworkError(throwable)
        }
    },
)

/** A missing, empty or malformed body degrades to `null`; cancellation is never swallowed. */
private suspend fun HttpResponse.readProblem(json: Json): ProblemDto? =
    try {
        val text = bodyAsText()
        if (text.isBlank()) null else json.decodeFromString<ProblemDto>(text)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
