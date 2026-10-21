package com.qhathia.mobile.staff.feature.orderrequests.ui

import com.qhathia.mobile.staff.core.domain.model.OrderStatus
import com.qhathia.mobile.staff.feature.orderrequests.R
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

class OrderDetailFooterTest {

    private val now = Instant.parse("2026-10-21T15:00:00Z")
    private val future = now.plusSeconds(600)
    private val past = now.minusSeconds(1)

    @Test
    fun `pending before its deadline shows the actions`() {
        assertEquals(OrderDetailFooter.Actions, orderDetailFooter(OrderStatus.PENDING, future, now))
    }

    @Test
    fun `pending without a deadline shows the actions`() {
        assertEquals(OrderDetailFooter.Actions, orderDetailFooter(OrderStatus.PENDING, null, now))
    }

    @Test
    fun `pending past its deadline reads as expired`() {
        assertEquals(
            OrderDetailFooter.Resolved(R.string.order_detail_resolved_expired),
            orderDetailFooter(OrderStatus.PENDING, past, now),
        )
    }

    @Test
    fun `pending exactly at its deadline reads as expired`() {
        assertEquals(
            OrderDetailFooter.Resolved(R.string.order_detail_resolved_expired),
            orderDetailFooter(OrderStatus.PENDING, now, now),
        )
    }

    @Test
    fun `resolved statuses show their outcome`() {
        val expected = mapOf(
            OrderStatus.CONFIRMED to R.string.order_detail_resolved_confirmed,
            OrderStatus.REJECTED to R.string.order_detail_resolved_rejected,
            OrderStatus.CANCELLED to R.string.order_detail_resolved_cancelled,
            OrderStatus.EXPIRED to R.string.order_detail_resolved_expired,
        )
        expected.forEach { (status, messageRes) ->
            // The deadline does not matter once the order is resolved.
            assertEquals(OrderDetailFooter.Resolved(messageRes), orderDetailFooter(status, future, now))
            assertEquals(OrderDetailFooter.Resolved(messageRes), orderDetailFooter(status, null, now))
        }
    }
}
