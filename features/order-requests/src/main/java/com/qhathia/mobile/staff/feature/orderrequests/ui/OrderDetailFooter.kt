package com.qhathia.mobile.staff.feature.orderrequests.ui

import androidx.annotation.StringRes
import com.qhathia.mobile.staff.core.domain.model.OrderStatus
import com.qhathia.mobile.staff.feature.orderrequests.R
import java.time.Instant

/** What the bottom of the order detail dialog shows. */
internal sealed interface OrderDetailFooter {
    /** `Rechazar` and `Confirmar pedido`: the order can still be acted on. */
    data object Actions : OrderDetailFooter

    /** One line stating the outcome, in place of actions that could only fail. */
    data class Resolved(@StringRes val messageRes: Int) : OrderDetailFooter
}

/**
 * Only a `PENDING` order before its deadline can be confirmed or rejected; anything else comes back
 * as `422`, so it gets its outcome instead of buttons. An in-flight request is deliberately not an
 * input: the buttons stay (disabled) rather than vanish under the merchant's finger.
 */
internal fun orderDetailFooter(
    status: OrderStatus,
    expiresAt: Instant?,
    now: Instant,
): OrderDetailFooter = when (status) {
    OrderStatus.PENDING ->
        if (isExpired(expiresAt, now)) {
            OrderDetailFooter.Resolved(R.string.order_detail_resolved_expired)
        } else {
            OrderDetailFooter.Actions
        }
    OrderStatus.CONFIRMED -> OrderDetailFooter.Resolved(R.string.order_detail_resolved_confirmed)
    OrderStatus.REJECTED -> OrderDetailFooter.Resolved(R.string.order_detail_resolved_rejected)
    OrderStatus.CANCELLED -> OrderDetailFooter.Resolved(R.string.order_detail_resolved_cancelled)
    OrderStatus.EXPIRED -> OrderDetailFooter.Resolved(R.string.order_detail_resolved_expired)
}
