package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.repository.AuthRepository
import com.qhathia.mobile.staff.core.domain.repository.SessionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Signs the merchant out. While the session is still valid it (1) unregisters this device's push
 * token and (2) invalidates the session server-side; it then (3) clears the stored session, which
 * flips the observed auth state to unauthenticated and returns the app to login.
 *
 * The two network steps are best-effort — a failure (e.g. offline) never prevents the local session
 * from being cleared. The push unregister runs first so the bearer plugin can still refresh, if
 * needed, against the not-yet-revoked refresh token before [AuthRepository.logout] revokes it.
 */
class LogoutUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val unregisterPushToken: UnregisterPushTokenUseCase,
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke() {
        unregisterPushToken()

        val refreshToken = sessionRepository.observeSession().first()?.tokens?.refreshToken
        if (refreshToken != null) {
            runCatching { authRepository.logout(refreshToken) }
        }

        sessionRepository.clearSession()
    }
}
