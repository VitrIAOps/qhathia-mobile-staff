package com.qhathia.mobile.staff.core.notifications

import com.qhathia.mobile.staff.core.common.format.toPenPrice
import com.qhathia.mobile.staff.core.domain.model.OrderDetail
import java.time.Duration
import java.time.Instant

/**
 * What the new-order notification says once the app fetched the order with its own session. Kept
 * free of Android types so it is JVM-testable; the presenter turns it into localized text.
 *
 * [customerName] is null when the kiosk sent none (the title then stays generic). [minutesLeft] is
 * null when the order has no deadline or it already passed. [lines] holds at most
 * [MAX_LINES] products ("2 × Taladro"); [moreLines] counts the ones left out.
 */
internal data class NewOrderContent(
    val customerName: String?,
    val total: String,
    val productCount: Int,
    val minutesLeft: Long?,
    val lines: List<String>,
    val moreLines: Int,
) {
    companion object {
        const val MAX_LINES = 3

        fun from(detail: OrderDetail, now: Instant): NewOrderContent {
            val order = detail.order
            val minutes = order.expiresAt
                ?.let { Duration.between(now, it) }
                ?.takeIf { !it.isNegative && !it.isZero }
                // A started minute counts: 30 s left still reads "1 min".
                ?.let { (it.seconds + SECONDS_PER_MINUTE - 1) / SECONDS_PER_MINUTE }
            return NewOrderContent(
                customerName = order.customerName?.trim()?.takeIf { it.isNotEmpty() },
                total = order.total.toPenPrice(),
                productCount = detail.items.size.takeIf { it > 0 } ?: order.itemCount,
                minutesLeft = minutes,
                lines = detail.items.take(MAX_LINES).map { "${it.quantity} × ${it.productName}" },
                moreLines = (detail.items.size - MAX_LINES).coerceAtLeast(0),
            )
        }

        private const val SECONDS_PER_MINUTE = 60L
    }
}
