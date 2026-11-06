package com.qhathia.mobile.staff.core.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.qhathia.mobile.staff.core.domain.usecase.HandleOrderPushUseCase
import com.qhathia.mobile.staff.core.domain.usecase.RegisterPushTokenUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * Receives FCM messages for this device. The backend sends **data-only, PII-free** payloads
 * (`{event, orderId, storeId, status}`), so the app builds (or, for `ORDER_TAKEN`,
 * cancels) the local notification itself. The payload's `status` is not needed: the event alone
 * decides what the tray shows.
 *
 * Besides the tray, a `NEW_ORDER` / `ORDER_TAKEN` push syncs the app ([HandleOrderPushUseCase]):
 * open order lists re-query and the pending count is re-read, so the badges move even when
 * Solicitudes is not on screen. The notification is posted first, so a slow network never delays it.
 *
 * Both callbacks run on an FCM background thread; their work is done with [runBlocking] so it
 * completes before the callback returns. Both use cases are fail-open, and the push sync is capped
 * at [PUSH_SYNC_TIMEOUT_MS].
 */
@AndroidEntryPoint
class QhathiaMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var registerPushToken: RegisterPushTokenUseCase

    @Inject
    lateinit var presenter: OrderNotificationPresenter

    @Inject
    lateinit var handleOrderPush: HandleOrderPushUseCase

    override fun onNewToken(token: String) {
        // Re-register the rotated token. No-ops server-side (401 → fail-open) when no session exists.
        runBlocking { registerPushToken() }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        presenter.handleOrderPush(
            event = data[KEY_EVENT],
            orderId = data[KEY_ORDER_ID],
        )
        runBlocking { withTimeoutOrNull(PUSH_SYNC_TIMEOUT_MS) { handleOrderPush(data[KEY_EVENT]) } }
    }

    private companion object {
        const val KEY_EVENT = "event"
        const val KEY_ORDER_ID = "orderId"
        const val PUSH_SYNC_TIMEOUT_MS = 5_000L
    }
}
