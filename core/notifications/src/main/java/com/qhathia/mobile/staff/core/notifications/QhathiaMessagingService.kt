package com.qhathia.mobile.staff.core.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.qhathia.mobile.staff.core.domain.usecase.RegisterPushTokenUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

/**
 * Receives FCM messages for this device. The backend sends **data-only, PII-free** payloads
 * (`{event, orderId, storeId, status}`), so the app always builds the local notification itself.
 *
 * Both callbacks run on an FCM background thread; token registration is done with [runBlocking] so
 * it completes before the callback returns (the underlying use case is fail-open).
 */
@AndroidEntryPoint
class QhathiaMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var registerPushToken: RegisterPushTokenUseCase

    @Inject
    lateinit var presenter: OrderNotificationPresenter

    override fun onNewToken(token: String) {
        // Re-register the rotated token. No-ops server-side (401 → fail-open) when no session exists.
        runBlocking { registerPushToken() }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        presenter.showOrderNotification(
            event = data[KEY_EVENT],
            status = data[KEY_STATUS],
            orderId = data[KEY_ORDER_ID],
        )
    }

    private companion object {
        const val KEY_EVENT = "event"
        const val KEY_STATUS = "status"
        const val KEY_ORDER_ID = "orderId"
    }
}
