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
 * backend (a rotation after the app's data was cleared used to answer 401). Calls run one at a time
 * through [PushRegistrationGate], and a token already registered for the signed-in user returns
 * success without calling the backend again (F-47). Otherwise it reports the outcome, so the caller
 * can retry; it never throws.
 */
class RegisterPushTokenUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val pushTokenProvider: PushTokenProvider,
    private val notificationTokenRepository: NotificationTokenRepository,
    private val gate: PushRegistrationGate,
) {
    suspend operator fun invoke(deviceLabel: String? = null): Result<Unit> =
        try {
            gate.withLock {
                val session = sessionRepository.observeSession().first()
                if (session == null) return@withLock Result.success(Unit)
                val token = pushTokenProvider.currentToken()
                    ?: return@withLock Result.failure(IllegalStateException("No push token yet"))
                val userId = session.user.id
                if (isRegistered(token, userId)) return@withLock Result.success(Unit)
                notificationTokenRepository.registerToken(token, deviceLabel)
                    .onSuccess { markRegistered(token, userId) }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
}
