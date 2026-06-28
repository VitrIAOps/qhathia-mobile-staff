package com.qhathia.mobile.staff.core.domain.repository

import com.qhathia.mobile.staff.core.domain.model.Session
import kotlinx.coroutines.flow.Flow

/**
 * Port for the lifecycle and persistence of the authenticated [Session]. Implemented in the
 * data layer over encrypted storage.
 */
interface SessionRepository {

    /** Emits the current session, or `null` when no session is stored. */
    fun observeSession(): Flow<Session?>

    /** Persist (or replace) the session. */
    suspend fun saveSession(session: Session)

    /** Remove any stored session. */
    suspend fun clearSession()

    /** The current access token, or `null` if there is no session. */
    suspend fun currentAccessToken(): String?
}
