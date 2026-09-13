package com.qhathia.mobile.staff.core.testing

import com.qhathia.mobile.staff.core.domain.model.OrderDetail
import com.qhathia.mobile.staff.core.domain.model.OrderPage
import com.qhathia.mobile.staff.core.domain.model.OrderStatus
import com.qhathia.mobile.staff.core.domain.repository.OrderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Controllable [OrderRepository] for tests. Set the `*Result` fields to the value the next call
 * should return; every call is recorded in [calls] so tests can assert exactly which statuses,
 * query and page were requested.
 *
 * [ordersResultFor] takes precedence over [ordersResult] when it returns non-null, which is how a
 * test gives the `size=1` pending count query a different answer from the list query.
 *
 * The pending count mirrors the real repository's contract rather than being inert: an unfiltered
 * pending [getOrders] publishes its `totalElements`, [refreshPendingOrderCount] publishes
 * [pendingCountResult], and a successful confirm or reject re-reads it. Tests that want to set the
 * count outright call [emitPendingCount].
 */
class FakeOrderRepository : OrderRepository {

    /** One recorded `getOrders` invocation. */
    data class OrdersCall(
        val statuses: List<OrderStatus>,
        val query: String?,
        val page: Int,
        val size: Int,
    )

    var ordersResult: Result<OrderPage> = Result.success(
        OrderPage(orders = listOf(TestFixtures.order()), totalElements = 1),
    )

    /** Per-call override, e.g. to answer the badge query with a different `totalElements`. */
    var ordersResultFor: ((OrdersCall) -> Result<OrderPage>?)? = null

    private val pendingCount = MutableStateFlow(0)

    override val pendingOrderCount: Flow<Int> = pendingCount.asStateFlow()

    /** What the next [refreshPendingOrderCount] returns; a success also publishes its value. */
    var pendingCountResult: Result<Int> = Result.success(0)

    var refreshPendingCountCallCount: Int = 0
        private set

    /** Pushes [count] straight onto [pendingOrderCount], bypassing any query. */
    fun emitPendingCount(count: Int) {
        pendingCount.value = count
    }

    var detailResult: Result<OrderDetail> = Result.success(TestFixtures.orderDetail())
    var confirmResult: Result<OrderDetail> = Result.success(TestFixtures.orderDetail())
    var rejectResult: Result<OrderDetail> = Result.success(TestFixtures.orderDetail())

    val calls: MutableList<OrdersCall> = mutableListOf()

    /** The list query of the most recent load, ignoring the `size=1` badge queries. */
    val lastListCall: OrdersCall? get() = calls.lastOrNull { it.size > 1 }

    var requestedDetailIds: MutableList<String> = mutableListOf()
        private set
    var confirmedIds: MutableList<String> = mutableListOf()
        private set
    var rejectedIds: MutableList<String> = mutableListOf()
        private set

    /** Reasons passed to [rejectOrder], in call order — null when the caller sent none. */
    var rejectedReasons: MutableList<String?> = mutableListOf()
        private set

    override suspend fun getOrders(
        statuses: List<OrderStatus>,
        query: String?,
        page: Int,
        size: Int,
    ): Result<OrderPage> {
        val call = OrdersCall(statuses, query, page, size)
        calls += call
        val result = ordersResultFor?.invoke(call) ?: ordersResult
        if (statuses == listOf(OrderStatus.PENDING) && query.isNullOrBlank()) {
            result.getOrNull()?.let { pendingCount.value = it.totalElements }
        }
        return result
    }

    override suspend fun refreshPendingOrderCount(): Result<Int> {
        refreshPendingCountCallCount++
        pendingCountResult.getOrNull()?.let { pendingCount.value = it }
        return pendingCountResult
    }

    override suspend fun getOrderDetail(id: String): Result<OrderDetail> {
        requestedDetailIds += id
        return detailResult
    }

    override suspend fun confirmOrder(id: String): Result<OrderDetail> {
        confirmedIds += id
        if (confirmResult.isSuccess) refreshPendingOrderCount()
        return confirmResult
    }

    override suspend fun rejectOrder(id: String, reason: String?): Result<OrderDetail> {
        rejectedIds += id
        rejectedReasons += reason
        if (rejectResult.isSuccess) refreshPendingOrderCount()
        return rejectResult
    }
}
