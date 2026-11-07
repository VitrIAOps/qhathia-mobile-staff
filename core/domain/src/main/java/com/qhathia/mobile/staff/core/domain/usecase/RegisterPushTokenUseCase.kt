package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.repository.NotificationTokenRepository
import com.qhathia.mobile.staff.core.domain.repository.PushTokenProvider
import com.qhathia.mobile.staff.core.domain.repository.SessionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Registers this device's current FCM token with the backend so it receives order pushes. Invoked
 * after login, on authenticated app start, and on token rotation.
 *
 * Without a session there is nothing to register for, so it returns success without calling the
 * backend (a rotation after the app's data was cleared used to answer 401). Otherwise it reports
 * the outcome, so the caller can retry; it never throws.
 */
class RegisterPushTokenUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val pushTokenProvider: PushTokenProvider,
    private val notificationTokenRepository: NotificationTokenRepository,
) {
    suspend operator fun invoke(deviceLabel: String? = null): Result<Unit> =
        try {
            if (sessionRepository.observeSession().first() == null) {
                Result.success(Unit)
            } else {
                val token = pushTokenProvider.currentToken()
                if (token == null) {
                    Result.failure(IllegalStateException("No push token yet"))
                } else {
                    notificationTokenRepository.registerToken(token, deviceLabel)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
}
