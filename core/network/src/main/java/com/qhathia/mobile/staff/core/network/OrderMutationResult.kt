package com.qhathia.mobile.staff.core.network

import com.qhathia.mobile.staff.core.network.dto.OrderDetailDto
import com.qhathia.mobile.staff.core.network.dto.OrderProblemDto

/**
 * Outcome of an order transition (confirm / reject).
 *
 * A plain [NetworkResult] is not enough here: the contract returns two different `422`s
 * (`STOCK_INSUFFICIENT` and `INVALID_STATE_TRANSITION`) that need different copy, and telling them
 * apart requires the ProblemDetail body rather than the status code. [HttpError] therefore carries
 * the parsed [problem] alongside the [code].
 */
sealed class OrderMutationResult {
    data class Success(val order: OrderDetailDto) : OrderMutationResult()

    /** Non-2xx. [problem] is null when the server sent no parseable ProblemDetail body. */
    data class HttpError(val code: Int, val problem: OrderProblemDto?) : OrderMutationResult()

    data class NetworkError(val cause: Throwable) : OrderMutationResult()
}
