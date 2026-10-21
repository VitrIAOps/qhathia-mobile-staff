package com.qhathia.mobile.staff.feature.orderrequests.ui

import com.qhathia.mobile.staff.core.domain.model.Availability
import com.qhathia.mobile.staff.core.domain.model.Order
import com.qhathia.mobile.staff.core.domain.model.OrderDetail
import com.qhathia.mobile.staff.core.domain.model.OrderItem
import com.qhathia.mobile.staff.core.domain.model.OrderStatus
import java.time.Instant

/**
 * Sample data shared by this module's `@Preview`s. Previews render inside the main source set, so
 * these cannot come from `core:testing`.
 */
internal val PREVIEW_NOW: Instant = Instant.parse("2026-06-29T20:00:00Z")

internal fun previewOrder(
    id: String = "859653-3211-aaaa",
    customerName: String? = "Maria Murillo",
    orderCode: String? = "859653321",
    total: Double = 156.90,
    status: OrderStatus = OrderStatus.PENDING,
    expiresIn: Long? = 20 * 60L,
) = Order(
    id = id,
    status = status,
    customerName = customerName,
    customerPhone = "987654321",
    orderCode = orderCode,
    itemCount = 3,
    total = total,
    expiresAt = expiresIn?.let { PREVIEW_NOW.plusSeconds(it) },
    rejectionReason = null,
    createdAt = PREVIEW_NOW,
)

internal fun previewOrderItem(
    id: String = "item-1",
    unitPrice: Double = 50.0,
    quantity: Int = 1,
    productStock: Int? = 12,
    productImageUrl: String? = null,
) = OrderItem(
    id = id,
    productId = "product-$id",
    productName = "Taladro Percutor Bosch GSB 13 RE",
    quantity = quantity,
    unitPrice = unitPrice,
    lineTotal = unitPrice * quantity,
    productStock = productStock,
    productAvailability = Availability.AVAILABLE,
    productImageUrl = productImageUrl,
)

internal fun previewOrderDetail() = OrderDetail(
    order = previewOrder(total = 150.0),
    items = listOf(
        previewOrderItem(id = "item-1"),
        // The low-stock variant: a tracked stock at or below the warning threshold.
        previewOrderItem(id = "item-2", productStock = 2),
        // The sold-out variant: zero reads "Sin existencias", not "quedan pocos".
        previewOrderItem(id = "item-4", productStock = 0),
        // A consult-mode product — no tracked stock, so no warning.
        previewOrderItem(id = "item-3", unitPrice = 10.0, quantity = 5, productStock = null),
    ),
)
