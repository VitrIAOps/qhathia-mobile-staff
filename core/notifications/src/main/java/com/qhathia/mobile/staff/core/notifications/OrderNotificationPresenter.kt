package com.qhathia.mobile.staff.core.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.qhathia.mobile.staff.core.common.navigation.DESTINATION_ORDER_REQUESTS
import com.qhathia.mobile.staff.core.common.navigation.EXTRA_DESTINATION
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds and posts the local, PII-free order notification from a push payload. Content is derived
 * solely from `event`/`status`; `orderId` is used only to key distinct notifications (never shown).
 * Tapping opens the app on the Solicitudes section via its launch intent plus a destination extra
 * (no direct dependency on `:app`).
 */
@Singleton
class OrderNotificationPresenter @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    fun showOrderNotification(event: String?, status: String?, orderId: String?) {
        ensureChannel()

        if (!notificationsAllowed()) return

        val (title, body) = contentFor(event, status)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_order)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openAppIntent(orderId))
            .build()

        // Permission is checked above, but it can be revoked between the check and the post.
        try {
            NotificationManagerCompat.from(context).notify(notificationId(orderId), notification)
        } catch (_: SecurityException) {
            // Notification permission revoked mid-flight — drop the notification silently.
        }
    }

    private fun contentFor(event: String?, status: String?): Pair<String, String> = when (event) {
        EVENT_NEW_ORDER -> context.getString(R.string.notif_new_order_title) to
            context.getString(R.string.notif_new_order_body)
        EVENT_ORDER_TAKEN -> context.getString(R.string.notif_order_taken_title) to when (status) {
            STATUS_CONFIRMED -> context.getString(R.string.notif_order_confirmed_body)
            STATUS_REJECTED -> context.getString(R.string.notif_order_rejected_body)
            else -> context.getString(R.string.notif_order_updated_body)
        }
        else -> context.getString(R.string.notif_order_generic_title) to
            context.getString(R.string.notif_order_updated_body)
    }

    /**
     * The tap target: the app's own launch intent carrying an [EXTRA_DESTINATION] the shell reads to
     * land on Solicitudes. Both order events belong to that section, so the destination is fixed.
     *
     * `SINGLE_TOP` matters — without it `CLEAR_TOP` destroys and recreates a running `MainActivity`
     * instead of delivering the intent to `onNewIntent`. The request code is keyed by order so that
     * `FLAG_UPDATE_CURRENT` cannot rewrite the extras of notifications already in the tray.
     */
    private fun openAppIntent(orderId: String?): PendingIntent {
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
            notificationId(orderId),
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

    /** Distinct id per order so multiple pending orders stack; falls back to a rolling id. */
    private fun notificationId(orderId: String?): Int =
        orderId?.hashCode() ?: (System.currentTimeMillis() and Int.MAX_VALUE.toLong()).toInt()

    companion object {
        private const val CHANNEL_ID = "orders"
        private const val EVENT_NEW_ORDER = "NEW_ORDER"
        private const val EVENT_ORDER_TAKEN = "ORDER_TAKEN"
        private const val STATUS_CONFIRMED = "CONFIRMED"
        private const val STATUS_REJECTED = "REJECTED"
    }
}
