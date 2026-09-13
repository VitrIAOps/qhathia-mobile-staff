package com.qhathia.mobile.staff.core.data.repository

import com.qhathia.mobile.staff.core.data.mapper.toDomain
import com.qhathia.mobile.staff.core.domain.model.ConfirmOrderError
import com.qhathia.mobile.staff.core.domain.model.ConfirmOrderException
import com.qhathia.mobile.staff.core.domain.model.OrderDetail
import com.qhathia.mobile.staff.core.domain.model.OrderPage
import com.qhathia.mobile.staff.core.domain.model.OrderStatus
import com.qhathia.mobile.staff.core.domain.model.RejectOrderError
import com.qhathia.mobile.staff.core.domain.model.RejectOrderException
import com.qhathia.mobile.staff.core.domain.repository.OrderRepository
import com.qhathia.mobile.staff.core.network.NetworkResult
import com.qhathia.mobile.staff.core.network.OrderMutationResult
import com.qhathia.mobile.staff.core.network.service.OrderApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Maps transport results to domain [Result]s at the data boundary, so the domain and presentation
 * layers never see Ktor or DTO types.
 *
 * The two transitions are where the mapping earns its keep: the backend answers both "someone else
 * bought the stock" and "someone else already handled this order" with a `422`, so the decision is
 * made on the ProblemDetail `errorCode` rather than the status code alone.
 *
 * It is a [Singleton] because it owns the app-wide pending-order count. That count follows the
 * data rather than the caller: any pending query updates it, so the Solicitudes screen's own load
 * feeds the Inicio bell and the Solicitudes tab badge without issuing a second request.
 */
@Singleton
class OrderRepositoryImpl @Inject constructor(
    private val apiService: OrderApiService,
) : OrderRepository {

    private val pendingCount = MutableStateFlow(0)

    override val pendingOrderCount: Flow<Int> = pendingCount.asStateFlow()

    override suspend fun refreshPendingOrderCount(): Result<Int> =
        when (val result = apiService.getOrders(
            statuses = listOf(OrderStatus.PENDING.name),
            query = null,
            page = FIRST_PAGE,
            size = COUNT_PAGE_SIZE,
        )) {
            is NetworkResult.Success -> {
                val total = result.data.totalElements
                pendingCount.value = total
                Result.success(total)
            }
            is NetworkResult.HttpError -> {
                // A rejected token means the count belonged to a session that is gone; keeping it
                // would let a stale badge outlive the merchant who earned it.
                if (result.code in AUTH_ERROR_CODES) pendingCount.value = 0
                Result.failure(
                    IllegalStateException("HTTP ${result.code}: ${result.message}"),
                )
            }
            is NetworkResult.NetworkError -> Result.failure(result.cause)
        }

    override suspend fun getOrders(
        statuses: List<OrderStatus>,
        query: String?,
        page: Int,
        size: Int,
    ): Result<OrderPage> {
        val result = apiService.getOrders(
            statuses = statuses.map { it.name },
            query = query?.trim()?.takeIf { it.isNotEmpty() },
            page = page,
            size = size,
        )
        return when (result) {
            is NetworkResult.Success -> {
                val orderPage = result.data.toDomain()
                // An unfiltered pending query already counted exactly what the badge needs. A
                // search-narrowed one did not, so it is deliberately not allowed to publish.
                if (statuses == PENDING_ONLY && query.isNullOrBlank()) {
                    pendingCount.value = orderPage.totalElements
                }
                Result.success(orderPage)
            }
            is NetworkResult.HttpError ->
                Result.failure(IllegalStateException("HTTP ${result.code}: ${result.message}"))
            is NetworkResult.NetworkError -> Result.failure(result.cause)
        }
    }

    override suspend fun getOrderDetail(id: String): Result<OrderDetail> =
        when (val result = apiService.getOrderDetail(id)) {
            is NetworkResult.Success -> Result.success(result.data.toDomain())
            is NetworkResult.HttpError ->
                Result.failure(IllegalStateException("HTTP ${result.code}: ${result.message}"))
            is NetworkResult.NetworkError -> Result.failure(result.cause)
        }

    override suspend fun confirmOrder(id: String): Result<OrderDetail> =
        when (val result = apiService.confirmOrder(id)) {
            is OrderMutationResult.Success -> {
                // One order just left PENDING; re-read the count so both badges move immediately.
                refreshPendingOrderCount()
                Result.success(result.order.toDomain())
            }
            is OrderMutationResult.HttpError ->
                Result.failure(ConfirmOrderException(result.toConfirmError()))
            is OrderMutationResult.NetworkError ->
                Result.failure(ConfirmOrderException(ConfirmOrderError.Network))
        }

    override suspend fun rejectOrder(id: String, reason: String?): Result<OrderDetail> =
        when (val result = apiService.rejectOrder(id, reason)) {
            is OrderMutationResult.Success -> {
                refreshPendingOrderCount()
                Result.success(result.order.toDomain())
            }
            is OrderMutationResult.HttpError ->
                Result.failure(RejectOrderException(result.toRejectError()))
            is OrderMutationResult.NetworkError ->
                Result.failure(RejectOrderException(RejectOrderError.Network))
        }

    private fun OrderMutationResult.HttpError.toConfirmError(): ConfirmOrderError = when {
        code == HTTP_NOT_FOUND -> ConfirmOrderError.NotFound
        problem?.errorCode == ERROR_STOCK_INSUFFICIENT -> ConfirmOrderError.StockInsufficient(
            productName = problem?.productName,
            available = problem?.available,
            requested = problem?.requested,
        )
        problem?.errorCode == ERROR_INVALID_STATE_TRANSITION -> ConfirmOrderError.AlreadyHandled
        else -> ConfirmOrderError.Unknown("HTTP $code")
    }

    private fun OrderMutationResult.HttpError.toRejectError(): RejectOrderError = when {
        code == HTTP_NOT_FOUND -> RejectOrderError.NotFound
        problem?.errorCode == ERROR_INVALID_STATE_TRANSITION -> RejectOrderError.AlreadyHandled
        else -> RejectOrderError.Unknown("HTTP $code")
    }

    private companion object {
        const val HTTP_NOT_FOUND = 404
        const val FIRST_PAGE = 0

        /** The count only needs `totalElements`, so it asks for the smallest possible page. */
        const val COUNT_PAGE_SIZE = 1

        /** The status filter whose `totalElements` *is* the pending count. */
        val PENDING_ONLY = listOf(OrderStatus.PENDING)
        val AUTH_ERROR_CODES = setOf(401, 403)
        const val ERROR_STOCK_INSUFFICIENT = "STOCK_INSUFFICIENT"
        const val ERROR_INVALID_STATE_TRANSITION = "INVALID_STATE_TRANSITION"
    }
}
