package com.qhathia.mobile.staff.core.testing

import com.qhathia.mobile.staff.core.domain.model.Session
import com.qhathia.mobile.staff.core.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory [SessionRepository] for tests, backed by a [MutableStateFlow]. */
class FakeSessionRepository(
    initial: Session? = null,
) : SessionRepository {

    private val sessionState = MutableStateFlow(initial)

    var saveCallCount: Int = 0
        private set
    var clearCallCount: Int = 0
        private set

    override fun observeSession(): Flow<Session?> = sessionState.asStateFlow()

    override suspend fun saveSession(session: Session) {
        saveCallCount++
        sessionState.value = session
    }

    override suspend fun clearSession() {
        clearCallCount++
        sessionState.value = null
    }

    override suspend fun currentAccessToken(): String? = sessionState.value?.tokens?.accessToken
}
