package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.repository.NotificationTokenRepository
import com.qhathia.mobile.staff.core.domain.repository.PushTokenProvider
import javax.inject.Inject

/**
 * Stops push delivery to this device: unregisters the token with the backend, then deletes the local
 * FCM token. Invoked on logout (while the session is still valid). Fail-open: a failed backend call
 * still proceeds to delete the local token, and no error is surfaced. It also forgets the last
 * registration in [PushRegistrationGate], so the next login registers again.
 */
class UnregisterPushTokenUseCase @Inject constructor(
    private val pushTokenProvider: PushTokenProvider,
    private val notificationTokenRepository: NotificationTokenRepository,
    private val gate: PushRegistrationGate,
) {
    suspend operator fun invoke() {
        gate.withLock {
            forget()
            runCatching {
                pushTokenProvider.currentToken()?.let { token ->
                    notificationTokenRepository.unregisterToken(token)
                }
                pushTokenProvider.deleteToken()
            }
        }
    }
}
