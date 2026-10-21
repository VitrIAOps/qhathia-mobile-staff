package com.qhathia.mobile.staff.core.notifications

/** What to do in the tray for one order push. Kept free of Android types so it is JVM-testable. */
internal sealed interface OrderPushAction {

    /** Post (or replace) the order's notification with [kind]'s text. */
    data class Post(val kind: Kind) : OrderPushAction

    /** Remove [orderId]'s notification: the order no longer needs attention. */
    data class Cancel(val orderId: String) : OrderPushAction

    /** Nothing to do. */
    data object Ignore : OrderPushAction

    enum class Kind { NEW_ORDER, EXPIRING, GENERIC }
}

internal const val EVENT_NEW_ORDER = "NEW_ORDER"
internal const val EVENT_ORDER_EXPIRING = "ORDER_EXPIRING"
internal const val EVENT_ORDER_TAKEN = "ORDER_TAKEN"

/**
 * Maps a push to its tray action.
 *
 * - `ORDER_TAKEN` means someone already confirmed or rejected the order, so its notification goes.
 *   Without an order id there is nothing to cancel.
 * - `ORDER_EXPIRING` keeps `status = PENDING` on purpose: the order did not change, so it replaces
 *   the order's notification rather than being read as a status change.
 * - Unknown events still post the generic text, so a future event is never silently dropped.
 */
internal fun orderPushAction(event: String?, orderId: String?): OrderPushAction = when (event) {
    EVENT_NEW_ORDER -> OrderPushAction.Post(OrderPushAction.Kind.NEW_ORDER)
    EVENT_ORDER_EXPIRING -> OrderPushAction.Post(OrderPushAction.Kind.EXPIRING)
    EVENT_ORDER_TAKEN -> orderId?.let(OrderPushAction::Cancel) ?: OrderPushAction.Ignore
    else -> OrderPushAction.Post(OrderPushAction.Kind.GENERIC)
}
