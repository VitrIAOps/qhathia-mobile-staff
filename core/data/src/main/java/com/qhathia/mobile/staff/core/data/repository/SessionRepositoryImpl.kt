package com.qhathia.mobile.staff.core.data.repository

import com.qhathia.mobile.staff.core.data.local.SessionStorage
import com.qhathia.mobile.staff.core.domain.model.Session
import com.qhathia.mobile.staff.core.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** [SessionRepository] backed by encrypted [SessionStorage]. */
class SessionRepositoryImpl @Inject constructor(
    private val storage: SessionStorage,
) : SessionRepository {

    override fun observeSession(): Flow<Session?> = storage.sessionFlow()

    override suspend fun saveSession(session: Session) = storage.save(session)

    override suspend fun clearSession() = storage.clear()

    override suspend fun currentAccessToken(): String? = storage.current()?.tokens?.accessToken
}
