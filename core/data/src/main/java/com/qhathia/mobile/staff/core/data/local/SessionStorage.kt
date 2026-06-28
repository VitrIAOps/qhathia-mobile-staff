package com.qhathia.mobile.staff.core.data.local

import com.qhathia.mobile.staff.core.domain.model.Session
import kotlinx.coroutines.flow.Flow

/**
 * Low-level persistence boundary for the [Session]. Abstracted so the repository can be unit
 * tested against an in-memory fake while production uses encrypted DataStore.
 */
interface SessionStorage {
    fun sessionFlow(): Flow<Session?>
    suspend fun save(session: Session)
    suspend fun clear()
    suspend fun current(): Session?
}
