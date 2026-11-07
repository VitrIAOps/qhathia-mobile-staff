package com.qhathia.mobile.staff.core.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.qhathia.mobile.staff.core.common.navigation.DESTINATION_ORDER_REQUESTS
import com.qhathia.mobile.staff.core.common.navigation.EXTRA_DESTINATION
import com.qhathia.mobile.staff.core.domain.repository.OrderNotificationTray
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Turns order pushes into tray notifications, and is the app's [OrderNotificationTray].
 *
 * Each order has at most one notification: it is posted with the order id as its tag and a fixed
 * id, so a later event for the same order replaces it. `ORDER_TAKEN` cancels it instead of posting,
 * because an order someone already handled needs nothing. Content is derived solely from the
 * `event` (see [orderPushAction]); the order id only keys the notification and is never shown.
 *
 * Tapping opens the app on the Solicitudes section via its launch intent plus a destination extra
 * (no direct dependency on `:app`).
 *
 * Order notifications share one group with an app-owned summary ("N órdenes por atender") that
 * opens Solicitudes too; without it Android builds its own summary, which opens nothing. The
 * summary is recomputed from the tray after every post and cancel, and goes with the last order.
 */
@Singleton
class OrderNotificationPresenter @Inject constructor(
    @ApplicationContext private val context: Context,
) : OrderNotificationTray {

    fun handleOrderPush(event: String?, orderId: String?) {
        when (val action = orderPushAction(event, orderId)) {
            is OrderPushAction.Post -> post(action.kind, orderId)
            is OrderPushAction.Cancel -> cancel(action.orderId)
            OrderPushAction.Ignore -> Unit
        }
    }

    private fun post(kind: OrderPushAction.Kind, orderId: String?) {
        ensureChannel()

        if (!notificationsAllowed()) return

        val (title, body) = contentFor(kind)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_order)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setGroup(GROUP_KEY)
            .setContentIntent(openAppIntent(orderId))
            .build()

        // Permission is checked above, but it can be revoked between the check and the post.
        try {
            val manager = NotificationManagerCompat.from(context)
            if (orderId != null) {
                manager.notify(orderId, ORDER_NOTIFICATION_ID, notification)
                refreshSummary(including = orderId)
            } else {
                val id = rollingId()
                manager.notify(id, notification)
                refreshSummary(including = OrderSummary.keyOf(null, id))
            }
        } catch (_: SecurityException) {
            // Notification permission revoked mid-flight — drop the notification silently.
        }
    }

    private fun contentFor(kind: OrderPushAction.Kind): Pair<String, String> = when (kind) {
        OrderPushAction.Kind.NEW_ORDER -> context.getString(R.string.notif_new_order_title) to
            context.getString(R.string.notif_new_order_body)
        OrderPushAction.Kind.EXPIRING -> context.getString(R.string.notif_order_expiring_title) to
            context.getString(R.string.notif_order_expiring_body)
        OrderPushAction.Kind.GENERIC -> context.getString(R.string.notif_order_generic_title) to
            context.getString(R.string.notif_order_updated_body)
    }

    /** Order ids with a notification in the tray; the group summary is not an order. */
    override fun activeOrderIds(): Set<String> =
        orderNotifications().filterNot { it.isGroupSummary() }.mapNotNullTo(HashSet()) { it.tag }

    override fun cancel(orderId: String) {
        NotificationManagerCompat.from(context).cancel(orderId, ORDER_NOTIFICATION_ID)
        refreshSummary(excluding = orderId)
    }

    /**
     * Untagged order notifications are the ones posted without an order id, and those left by app
     * versions that keyed notifications by `orderId.hashCode()`. Neither can be matched to an order.
     */
    override fun cancelUnmatched() {
        val manager = NotificationManagerCompat.from(context)
        orderNotifications()
            .filter { it.tag == null && !it.isGroupSummary() }
            .forEach { manager.cancel(it.id) }
        refreshSummary()
    }

    /**
     * Keeps one summary over the order notifications so a tap on the group opens Solicitudes. It is
     * cancelled only when no order is left: cancelling a summary also cancels its children.
     */
    private fun refreshSummary(including: String? = null, excluding: String? = null) {
        val entries = orderNotifications().map { TrayEntry(it.tag, it.id, it.isGroupSummary()) }
        val count = OrderSummary.childKeys(entries, including, excluding).size
        val manager = NotificationManagerCompat.from(context)
        if (count == 0) {
            manager.cancel(SUMMARY_TAG, SUMMARY_ID)
            return
        }
        if (!notificationsAllowed()) return

        val title = context.resources.getQuantityString(R.plurals.notif_orders_summary_title, count, count)
        val summary = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_order)
            .setContentTitle(title)
            .setStyle(NotificationCompat.InboxStyle().setSummaryText(title))
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
            // Only the order itself sounds; updating the count is silent.
            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(SUMMARY_TAG))
            .build()
        try {
            manager.notify(SUMMARY_TAG, SUMMARY_ID, summary)
        } catch (_: SecurityException) {
            // Permission revoked mid-flight; the children still open Solicitudes.
        }
    }

    private fun StatusBarNotification.isGroupSummary(): Boolean =
        (notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0

    /** This app's notifications on the orders channel; empty if the tray cannot be read. */
    private fun orderNotifications(): List<StatusBarNotification> = try {
        context.getSystemService(NotificationManager::class.java)
            ?.activeNotifications
            ?.filter { it.notification.channelId == CHANNEL_ID }
            .orEmpty()
    } catch (_: RuntimeException) {
        // Some OEM builds throw while the notification service is restarting.
        emptyList()
    }

    /**
     * The tap target: the app's own launch intent carrying an [EXTRA_DESTINATION] the shell reads to
     * land on Solicitudes. Every order event belongs to that section, so the destination is fixed.
     *
     * `SINGLE_TOP` matters — without it `CLEAR_TOP` destroys and recreates a running `MainActivity`
     * instead of delivering the intent to `onNewIntent`. The request code is keyed by order (or by
     * [SUMMARY_TAG] for the summary) so that `FLAG_UPDATE_CURRENT` cannot rewrite the extras of
     * notifications already in the tray.
     */
    private fun openAppIntent(key: String?): PendingIntent {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_DESTINATION, DESTINATION_ORDER_REQUESTS)
            }
            ?: Intent()
        return PendingIntent.getActivity(
            context,
            key?.hashCode() ?: rollingId(),
            launch,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun ensureChannel() {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notif_channel_orders_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notif_channel_orders_description)
        }
        manager.createNotificationChannel(channel)
    }

    private fun notificationsAllowed(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /** For a push with no order id, which cannot be keyed by order. */
    private fun rollingId(): Int = (System.currentTimeMillis() and Int.MAX_VALUE.toLong()).toInt()

    companion object {
        private const val CHANNEL_ID = "orders"

        /** Fixed id shared by every order notification; the tag (the order id) tells them apart. */
        private const val ORDER_NOTIFICATION_ID = 1

        /** The group every order notification joins, under the summary below. */
        private const val GROUP_KEY = "com.qhathia.mobile.staff.ORDERS"
        private const val SUMMARY_TAG = "orders_summary"
        private const val SUMMARY_ID = 2
    }
}
