package com.qhathia.mobile.staff

import com.qhathia.mobile.staff.core.common.navigation.DESTINATION_ORDER_REQUESTS
import com.qhathia.mobile.staff.core.domain.usecase.ObserveBiometricEnabledUseCase
import com.qhathia.mobile.staff.core.domain.usecase.ObserveSessionUseCase
import com.qhathia.mobile.staff.core.domain.usecase.RegisterPushTokenUseCase
import com.qhathia.mobile.staff.core.testing.FakeAppPreferencesRepository
import com.qhathia.mobile.staff.core.testing.FakeNetworkMonitor
import com.qhathia.mobile.staff.core.testing.FakeNotificationTokenRepository
import com.qhathia.mobile.staff.core.testing.FakePushTokenProvider
import com.qhathia.mobile.staff.core.testing.FakeSessionRepository
import com.qhathia.mobile.staff.core.testing.MainDispatcherExtension
import com.qhathia.mobile.staff.core.testing.MutableClock
import com.qhathia.mobile.staff.core.testing.TestFixtures
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.Duration

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val clock = MutableClock()
    private val tokens = FakeNotificationTokenRepository()

    private val network = FakeNetworkMonitor(online = true)

    private fun TestScope.createViewModel(
        sessions: FakeSessionRepository,
        prefs: FakeAppPreferencesRepository,
    ): MainViewModel {
        val viewModel = MainViewModel(
            observeSession = ObserveSessionUseCase(sessions),
            observeBiometricEnabled = ObserveBiometricEnabledUseCase(prefs),
            registerPushToken = RegisterPushTokenUseCase(sessions, FakePushTokenProvider(), tokens),
            networkMonitor = network,
            clock = clock,
        )
        // authState is shared WhileSubscribed: keep a subscriber for the whole test.
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.authState.collect {} }
        return viewModel
    }

    @Test
    fun `a stored session with fingerprint sign-in starts locked until the prompt passes`() = runTest {
        val viewModel = createViewModel(
            FakeSessionRepository(TestFixtures.session()),
            FakeAppPreferencesRepository(biometricEnabled = true),
        )
        assertEquals(AuthState.Locked, viewModel.authState.value)

        viewModel.onUnlocked()

        assertEquals(AuthState.Authenticated, viewModel.authState.value)
    }

    @Test
    fun `without fingerprint sign-in a stored session goes straight in and registers the push token`() = runTest {
        val viewModel = createViewModel(
            FakeSessionRepository(TestFixtures.session()),
            FakeAppPreferencesRepository(biometricEnabled = false),
        )

        assertEquals(AuthState.Authenticated, viewModel.authState.value)
        assertEquals(1, tokens.registerCallCount)
    }

    @Test
    fun `a password login is never met by the lock`() = runTest {
        val sessions = FakeSessionRepository(initial = null)
        val viewModel = createViewModel(sessions, FakeAppPreferencesRepository(biometricEnabled = true))
        assertEquals(AuthState.Unauthenticated, viewModel.authState.value)

        sessions.saveSession(TestFixtures.session())

        assertEquals(AuthState.Authenticated, viewModel.authState.value)
    }

    @Test
    fun `a long absence locks again, a short one does not`() = runTest {
        val viewModel = createViewModel(
            FakeSessionRepository(TestFixtures.session()),
            FakeAppPreferencesRepository(biometricEnabled = true),
        )
        viewModel.onUnlocked()

        viewModel.onAppBackgrounded()
        clock.advanceBy(Duration.ofMinutes(2))
        viewModel.onAppForegrounded()
        assertEquals(AuthState.Authenticated, viewModel.authState.value)

        viewModel.onAppBackgrounded()
        clock.advanceBy(MainViewModel.RELOCK_AFTER)
        viewModel.onAppForegrounded()
        assertEquals(AuthState.Locked, viewModel.authState.value)
    }

    @Test
    fun `deepLink_whileRelockDue_survivesConsumeUntilUnlocked`() = runTest {
        val viewModel = createViewModel(
            FakeSessionRepository(TestFixtures.session()),
            FakeAppPreferencesRepository(biometricEnabled = true),
        )
        viewModel.onUnlocked()
        viewModel.onAppBackgrounded()
        clock.advanceBy(Duration.ofMinutes(6))

        // The tap arrives before the return to the foreground is seen.
        viewModel.onDeepLink(DESTINATION_ORDER_REQUESTS)
        assertEquals(AuthState.Locked, viewModel.authState.value)

        // The shell still on screen switches tabs and consumes, then is replaced by the gate.
        viewModel.onPendingDestinationConsumed()
        viewModel.onAppForegrounded()
        assertEquals(DESTINATION_ORDER_REQUESTS, viewModel.pendingDestination.value)

        viewModel.onUnlocked()
        viewModel.onPendingDestinationConsumed()
        assertNull(viewModel.pendingDestination.value)
    }

    @Test
    fun `a tap on a cold start with fingerprint keeps the destination until unlocked`() = runTest {
        val viewModel = createViewModel(
            FakeSessionRepository(TestFixtures.session()),
            FakeAppPreferencesRepository(biometricEnabled = true),
        )
        viewModel.onDeepLink(DESTINATION_ORDER_REQUESTS)

        viewModel.onPendingDestinationConsumed()
        assertEquals(DESTINATION_ORDER_REQUESTS, viewModel.pendingDestination.value)

        viewModel.onUnlocked()
        viewModel.onPendingDestinationConsumed()
        assertNull(viewModel.pendingDestination.value)
    }

    @Test
    fun `without fingerprint the destination is consumed at once`() = runTest {
        val viewModel = createViewModel(
            FakeSessionRepository(TestFixtures.session()),
            FakeAppPreferencesRepository(biometricEnabled = false),
        )
        viewModel.onAppBackgrounded()
        clock.advanceBy(Duration.ofMinutes(6))
        viewModel.onDeepLink(DESTINATION_ORDER_REQUESTS)

        viewModel.onPendingDestinationConsumed()

        assertEquals(AuthState.Authenticated, viewModel.authState.value)
        assertNull(viewModel.pendingDestination.value)
    }

    @Test
    fun `after a short background with fingerprint the destination is consumed at once`() = runTest {
        val viewModel = createViewModel(
            FakeSessionRepository(TestFixtures.session()),
            FakeAppPreferencesRepository(biometricEnabled = true),
        )
        viewModel.onUnlocked()
        viewModel.onAppBackgrounded()
        clock.advanceBy(Duration.ofMinutes(2))
        viewModel.onDeepLink(DESTINATION_ORDER_REQUESTS)

        viewModel.onPendingDestinationConsumed()

        assertEquals(AuthState.Authenticated, viewModel.authState.value)
        assertNull(viewModel.pendingDestination.value)
    }

    @Test
    fun `a registration that fails offline is retried when the network comes back`() = runTest {
        network.online(false)
        tokens.registerResult = Result.failure(java.io.IOException("offline"))
        createViewModel(FakeSessionRepository(TestFixtures.session()), FakeAppPreferencesRepository())
        assertEquals(1, tokens.registerCallCount)

        tokens.registerResult = Result.success(Unit)
        network.online(true)
        mainDispatcher.dispatcher.scheduler.advanceTimeBy(MainViewModel.PUSH_RETRY_PAUSE.toMillis() + 1)

        assertEquals(2, tokens.registerCallCount)
    }

    @Test
    fun `retries stop after the last attempt`() = runTest {
        tokens.registerResult = Result.failure(java.io.IOException("boom"))
        createViewModel(FakeSessionRepository(TestFixtures.session()), FakeAppPreferencesRepository())

        mainDispatcher.dispatcher.scheduler.advanceTimeBy(Duration.ofMinutes(10).toMillis())

        assertEquals(MainViewModel.MAX_PUSH_ATTEMPTS, tokens.registerCallCount)
    }

    @Test
    fun `signing out cancels the retries`() = runTest {
        network.online(false)
        tokens.registerResult = Result.failure(java.io.IOException("offline"))
        val sessions = FakeSessionRepository(TestFixtures.session())
        createViewModel(sessions, FakeAppPreferencesRepository())

        sessions.clearSession()
        network.online(true)
        mainDispatcher.dispatcher.scheduler.advanceTimeBy(Duration.ofMinutes(10).toMillis())

        assertEquals(1, tokens.registerCallCount)
    }
}
