package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.testing.FakeAuthRepository
import com.qhathia.mobile.staff.core.testing.FakeNotificationTokenRepository
import com.qhathia.mobile.staff.core.testing.FakePushTokenProvider
import com.qhathia.mobile.staff.core.testing.FakeSessionRepository
import com.qhathia.mobile.staff.core.testing.TestFixtures
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LogoutUseCaseTest {

    private lateinit var sessionRepository: FakeSessionRepository
    private lateinit var authRepository: FakeAuthRepository
    private lateinit var notificationTokenRepository: FakeNotificationTokenRepository
    private lateinit var pushTokenProvider: FakePushTokenProvider
    private lateinit var useCase: LogoutUseCase

    @BeforeEach
    fun setUp() {
        sessionRepository = FakeSessionRepository(
            initial = TestFixtures.session(tokens = TestFixtures.tokens(refreshToken = "ref-1")),
        )
        authRepository = FakeAuthRepository()
        notificationTokenRepository = FakeNotificationTokenRepository()
        pushTokenProvider = FakePushTokenProvider(token = "fcm-abc")
        useCase = LogoutUseCase(
            sessionRepository = sessionRepository,
            unregisterPushToken = UnregisterPushTokenUseCase(
                pushTokenProvider,
                notificationTokenRepository,
            ),
            authRepository = authRepository,
        )
    }

    @Test
    fun `when invoked then it unregisters the push token, logs out server-side, and clears session`() =
        runTest {
            useCase()

            assertEquals(1, notificationTokenRepository.unregisterCallCount)
            assertEquals(1, pushTokenProvider.deleteTokenCallCount)
            assertEquals(1, authRepository.logoutCallCount)
            assertEquals(1, sessionRepository.clearCallCount)
        }

    @Test
    fun `when invoked then server logout runs before the session is cleared`() = runTest {
        // The session still carries the refresh token when logout is called — proving the network
        // steps ran before clearSession() nulled the session out.
        useCase()

        assertEquals("ref-1", authRepository.lastLogoutRefreshToken)
        assertNull(sessionRepository.currentAccessToken())
    }

    @Test
    fun `given no session when invoked then it clears without calling server logout`() = runTest {
        val emptySession = FakeSessionRepository(initial = null)
        val subject = LogoutUseCase(
            sessionRepository = emptySession,
            unregisterPushToken = UnregisterPushTokenUseCase(
                pushTokenProvider,
                notificationTokenRepository,
            ),
            authRepository = authRepository,
        )

        subject()

        assertEquals(0, authRepository.logoutCallCount)
        assertEquals(1, emptySession.clearCallCount)
    }

    @Test
    fun `given network failures when invoked then the session is still cleared`() = runTest {
        notificationTokenRepository.unregisterResult = Result.failure(IllegalStateException("offline"))
        authRepository.logoutResult = Result.failure(IllegalStateException("offline"))

        useCase() // must not throw

        assertEquals(1, sessionRepository.clearCallCount)
    }
}
