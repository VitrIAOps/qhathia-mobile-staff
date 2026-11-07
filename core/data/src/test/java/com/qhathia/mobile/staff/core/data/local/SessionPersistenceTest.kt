package com.qhathia.mobile.staff.core.data.local

import org.junit.jupiter.api.Assertions.assertNotNull
import com.qhathia.mobile.staff.core.domain.model.LoginException
import com.qhathia.mobile.staff.core.domain.model.LoginError
import com.qhathia.mobile.staff.core.data.repository.SessionAuthTokenProvider
import com.qhathia.mobile.staff.core.domain.model.AuthTokens
import com.qhathia.mobile.staff.core.domain.repository.AuthRepository
import com.qhathia.mobile.staff.core.testing.FakeSessionRepository
import com.qhathia.mobile.staff.core.testing.TestFixtures
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SessionPersistenceTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `a session stored before session ids existed still restores`() {
        val legacyBlob = """
            {"accessToken":"a","refreshToken":"r","expiresInSeconds":60,"userId":"u1",
             "userEmail":"owner@tienda.com","userFullName":"Juan","userRole":"OWNER","store":null}
        """.trimIndent()

        val session = json.decodeFromString<PersistedSession>(legacyBlob).toDomain()

        assertEquals("u1", session.user.id)
        assertNull(session.sessionId)
    }

    @Test
    fun `session id survives a persistence round trip`() {
        val session = TestFixtures.session(sessionId = "3f0c")

        val restored = json.decodeFromString<PersistedSession>(json.encodeToString(session.toPersisted())).toDomain()

        assertEquals("3f0c", restored.sessionId)
    }

    @Test
    fun `refreshing tokens keeps the session id`() = runTest {
        val sessions = FakeSessionRepository(TestFixtures.session(sessionId = "3f0c"))
        val auth = mockk<AuthRepository>()
        coEvery { auth.refresh(any()) } returns Result.success(AuthTokens("new-access", "new-refresh", 60))

        SessionAuthTokenProvider(sessions, auth).refreshTokens()

        val saved = sessions.observeSession().first()
        assertEquals("new-access", saved?.tokens?.accessToken)
        assertEquals("3f0c", saved?.sessionId)
    }

    @Test
    fun `a refresh lost to the network keeps the session`() = runTest {
        val sessions = FakeSessionRepository(TestFixtures.session())
        val auth = mockk<AuthRepository>()
        coEvery { auth.refresh(any()) } returns Result.failure(LoginException(LoginError.Network))

        assertNull(SessionAuthTokenProvider(sessions, auth).refreshTokens())

        assertNotNull(sessions.observeSession().first())
    }

    @Test
    fun `a refresh the server rejects clears the session`() = runTest {
        val sessions = FakeSessionRepository(TestFixtures.session())
        val auth = mockk<AuthRepository>()
        coEvery { auth.refresh(any()) } returns Result.failure(LoginException(LoginError.InvalidCredentials))

        assertNull(SessionAuthTokenProvider(sessions, auth).refreshTokens())

        assertNull(sessions.observeSession().first())
    }

    @Test
    fun `device label joins maker and model without repeating the maker`() {
        assertEquals("Xiaomi Redmi Note 12", deviceLabel("Xiaomi", "Redmi Note 12"))
        assertEquals("Samsung Galaxy S23", deviceLabel("samsung", "Samsung Galaxy S23"))
        assertEquals("Google Pixel 8", deviceLabel("google", "Pixel 8"))
        assertNull(deviceLabel(" ", ""))
        assertEquals(100, deviceLabel("Maker", "M".repeat(200))?.length)
    }
}
