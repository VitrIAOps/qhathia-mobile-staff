package com.qhathia.mobile.staff.core.data.repository

import com.qhathia.mobile.staff.core.domain.repository.AuthRepository
import com.qhathia.mobile.staff.core.domain.repository.SessionRepository
import com.qhathia.mobile.staff.core.network.AuthTokenProvider
import io.ktor.client.plugins.auth.providers.BearerTokens
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Bridges the Ktor bearer Auth plugin to the session: loads the stored tokens and, on refresh,
 * rotates them via [AuthRepository.refresh], persisting the new pair or clearing the session if
 * refresh fails.
 */
class SessionAuthTokenProvider @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val authRepository: AuthRepository,
) : AuthTokenProvider {

    override suspend fun loadTokens(): BearerTokens? =
        sessionRepository.observeSession().first()?.let { session ->
            BearerTokens(session.tokens.accessToken, session.tokens.refreshToken)
        }

    override suspend fun refreshTokens(): BearerTokens? {
        val session = sessionRepository.observeSession().first() ?: return null
        return authRepository.refresh(session.tokens.refreshToken).fold(
            onSuccess = { tokens ->
                sessionRepository.saveSession(session.copy(tokens = tokens))
                BearerTokens(tokens.accessToken, tokens.refreshToken)
            },
            onFailure = {
                sessionRepository.clearSession()
                null
            },
        )
    }
}
