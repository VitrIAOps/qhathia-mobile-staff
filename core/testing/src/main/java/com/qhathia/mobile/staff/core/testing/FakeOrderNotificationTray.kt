package com.qhathia.mobile.staff.core.testing

import com.qhathia.mobile.staff.core.domain.repository.OrderNotificationTray

/**
 * In-memory [OrderNotificationTray]. Seed [activeIds] with the orders that have a notification;
 * [cancel] removes them and records the call in [cancelledIds].
 */
class FakeOrderNotificationTray(activeIds: Set<String> = emptySet()) : OrderNotificationTray {

    val activeIds: MutableSet<String> = activeIds.toMutableSet()

    val cancelledIds: MutableList<String> = mutableListOf()

    var cancelUnmatchedCallCount: Int = 0
        private set

    override fun activeOrderIds(): Set<String> = activeIds.toSet()

    override fun cancel(orderId: String) {
        cancelledIds += orderId
        activeIds -= orderId
    }

    override fun cancelUnmatched() {
        cancelUnmatchedCallCount++
    }
}
