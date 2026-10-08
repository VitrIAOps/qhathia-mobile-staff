package com.qhathia.mobile.staff.core.data.mapper

import com.qhathia.mobile.staff.core.domain.model.Availability
import com.qhathia.mobile.staff.core.domain.model.OrderStatus
import com.qhathia.mobile.staff.core.network.dto.OrderDetailDto
import com.qhathia.mobile.staff.core.network.dto.OrderItemDto
import com.qhathia.mobile.staff.core.network.dto.OrderPageDto
import com.qhathia.mobile.staff.core.network.dto.OrderSummaryDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant

class OrderMappersTest {

    private fun summaryDto(
        status: String? = "PENDING",
        customerName: String? = "Maria Murillo",
        orderCode: String? = "859653",
        expiresAt: String? = "2026-06-29T21:00:00Z",
        createdAt: String? = "2026-06-29T20:00:00Z",
    ) = OrderSummaryDto(
        id = "order-1",
        status = status,
        customerName = customerName,
        customerPhone = "987654321",
        orderCode = orderCode,
        itemCount = 2,
        total = 156.90,
        expiresAt = expiresAt,
        rejectionReason = null,
        createdAt = createdAt,
    )

    private fun itemDto(
        productStock: Int? = 3,
        productAvailability: String? = "LOW_STOCK",
        productImageUrl: String? = null,
    ) = OrderItemDto(
        id = "item-1",
        productId = "product-1",
        productName = "Foco LED Bosch 9W",
        quantity = 2,
        unitPriceSnapshot = 25.0,
        lineTotal = 50.0,
        productStock = productStock,
        productAvailability = productAvailability,
        productImageUrl = productImageUrl,
    )

    @Test
    fun `summary maps every field and parses timestamps`() {
        val order = summaryDto().toDomain()

        assertEquals("order-1", order.id)
        assertEquals(OrderStatus.PENDING, order.status)
        assertEquals("Maria Murillo", order.customerName)
        assertEquals("987654321", order.customerPhone)
        assertEquals("859653", order.orderCode)
        assertEquals(2, order.itemCount)
        assertEquals(156.90, order.total)
        assertEquals(Instant.parse("2026-06-29T21:00:00Z"), order.expiresAt)
        assertEquals(Instant.parse("2026-06-29T20:00:00Z"), order.createdAt)
    }

    @Test
    fun `null orderCode and customerName stay null for the display fallbacks`() {
        val order = summaryDto(orderCode = null, customerName = null).toDomain()

        assertNull(order.orderCode)
        assertNull(order.customerName)
    }

    @Test
    fun `blank customerName is treated as absent`() {
        val order = summaryDto(customerName = "   ").toDomain()

        assertNull(order.customerName)
    }

    @Test
    fun `EXPIRED status maps to the domain enum`() {
        val order = summaryDto(status = "EXPIRED").toDomain()

        assertEquals(OrderStatus.EXPIRED, order.status)
    }

    @Test
    fun `unknown status degrades to PENDING instead of failing`() {
        val order = summaryDto(status = "SOMETHING_NEW").toDomain()

        assertEquals(OrderStatus.PENDING, order.status)
    }

    @Test
    fun `absent expiresAt maps to null`() {
        val order = summaryDto(expiresAt = null).toDomain()

        assertNull(order.expiresAt)
    }

    @Test
    fun `malformed timestamp degrades to null rather than throwing`() {
        val order = summaryDto(expiresAt = "not-a-timestamp").toDomain()

        assertNull(order.expiresAt)
    }

    @Test
    fun `page keeps totalElements for the pending badge`() {
        val page = OrderPageDto(content = listOf(summaryDto()), totalElements = 7).toDomain()

        assertEquals(1, page.orders.size)
        assertEquals(7, page.totalElements)
    }

    @Test
    fun `detail maps items and derives itemCount from them`() {
        val dto = OrderDetailDto(
            id = "order-1",
            status = "PENDING",
            customerName = "Maria Murillo",
            orderCode = "859653",
            total = 50.0,
            items = listOf(itemDto(), itemDto()),
        )

        val detail = dto.toDomain()

        assertEquals(2, detail.items.size)
        assertEquals(2, detail.order.itemCount)
        assertEquals("859653", detail.order.orderCode)
    }

    @Test
    fun `item maps unitPriceSnapshot to unitPrice and keeps live stock`() {
        val item = itemDto().toDomain()

        assertEquals("Foco LED Bosch 9W", item.productName)
        assertEquals(2, item.quantity)
        assertEquals(25.0, item.unitPrice)
        assertEquals(50.0, item.lineTotal)
        assertEquals(3, item.productStock)
        assertEquals(Availability.LOW_STOCK, item.productAvailability)
    }

    @Test
    fun `null productStock stays null so consult-mode items show no warning`() {
        val item = itemDto(productStock = null, productAvailability = "CONSULT").toDomain()

        assertNull(item.productStock)
        assertEquals(Availability.CONSULT, item.productAvailability)
    }

    @Test
    fun `unmodelled availability maps to null rather than being folded into CONSULT`() {
        val item = itemDto(productAvailability = "OUT_OF_STOCK").toDomain()

        assertNull(item.productAvailability)
    }

    @Test
    fun `item keeps the product image url`() {
        val item = itemDto(productImageUrl = "https://cdn.test/taladro.jpg").toDomain()

        assertEquals("https://cdn.test/taladro.jpg", item.productImageUrl)
    }

    @Test
    fun `null product image url stays null so the dialog shows the placeholder`() {
        val item = itemDto(productImageUrl = null).toDomain()

        assertNull(item.productImageUrl)
    }

    @Test
    fun `blank product image url is treated as absent`() {
        val item = itemDto(productImageUrl = "   ").toDomain()

        assertNull(item.productImageUrl)
    }
}
