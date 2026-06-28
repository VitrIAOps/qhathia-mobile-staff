package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.model.Session
import com.qhathia.mobile.staff.core.domain.repository.AuthRepository
import com.qhathia.mobile.staff.core.domain.repository.SessionRepository
import javax.inject.Inject

/**
 * Authenticates the merchant and, on success, persists the resulting session so the rest of the
 * app becomes authenticated. Returns a failed [Result] carrying a
 * [com.qhathia.mobile.staff.core.domain.model.LoginException] on error.
 */
class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(email: String, password: String): Result<Session> =
        authRepository.login(email.trim(), password)
            .onSuccess { session -> sessionRepository.saveSession(session) }
}
