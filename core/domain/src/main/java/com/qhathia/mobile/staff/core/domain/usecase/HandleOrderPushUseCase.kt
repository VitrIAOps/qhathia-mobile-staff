package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.repository.OrderRepository
import javax.inject.Inject

/**
 * Syncs the app with an order push. `NEW_ORDER` and `ORDER_TAKEN` change which orders are pending,
 * so open lists are told to re-query and the shared pending count is re-read, which moves every
 * badge even when Solicitudes is not on screen. `ORDER_EXPIRING` changes nothing in the list.
 *
 * Fail-open: a push that cannot refresh anything must never break the FCM callback.
 */
class HandleOrderPushUseCase @Inject constructor(
    private val repository: OrderRepository,
) {
    suspend operator fun invoke(event: String?) {
        if (event != EVENT_NEW_ORDER && event != EVENT_ORDER_TAKEN) return
        repository.notifyOrdersChanged()
        // A count request already running may have left before this order existed.
        runCatching { repository.refreshPendingOrderCount(reuseInFlight = false) }
    }

    private companion object {
        const val EVENT_NEW_ORDER = "NEW_ORDER"
        const val EVENT_ORDER_TAKEN = "ORDER_TAKEN"
    }
}
