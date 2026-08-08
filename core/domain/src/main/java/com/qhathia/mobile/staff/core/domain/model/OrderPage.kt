package com.qhathia.mobile.staff.core.domain.model

/**
 * One page of orders. [totalElements] counts every order matching the query, not just the ones in
 * [orders] — the Pendientes badge reads it from a `size=1` pending query, since the backend exposes
 * no dedicated count endpoint.
 */
data class OrderPage(
    val orders: List<Order>,
    val totalElements: Int,
)
