package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.model.ActiveSession
import com.qhathia.mobile.staff.core.domain.repository.AccountSecurityRepository
import com.qhathia.mobile.staff.core.domain.repository.SessionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Lists the user's signed-in devices, this device first and then by most recent use. A session is
 * this device when the backend flags it or when its id matches the one stored at login — the second
 * path covers responses where the backend could not resolve `current`.
 */
class GetActiveSessionsUseCase @Inject constructor(
    private val repository: AccountSecurityRepository,
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke(): Result<List<ActiveSession>> {
        val storedSessionId = sessionRepository.observeSession().first()?.sessionId
        return repository.getSessions().map { sessions ->
            sessions
                .map { it.copy(isCurrent = it.isCurrent || it.sessionId == storedSessionId) }
                .sortedWith(
                    compareByDescending<ActiveSession> { it.isCurrent }
                        .thenByDescending { it.lastUsedAt },
                )
        }
    }
}
