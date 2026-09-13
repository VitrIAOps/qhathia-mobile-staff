package com.qhathia.mobile.staff.core.domain.repository

import com.qhathia.mobile.staff.core.domain.model.OrderDetail
import com.qhathia.mobile.staff.core.domain.model.OrderPage
import com.qhathia.mobile.staff.core.domain.model.OrderStatus
import kotlinx.coroutines.flow.Flow

/** Port for reading purchase orders and moving them out of `PENDING`. */
interface OrderRepository {

    /**
     * The app-wide number of orders still awaiting a decision, cached by the data layer so the
     * Inicio bell, the Solicitudes tab badge and the `Pendientes (N)` filter label all read one
     * value. It is kept fresh by [refreshPendingOrderCount], by every pending [getOrders] query and
     * by every successful [confirmOrder] / [rejectOrder]; no screen keeps its own copy.
     */
    val pendingOrderCount: Flow<Int>

    /**
     * Re-reads the pending count and publishes it to [pendingOrderCount]. Failing with an auth
     * error resets the cache to zero, so a count cannot outlive the session it was read under.
     */
    suspend fun refreshPendingOrderCount(): Result<Int>

    /**
     * Lists the store's orders, newest first. [statuses] is sent as the backend's comma-separated
     * `status` filter (empty = every status); [query] searches customer name and order-code prefix
     * server-side and is omitted when null.
     */
    suspend fun getOrders(
        statuses: List<OrderStatus>,
        query: String?,
        page: Int,
        size: Int,
    ): Result<OrderPage>

    /** Fetches the full order, including its items and their live stock. */
    suspend fun getOrderDetail(id: String): Result<OrderDetail>

    /**
     * Confirms the order (`PENDING → CONFIRMED`), decrementing each item's stock server-side.
     * Failures carry a `ConfirmOrderException`.
     */
    suspend fun confirmOrder(id: String): Result<OrderDetail>

    /**
     * Rejects the order (`PENDING → REJECTED`), recording [reason] when the merchant gave one.
     * Failures carry a `RejectOrderException`.
     */
    suspend fun rejectOrder(id: String, reason: String?): Result<OrderDetail>
}
