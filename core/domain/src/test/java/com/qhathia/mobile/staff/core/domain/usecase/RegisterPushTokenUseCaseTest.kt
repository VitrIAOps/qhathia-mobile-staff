package com.qhathia.mobile.staff.core.domain.usecase

import org.junit.jupiter.api.Assertions.assertTrue
import com.qhathia.mobile.staff.core.testing.TestFixtures
import com.qhathia.mobile.staff.core.testing.FakeSessionRepository
import com.qhathia.mobile.staff.core.testing.FakeNotificationTokenRepository
import com.qhathia.mobile.staff.core.testing.FakePushTokenProvider
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RegisterPushTokenUseCaseTest {

    private val signedIn = FakeSessionRepository(TestFixtures.session())

    @Test
    fun `given a token when invoked then it is registered with the backend`() = runTest {
        val provider = FakePushTokenProvider(token = "fcm-abc")
        val repository = FakeNotificationTokenRepository()
        val useCase = RegisterPushTokenUseCase(signedIn, provider, repository)

        useCase(deviceLabel = "Pixel 7")

        assertEquals(1, repository.registerCallCount)
        assertEquals("fcm-abc", repository.lastRegisteredToken)
        assertEquals("Pixel 7", repository.lastRegisteredLabel)
    }

    @Test
    fun `given no token when invoked then nothing is registered`() = runTest {
        val provider = FakePushTokenProvider(token = null)
        val repository = FakeNotificationTokenRepository()
        val useCase = RegisterPushTokenUseCase(signedIn, provider, repository)

        useCase()

        assertEquals(0, repository.registerCallCount)
    }

    @Test
    fun `given registration fails when invoked then it does not throw`() = runTest {
        val provider = FakePushTokenProvider(token = "fcm-abc")
        val repository = FakeNotificationTokenRepository().apply {
            registerResult = Result.failure(IllegalStateException("HTTP 401"))
        }
        val useCase = RegisterPushTokenUseCase(signedIn, provider, repository)

        val result = useCase() // must not throw

        assertEquals(1, repository.registerCallCount)
        assertTrue(result.isFailure)
    }

    @Test
    fun `without a session nothing is sent`() = runTest {
        val repository = FakeNotificationTokenRepository()
        val useCase = RegisterPushTokenUseCase(FakeSessionRepository(initial = null), FakePushTokenProvider(token = "fcm-abc"), repository)

        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(0, repository.registerCallCount)
    }

    @Test
    fun `a successful registration reports success`() = runTest {
        val useCase = RegisterPushTokenUseCase(signedIn, FakePushTokenProvider(token = "fcm-abc"), FakeNotificationTokenRepository())

        assertTrue(useCase().isSuccess)
    }
}
