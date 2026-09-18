package com.qhathia.mobile.staff

import com.qhathia.mobile.staff.core.domain.usecase.ObserveBiometricEnabledUseCase
import com.qhathia.mobile.staff.core.domain.usecase.ObserveSessionUseCase
import com.qhathia.mobile.staff.core.domain.usecase.RegisterPushTokenUseCase
import com.qhathia.mobile.staff.core.testing.FakeAppPreferencesRepository
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

    private fun TestScope.createViewModel(
        sessions: FakeSessionRepository,
        prefs: FakeAppPreferencesRepository,
    ): MainViewModel {
        val viewModel = MainViewModel(
            observeSession = ObserveSessionUseCase(sessions),
            observeBiometricEnabled = ObserveBiometricEnabledUseCase(prefs),
            registerPushToken = RegisterPushTokenUseCase(FakePushTokenProvider(), tokens),
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
}
