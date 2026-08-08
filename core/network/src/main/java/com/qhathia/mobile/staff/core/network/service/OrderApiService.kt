package com.qhathia.mobile.staff.core.network.service

import com.qhathia.mobile.staff.core.network.NetworkResult
import com.qhathia.mobile.staff.core.network.OrderMutationResult
import com.qhathia.mobile.staff.core.network.dto.OrderDetailDto
import com.qhathia.mobile.staff.core.network.dto.OrderPageDto
import com.qhathia.mobile.staff.core.network.dto.OrderProblemDto
import com.qhathia.mobile.staff.core.network.toNetworkResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json

/**
 * Wraps the orders API. Runs on the authenticated client so the seller bearer token (and its
 * refresh) is injected by the Ktor Auth plugin. Optional list filters are only sent when non-null.
 */
class OrderApiService(
    private val client: HttpClient,
    private val json: Json,
) {

    /**
     * Lists orders. [statuses] is joined with commas into the backend's multi-value `status` param
     * (an empty list omits it, meaning every status); [query] feeds `q`, which searches customer
     * name and order-code prefix server-side.
     */
    suspend fun getOrders(
        statuses: List<String>,
        query: String?,
        page: Int?,
        size: Int?,
    ): NetworkResult<OrderPageDto> =
        runCatching {
            client.get("api/v1/orders") {
                statuses.takeIf { it.isNotEmpty() }
                    ?.let { parameter("status", it.joinToString(",")) }
                query?.let { parameter("q", it) }
                page?.let { parameter("page", it) }
                size?.let { parameter("size", it) }
            }.body<OrderPageDto>()
        }.toNetworkResult()

    suspend fun getOrderDetail(orderId: String): NetworkResult<OrderDetailDto> =
        runCatching {
            client.get("api/v1/orders/$orderId").body<OrderDetailDto>()
        }.toNetworkResult()

    /**
     * Confirms the order. On failure the ProblemDetail body is read so the caller can tell
     * `STOCK_INSUFFICIENT` from `INVALID_STATE_TRANSITION` — both are `422`.
     */
    suspend fun confirmOrder(orderId: String): OrderMutationResult =
        mutate("api/v1/orders/$orderId/confirm")

    /** Rejects the order. No body is sent — this screen never asks for a rejection reason. */
    suspend fun rejectOrder(orderId: String): OrderMutationResult =
        mutate("api/v1/orders/$orderId/reject")

    private suspend fun mutate(path: String): OrderMutationResult =
        runCatching { client.post(path).body<OrderDetailDto>() }.fold(
            onSuccess = { OrderMutationResult.Success(it) },
            onFailure = { throwable ->
                when (throwable) {
                    is ResponseException -> OrderMutationResult.HttpError(
                        code = throwable.response.status.value,
                        problem = throwable.response.parseProblem(),
                    )
                    else -> OrderMutationResult.NetworkError(throwable)
                }
            },
        )

    /** Best-effort ProblemDetail parse: a missing or malformed error body degrades to null. */
    private suspend fun HttpResponse.parseProblem(): OrderProblemDto? =
        runCatching { json.decodeFromString<OrderProblemDto>(bodyAsText()) }.getOrNull()
}
