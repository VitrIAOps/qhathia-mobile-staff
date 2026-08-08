package com.qhathia.mobile.staff.core.testing

import com.qhathia.mobile.staff.core.domain.model.OrderDetail
import com.qhathia.mobile.staff.core.domain.model.OrderPage
import com.qhathia.mobile.staff.core.domain.model.OrderStatus
import com.qhathia.mobile.staff.core.domain.repository.OrderRepository

/**
 * Controllable [OrderRepository] for tests. Set the `*Result` fields to the value the next call
 * should return; every call is recorded in [calls] so tests can assert exactly which statuses,
 * query and page were requested.
 *
 * [ordersResultFor] takes precedence over [ordersResult] when it returns non-null, which is how a
 * test gives the `size=1` Pendientes badge query a different answer from the list query.
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

    override suspend fun getOrders(
        statuses: List<OrderStatus>,
        query: String?,
        page: Int,
        size: Int,
    ): Result<OrderPage> {
        val call = OrdersCall(statuses, query, page, size)
        calls += call
        return ordersResultFor?.invoke(call) ?: ordersResult
    }

    override suspend fun getOrderDetail(id: String): Result<OrderDetail> {
        requestedDetailIds += id
        return detailResult
    }

    override suspend fun confirmOrder(id: String): Result<OrderDetail> {
        confirmedIds += id
        return confirmResult
    }

    override suspend fun rejectOrder(id: String): Result<OrderDetail> {
        rejectedIds += id
        return rejectResult
    }
}
