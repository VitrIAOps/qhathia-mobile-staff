package com.qhathia.mobile.staff.core.domain.usecase

import org.junit.jupiter.api.Assertions.assertTrue
import com.qhathia.mobile.staff.core.testing.TestFixtures
import com.qhathia.mobile.staff.core.testing.FakeSessionRepository
import com.qhathia.mobile.staff.core.testing.FakeNotificationTokenRepository
import com.qhathia.mobile.staff.core.testing.FakePushTokenProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RegisterPushTokenUseCaseTest {

    private val signedIn = FakeSessionRepository(TestFixtures.session())

    @Test
    fun `given a token when invoked then it is registered with the backend`() = runTest {
        val provider = FakePushTokenProvider(token = "fcm-abc")
        val repository = FakeNotificationTokenRepository()
        val useCase = RegisterPushTokenUseCase(signedIn, provider, repository, PushRegistrationGate())

        useCase(deviceLabel = "Pixel 7")

        assertEquals(1, repository.registerCallCount)
        assertEquals("fcm-abc", repository.lastRegisteredToken)
        assertEquals("Pixel 7", repository.lastRegisteredLabel)
    }

    @Test
    fun `given no token when invoked then nothing is registered`() = runTest {
        val provider = FakePushTokenProvider(token = null)
        val repository = FakeNotificationTokenRepository()
        val useCase = RegisterPushTokenUseCase(signedIn, provider, repository, PushRegistrationGate())

        useCase()

        assertEquals(0, repository.registerCallCount)
    }

    @Test
    fun `given registration fails when invoked then it does not throw`() = runTest {
        val provider = FakePushTokenProvider(token = "fcm-abc")
        val repository = FakeNotificationTokenRepository().apply {
            registerResult = Result.failure(IllegalStateException("HTTP 401"))
        }
        val useCase = RegisterPushTokenUseCase(signedIn, provider, repository, PushRegistrationGate())

        val result = useCase() // must not throw

        assertEquals(1, repository.registerCallCount)
        assertTrue(result.isFailure)
    }

    @Test
    fun `without a session nothing is sent`() = runTest {
        val repository = FakeNotificationTokenRepository()
        val useCase = RegisterPushTokenUseCase(FakeSessionRepository(initial = null), FakePushTokenProvider(token = "fcm-abc"), repository, PushRegistrationGate())

        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(0, repository.registerCallCount)
    }

    @Test
    fun `a successful registration reports success`() = runTest {
        val useCase = RegisterPushTokenUseCase(signedIn, FakePushTokenProvider(token = "fcm-abc"), FakeNotificationTokenRepository(), PushRegistrationGate())

        assertTrue(useCase().isSuccess)
    }

    @Test
    fun concurrentCalls_registerOnce() = runTest {
        // F-47: the sign-in and FCM's new-token callback ask at the same moment.
        val repository = FakeNotificationTokenRepository().apply { registerDelayMs = 500 }
        val gate = PushRegistrationGate()
        val fromSignIn = RegisterPushTokenUseCase(signedIn, FakePushTokenProvider(token = "fcm-abc"), repository, gate)
        val fromNewToken = RegisterPushTokenUseCase(signedIn, FakePushTokenProvider(token = "fcm-abc"), repository, gate)

        val results = listOf(async { fromSignIn() }, async { fromNewToken() }).awaitAll()

        assertEquals(1, repository.registerCallCount)
        assertTrue(results.all { it.isSuccess })
    }

    @Test
    fun sameTokenSameUser_skipsSecondCall() = runTest {
        val repository = FakeNotificationTokenRepository()
        val useCase = RegisterPushTokenUseCase(signedIn, FakePushTokenProvider(token = "fcm-abc"), repository, PushRegistrationGate())

        useCase()
        val second = useCase()

        assertTrue(second.isSuccess)
        assertEquals(1, repository.registerCallCount)
    }

    @Test
    fun failedRegistration_isRetriedNextTime() = runTest {
        val repository = FakeNotificationTokenRepository().apply {
            registerResult = Result.failure(IllegalStateException("offline"))
        }
        val useCase = RegisterPushTokenUseCase(signedIn, FakePushTokenProvider(token = "fcm-abc"), repository, PushRegistrationGate())

        useCase()
        repository.registerResult = Result.success(Unit)
        useCase()

        assertEquals(2, repository.registerCallCount)
    }

    @Test
    fun rotatedToken_isRegistered() = runTest {
        val repository = FakeNotificationTokenRepository()
        val provider = FakePushTokenProvider(token = "fcm-abc")
        val useCase = RegisterPushTokenUseCase(signedIn, provider, repository, PushRegistrationGate())

        useCase()
        provider.token = "fcm-new"
        useCase()

        assertEquals(2, repository.registerCallCount)
        assertEquals("fcm-new", repository.lastRegisteredToken)
    }

    @Test
    fun afterUnregister_sameToken_registersAgain() = runTest {
        val repository = FakeNotificationTokenRepository()
        val provider = FakePushTokenProvider(token = "fcm-abc")
        val gate = PushRegistrationGate()
        val register = RegisterPushTokenUseCase(signedIn, provider, repository, gate)

        register()
        UnregisterPushTokenUseCase(provider, repository, gate)()
        register()

        assertEquals(2, repository.registerCallCount)
    }
}
