package com.qhathia.mobile.staff.core.notifications

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OrderPushActionTest {

    @Test
    fun `NEW_ORDER posts the new-order notification`() {
        assertEquals(
            OrderPushAction.Post(OrderPushAction.Kind.NEW_ORDER),
            orderPushAction(EVENT_NEW_ORDER, "order-1"),
        )
    }

    @Test
    fun `ORDER_EXPIRING posts its own notification instead of the generic one`() {
        assertEquals(
            OrderPushAction.Post(OrderPushAction.Kind.EXPIRING),
            orderPushAction(EVENT_ORDER_EXPIRING, "order-1"),
        )
    }

    @Test
    fun `ORDER_TAKEN cancels the order's notification and posts nothing`() {
        assertEquals(OrderPushAction.Cancel("order-1"), orderPushAction(EVENT_ORDER_TAKEN, "order-1"))
    }

    @Test
    fun `ORDER_TAKEN without an order id does nothing`() {
        assertEquals(OrderPushAction.Ignore, orderPushAction(EVENT_ORDER_TAKEN, null))
    }

    @Test
    fun `an unknown event posts the generic notification`() {
        assertEquals(
            OrderPushAction.Post(OrderPushAction.Kind.GENERIC),
            orderPushAction("SOMETHING_NEW", "order-1"),
        )
    }

    @Test
    fun `a missing event posts the generic notification`() {
        assertEquals(OrderPushAction.Post(OrderPushAction.Kind.GENERIC), orderPushAction(null, null))
    }

    @Test
    fun `posting does not depend on the order id`() {
        assertEquals(orderPushAction(EVENT_NEW_ORDER, "order-1"), orderPushAction(EVENT_NEW_ORDER, null))
        assertEquals(orderPushAction(EVENT_ORDER_EXPIRING, "order-1"), orderPushAction(EVENT_ORDER_EXPIRING, null))
    }
}
