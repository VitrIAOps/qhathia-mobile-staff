package com.qhathia.mobile.staff.core.data.repository

import com.qhathia.mobile.staff.core.data.local.SessionStorage
import com.qhathia.mobile.staff.core.domain.model.Session
import com.qhathia.mobile.staff.core.testing.TestFixtures
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SessionRepositoryImplTest {

    private class InMemorySessionStorage : SessionStorage {
        private val state = MutableStateFlow<Session?>(null)
        override fun sessionFlow(): Flow<Session?> = state.asStateFlow()
        override suspend fun save(session: Session) { state.value = session }
        override suspend fun clear() { state.value = null }
        override suspend fun current(): Session? = state.value
    }

    private val storage = InMemorySessionStorage()
    private val repository = SessionRepositoryImpl(storage)

    @Test
    fun `given saved session when observed then it is emitted`() = runTest {
        val session = TestFixtures.session()

        repository.saveSession(session)

        assertEquals(session, repository.observeSession().first())
    }

    @Test
    fun `given saved session when currentAccessToken then returns the access token`() = runTest {
        repository.saveSession(TestFixtures.session())

        assertEquals("access-token", repository.currentAccessToken())
    }

    @Test
    fun `given saved session when cleared then nothing remains`() = runTest {
        repository.saveSession(TestFixtures.session())

        repository.clearSession()

        assertNull(repository.observeSession().first())
        assertNull(repository.currentAccessToken())
    }
}
