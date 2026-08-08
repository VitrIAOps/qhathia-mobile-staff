package com.qhathia.mobile.staff.core.data.mapper

import com.qhathia.mobile.staff.core.domain.model.Availability
import com.qhathia.mobile.staff.core.domain.model.Order
import com.qhathia.mobile.staff.core.domain.model.OrderDetail
import com.qhathia.mobile.staff.core.domain.model.OrderItem
import com.qhathia.mobile.staff.core.domain.model.OrderPage
import com.qhathia.mobile.staff.core.domain.model.OrderStatus
import com.qhathia.mobile.staff.core.network.dto.OrderDetailDto
import com.qhathia.mobile.staff.core.network.dto.OrderItemDto
import com.qhathia.mobile.staff.core.network.dto.OrderPageDto
import com.qhathia.mobile.staff.core.network.dto.OrderSummaryDto
import java.time.Instant

/** Maps a page of order summaries, preserving `totalElements` for the Pendientes badge. */
fun OrderPageDto.toDomain(): OrderPage = OrderPage(
    orders = content.map { it.toDomain() },
    totalElements = totalElements,
)

/** Maps a transport [OrderSummaryDto] to the domain [Order]. */
fun OrderSummaryDto.toDomain(): Order = Order(
    id = id,
    status = status.toOrderStatus(),
    customerName = customerName?.takeIf { it.isNotBlank() },
    customerPhone = customerPhone?.takeIf { it.isNotBlank() },
    orderCode = orderCode?.takeIf { it.isNotBlank() },
    itemCount = itemCount,
    total = total,
    expiresAt = expiresAt.toInstantOrNull(),
    rejectionReason = rejectionReason?.takeIf { it.isNotBlank() },
    createdAt = createdAt.toInstantOrNull(),
)

/**
 * Maps the full order. The detail endpoint carries no `itemCount`, so it is derived from `items[]`.
 */
fun OrderDetailDto.toDomain(): OrderDetail = OrderDetail(
    order = Order(
        id = id,
        status = status.toOrderStatus(),
        customerName = customerName?.takeIf { it.isNotBlank() },
        customerPhone = customerPhone?.takeIf { it.isNotBlank() },
        orderCode = orderCode?.takeIf { it.isNotBlank() },
        itemCount = items.size,
        total = total,
        expiresAt = expiresAt.toInstantOrNull(),
        rejectionReason = rejectionReason?.takeIf { it.isNotBlank() },
        createdAt = createdAt.toInstantOrNull(),
    ),
    items = items.map { it.toDomain() },
)

/** Maps a transport [OrderItemDto] to the domain [OrderItem]. */
fun OrderItemDto.toDomain(): OrderItem = OrderItem(
    id = id,
    productId = productId,
    productName = productName,
    quantity = quantity,
    unitPrice = unitPriceSnapshot,
    lineTotal = lineTotal,
    productStock = productStock,
    productAvailability = productAvailability.toAvailabilityOrNull(),
)

/**
 * Unknown or absent status strings degrade to [OrderStatus.PENDING]. The list is already filtered
 * server-side by the status the screen asked for, so an unrecognized value can only mean the
 * backend added a state this build does not know; keeping the row visible and letting any action
 * resolve through the documented `422` is safer than hiding the order.
 */
private fun String?.toOrderStatus(): OrderStatus = when (this) {
    "PENDING" -> OrderStatus.PENDING
    "CONFIRMED" -> OrderStatus.CONFIRMED
    "REJECTED" -> OrderStatus.REJECTED
    "CANCELLED" -> OrderStatus.CANCELLED
    "EXPIRED" -> OrderStatus.EXPIRED
    else -> OrderStatus.PENDING
}

/**
 * Availability values this app does not model (e.g. `OUT_OF_STOCK`) map to null rather than being
 * folded into [Availability.CONSULT], which would wrongly read as "stock unknown".
 */
private fun String?.toAvailabilityOrNull(): Availability? = when (this) {
    "AVAILABLE" -> Availability.AVAILABLE
    "LOW_STOCK" -> Availability.LOW_STOCK
    "CONSULT" -> Availability.CONSULT
    else -> null
}

/** A malformed or absent timestamp degrades to null instead of failing the whole response. */
private fun String?.toInstantOrNull(): Instant? =
    this?.let { runCatching { Instant.parse(it) }.getOrNull() }
