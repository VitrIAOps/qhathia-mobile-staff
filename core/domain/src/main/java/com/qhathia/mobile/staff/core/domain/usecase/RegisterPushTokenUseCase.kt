package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.repository.NotificationTokenRepository
import com.qhathia.mobile.staff.core.domain.repository.PushTokenProvider
import javax.inject.Inject

/**
 * Registers this device's current FCM token with the backend so it receives order pushes. Invoked
 * after login, on authenticated app start, and on token rotation. Fail-open: any error (no token,
 * network failure, missing session → 401) is swallowed so it never blocks the user.
 */
class RegisterPushTokenUseCase @Inject constructor(
    private val pushTokenProvider: PushTokenProvider,
    private val notificationTokenRepository: NotificationTokenRepository,
) {
    suspend operator fun invoke(deviceLabel: String? = null) {
        runCatching {
            val token = pushTokenProvider.currentToken() ?: return
            notificationTokenRepository.registerToken(token, deviceLabel)
        }
    }
}
