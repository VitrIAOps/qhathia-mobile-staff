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
    rejectionReason: String? = null,
) = Order(
    id = id,
    status = status,
    customerName = customerName,
    customerPhone = "987654321",
    orderCode = orderCode,
    itemCount = 3,
    total = total,
    expiresAt = expiresIn?.let { PREVIEW_NOW.plusSeconds(it) },
    rejectionReason = rejectionReason,
    // Arrived 5 minutes before the preview's clock: "hace 5 min".
    createdAt = PREVIEW_NOW.minusSeconds(5 * 60L),
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
    productName = "Taladro percutor inalámbrico Bosch GSB 13 RE con maletín y 2 baterías",
    quantity = quantity,
    unitPrice = unitPrice,
    lineTotal = unitPrice * quantity,
    productStock = productStock,
    productAvailability = Availability.AVAILABLE,
    productImageUrl = productImageUrl,
)

internal fun previewOrderDetail(
    status: OrderStatus = OrderStatus.PENDING,
    expiresIn: Long? = 20 * 60L,
) = OrderDetail(
    order = previewOrder(total = 150.0, status = status, expiresIn = expiresIn),
    items = listOf(
        previewOrderItem(id = "item-1"),
        // The low-stock variant: a tracked stock at or below the warning threshold.
        previewOrderItem(id = "item-2", productStock = 2),
        // The sold-out variant: zero reads "Agotado", not "Pocas unidades".
        previewOrderItem(id = "item-4", productStock = 0),
        // A consult-mode product — no tracked stock, so no warning.
        previewOrderItem(id = "item-3", unitPrice = 10.0, quantity = 5, productStock = null),
    ),
)
