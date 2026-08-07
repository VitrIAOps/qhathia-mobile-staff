package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.testing.FakeNotificationTokenRepository
import com.qhathia.mobile.staff.core.testing.FakePushTokenProvider
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UnregisterPushTokenUseCaseTest {

    @Test
    fun `given a token when invoked then it unregisters and deletes the local token`() = runTest {
        val provider = FakePushTokenProvider(token = "fcm-abc")
        val repository = FakeNotificationTokenRepository()
        val useCase = UnregisterPushTokenUseCase(provider, repository)

        useCase()

        assertEquals(1, repository.unregisterCallCount)
        assertEquals("fcm-abc", repository.lastUnregisteredToken)
        assertEquals(1, provider.deleteTokenCallCount)
    }

    @Test
    fun `given no token when invoked then it still deletes the local token`() = runTest {
        val provider = FakePushTokenProvider(token = null)
        val repository = FakeNotificationTokenRepository()
        val useCase = UnregisterPushTokenUseCase(provider, repository)

        useCase()

        assertEquals(0, repository.unregisterCallCount)
        assertEquals(1, provider.deleteTokenCallCount)
    }

    @Test
    fun `given backend unregister fails when invoked then it still deletes the local token`() =
        runTest {
            val provider = FakePushTokenProvider(token = "fcm-abc")
            val repository = FakeNotificationTokenRepository().apply {
                unregisterResult = Result.failure(IllegalStateException("offline"))
            }
            val useCase = UnregisterPushTokenUseCase(provider, repository)

            useCase() // must not throw

            assertEquals(1, provider.deleteTokenCallCount)
        }
}
