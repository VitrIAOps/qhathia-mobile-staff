package com.qhathia.mobile.staff.core.notifications

import com.qhathia.mobile.staff.core.domain.model.Order
import com.qhathia.mobile.staff.core.domain.model.OrderDetail
import com.qhathia.mobile.staff.core.domain.model.OrderItem
import com.qhathia.mobile.staff.core.domain.model.OrderStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant

class NewOrderContentTest {

    private val now = Instant.parse("2026-11-15T10:00:00Z")

    @Test
    fun `names the customer, total, products and minutes left, with the first three products`() {
        val content = NewOrderContent.from(
            detail(
                name = " Ana Torres ",
                total = 1250.5,
                expiresAt = now.plusSeconds(12 * 60),
                items = listOf(item("Taladro", 1), item("Brocas", 2), item("Martillo", 1), item("Clavos", 5)),
            ),
            now,
        )

        assertEquals("Ana Torres", content.customerName)
        assertEquals("S/\u00A01,250.50", content.total)
        assertEquals(4, content.productCount)
        assertEquals(12L, content.minutesLeft)
        assertEquals(listOf("1 × Taladro", "2 × Brocas", "1 × Martillo"), content.lines)
        assertEquals(1, content.moreLines)
    }

    @Test
    fun `a started minute counts`() {
        val content = NewOrderContent.from(detail(expiresAt = now.plusSeconds(30)), now)
        assertEquals(1L, content.minutesLeft)
    }

    @Test
    fun `no name, no deadline and a passed deadline leave those parts out`() {
        assertNull(NewOrderContent.from(detail(name = "  "), now).customerName)
        assertNull(NewOrderContent.from(detail(expiresAt = null), now).minutesLeft)
        assertNull(NewOrderContent.from(detail(expiresAt = now.minusSeconds(1)), now).minutesLeft)
    }

    @Test
    fun `without items the list count is used`() {
        val content = NewOrderContent.from(detail(items = emptyList(), itemCount = 3), now)
        assertEquals(3, content.productCount)
        assertEquals(0, content.moreLines)
    }

    private fun item(name: String, quantity: Int) = OrderItem(
        id = name, productId = name, productName = name, quantity = quantity, unitPrice = 1.0,
        lineTotal = quantity.toDouble(), productStock = null, productAvailability = null, productImageUrl = null,
    )

    private fun detail(
        name: String? = "Ana",
        total: Double = 10.0,
        expiresAt: Instant? = now.plusSeconds(600),
        items: List<OrderItem> = listOf(item("Taladro", 1)),
        itemCount: Int = items.size,
    ) = OrderDetail(
        order = Order(
            id = "o1", status = OrderStatus.PENDING, customerName = name, customerPhone = null, orderCode = "ABC",
            itemCount = itemCount, total = total, expiresAt = expiresAt, rejectionReason = null, createdAt = now,
        ),
        items = items,
    )
}
