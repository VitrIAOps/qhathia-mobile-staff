package com.qhathia.mobile.staff.core.domain.repository

/**
 * Port over the order notifications currently shown in the device's notification tray, keeping the
 * Android notification APIs out of the domain. Implemented in the notifications layer.
 *
 * Each order has at most one notification, identified by the order id. Android derives the launcher
 * badge from these notifications, so keeping them in line with the server keeps the badge honest.
 */
interface OrderNotificationTray {

    /** Ids of the orders that currently have a notification in the tray. */
    fun activeOrderIds(): Set<String>

    /** Removes [orderId]'s notification; does nothing when it has none. */
    fun cancel(orderId: String)

    /** Removes order notifications that carry no order id, which can never be matched to an order. */
    fun cancelUnmatched()
}
