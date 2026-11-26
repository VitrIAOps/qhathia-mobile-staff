package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.model.Session
import com.qhathia.mobile.staff.core.domain.repository.AuthRepository
import com.qhathia.mobile.staff.core.domain.repository.AuthTokenCache
import com.qhathia.mobile.staff.core.domain.repository.SessionRepository
import javax.inject.Inject

/**
 * Authenticates the merchant and, on success, persists the resulting session so the rest of the
 * app becomes authenticated. Then it clears the network's cached token, so the first requests of the
 * new session carry its token and not one cached earlier (F-47). Returns a failed [Result] carrying a
 * [com.qhathia.mobile.staff.core.domain.model.LoginException] on error.
 */
class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository,
    private val authTokenCache: AuthTokenCache,
) {
    suspend operator fun invoke(email: String, password: String): Result<Session> =
        authRepository.login(email.trim(), password)
            .onSuccess { session ->
                sessionRepository.saveSession(session)
                authTokenCache.clear()
            }
}
