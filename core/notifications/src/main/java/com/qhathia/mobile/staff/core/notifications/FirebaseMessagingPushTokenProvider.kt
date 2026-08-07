package com.qhathia.mobile.staff.core.notifications

import com.google.firebase.messaging.FirebaseMessaging
import com.qhathia.mobile.staff.core.domain.repository.PushTokenProvider
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * [PushTokenProvider] over Firebase Cloud Messaging. Bridges the Play Services `Task<T>` API to
 * coroutines via `.await()`. Both operations are best-effort — a failure yields `null` / no-op so
 * the calling use cases stay fail-open.
 */
class FirebaseMessagingPushTokenProvider @Inject constructor() : PushTokenProvider {

    override suspend fun currentToken(): String? =
        runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull()

    override suspend fun deleteToken() {
        runCatching { FirebaseMessaging.getInstance().deleteToken().await() }
    }
}
