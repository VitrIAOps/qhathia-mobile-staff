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
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds and posts the local, PII-free order notification from a push payload. Content is derived
 * solely from `event`/`status`; `orderId` is used only to key distinct notifications (never shown).
 * Tapping opens the app via its launch intent (no direct dependency on `:app`).
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
            .setContentIntent(openAppIntent())
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

    private fun openAppIntent(): PendingIntent {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP }
            ?: Intent()
        return PendingIntent.getActivity(
            context,
            0,
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
