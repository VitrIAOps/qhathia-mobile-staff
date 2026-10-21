package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.model.OrderStatus
import com.qhathia.mobile.staff.core.domain.repository.OrderNotificationTray
import com.qhathia.mobile.staff.core.domain.repository.OrderRepository
import javax.inject.Inject

/**
 * Brings the tray's order notifications in line with the server's pending orders, and refreshes the
 * app-wide pending count on the way.
 *
 * Pushes are best-effort: an `ORDER_TAKEN` lost to Doze or a dead network leaves a notification for
 * an order nobody needs to handle, and the launcher badge counts it. Running this whenever the app
 * comes to the foreground clears such leftovers.
 *
 * It only ever removes notifications it can prove stale. If the read fails, or the server has more
 * pending orders than one page returns, order notifications are kept: hiding a pending order is
 * worse than a leftover that the next run clears. It never throws.
 */
class ReconcileOrderNotificationsUseCase @Inject constructor(
    private val repository: OrderRepository,
    private val tray: OrderNotificationTray,
) {
    suspend operator fun invoke() {
        tray.cancelUnmatched()

        val active = tray.activeOrderIds()
        if (active.isEmpty()) {
            repository.refreshPendingOrderCount()
            return
        }

        // An unfiltered pending query also publishes the pending count, so no second request.
        val page = repository.getOrders(
            statuses = listOf(OrderStatus.PENDING),
            query = null,
            page = 0,
            size = RECONCILE_PAGE_SIZE,
        ).getOrNull() ?: return
        if (page.totalElements > page.orders.size) return

        val pending = page.orders.mapTo(HashSet()) { it.id }
        (active - pending).forEach(tray::cancel)
    }

    companion object {
        /** Far above a store's realistic pending backlog; beyond it nothing is cancelled. */
        const val RECONCILE_PAGE_SIZE = 50
    }
}
