package com.qhathia.mobile.staff.feature.profile.hub

import app.cash.turbine.test
import com.qhathia.mobile.staff.core.domain.model.NotificationPreference
import com.qhathia.mobile.staff.core.domain.model.UserRole
import com.qhathia.mobile.staff.core.domain.usecase.GetDevicesUseCase
import com.qhathia.mobile.staff.core.domain.usecase.GetNotificationPreferencesUseCase
import com.qhathia.mobile.staff.core.domain.usecase.GetSubscriptionUseCase
import com.qhathia.mobile.staff.core.domain.usecase.GetUserProfileUseCase
import com.qhathia.mobile.staff.core.domain.usecase.LogoutUseCase
import com.qhathia.mobile.staff.core.domain.usecase.ObserveSessionUseCase
import com.qhathia.mobile.staff.core.domain.usecase.UnregisterPushTokenUseCase
import com.qhathia.mobile.staff.core.testing.FakeAuthRepository
import com.qhathia.mobile.staff.core.testing.FakeDeviceRepository
import com.qhathia.mobile.staff.core.testing.FakeNotificationPreferencesRepository
import com.qhathia.mobile.staff.core.testing.FakeNotificationTokenRepository
import com.qhathia.mobile.staff.core.testing.FakePushTokenProvider
import com.qhathia.mobile.staff.core.testing.FakeSessionRepository
import com.qhathia.mobile.staff.core.testing.FakeSubscriptionRepository
import com.qhathia.mobile.staff.core.testing.FakeUserProfileRepository
import com.qhathia.mobile.staff.core.testing.MainDispatcherExtension
import com.qhathia.mobile.staff.core.testing.MutableClock
import com.qhathia.mobile.staff.core.testing.TestFixtures
import com.qhathia.mobile.staff.feature.profile.common.LoadState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.io.IOException
import java.time.Duration

class ProfileHubViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val profiles = FakeUserProfileRepository()
    private val subscriptions = FakeSubscriptionRepository()
    private val preferences = FakeNotificationPreferencesRepository()
    private val devices = FakeDeviceRepository()
    private val clock = MutableClock()

    private fun sessions(role: UserRole) =
        FakeSessionRepository(TestFixtures.session(user = TestFixtures.user(role = role)))

    /** The hub loads on its first resume; [resumed] = false leaves it before that. */
    private fun createViewModel(
        sessionRepository: FakeSessionRepository = sessions(UserRole.OWNER),
        resumed: Boolean = true,
    ) = ProfileHubViewModel(
            observeSessionUseCase = ObserveSessionUseCase(sessionRepository),
            getUserProfileUseCase = GetUserProfileUseCase(profiles),
            getSubscriptionUseCase = GetSubscriptionUseCase(subscriptions),
            getNotificationPreferencesUseCase = GetNotificationPreferencesUseCase(preferences),
            getDevicesUseCase = GetDevicesUseCase(devices),
            logoutUseCase = LogoutUseCase(
                sessionRepository = sessionRepository,
                unregisterPushToken = UnregisterPushTokenUseCase(FakePushTokenProvider(), FakeNotificationTokenRepository()),
                authRepository = FakeAuthRepository(),
            ),
            clock = clock,
        ).also { if (resumed) it.onEvent(ProfileHubUiEvent.Resumed) }

    @Test
    fun `an owner sees profile, subscription, alert counts and linked and connected tablets`() = runTest {
        devices.devicesResult = Result.success(
            listOf(
                TestFixtures.device(id = "d1", lastPingAt = clock.now.minus(Duration.ofMinutes(2))),
                TestFixtures.device(id = "d2", lastPingAt = clock.now.minus(Duration.ofHours(3))),
            ),
        )

        val state = createViewModel().uiState.value

        assertTrue(state.canManageStore)
        assertEquals("Carla Mendoza", state.displayName)
        assertEquals(3, state.storeCount)
        assertTrue(state.subscription is LoadState.Loaded)
        assertEquals(AlertCounts(active = 2, total = 2), state.alertCounts)
        assertEquals(TabletCounts(linked = 2, connected = 1), state.tablets)
    }

    @Test
    fun `a seller never requests the subscription or the tablets`() = runTest {
        val state = createViewModel(sessions(UserRole.SELLER)).uiState.value

        assertFalse(state.canManageStore)
        assertEquals(0, subscriptions.callCount)
        assertEquals(0, devices.getDevicesCallCount)
        assertEquals(AlertCounts(active = 2, total = 2), state.alertCounts)
    }

    @Test
    fun `each block fails on its own and the session fills in the identity`() = runTest {
        profiles.profileResult = Result.failure(IOException("offline"))
        preferences.getFailure = IOException("offline")
        subscriptions.subscriptionResult = Result.failure(IOException("offline"))

        val state = createViewModel().uiState.value

        assertEquals(LoadState.Error, state.profile)
        assertEquals("Juan Pérez", state.displayName)
        assertNull(state.alertCounts)
        assertEquals(LoadState.Error, state.subscription)
        assertEquals(TabletCounts(linked = 1, connected = 1), state.tablets)
    }

    @Test
    fun `a store without subscription is shown as loaded with none`() = runTest {
        subscriptions.subscriptionResult = Result.success(null)

        assertEquals(LoadState.Loaded(null), createViewModel().uiState.value.subscription)
    }

    @Test
    fun `a failed refresh keeps what is already on screen and picks up new counts`() = runTest {
        val viewModel = createViewModel()
        profiles.profileResult = Result.failure(IOException("offline"))
        preferences.set(NotificationPreference.NEW_ORDER, false)

        viewModel.onEvent(ProfileHubUiEvent.Retry)

        val state = viewModel.uiState.value
        assertTrue(state.profile is LoadState.Loaded)
        // Only the alerts the backend sends count: "1 de 2".
        assertEquals(AlertCounts(active = 1, total = 2), state.alertCounts)
        assertEquals(2, profiles.callCount)
    }

    @Test
    fun `menu choices are raised as navigation`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiEffect.test {
            viewModel.onEvent(ProfileHubUiEvent.Open(ProfileDestination.SECURITY))
            assertEquals(ProfileHubUiEffect.Navigate(ProfileDestination.SECURITY), awaitItem())
        }
    }

    @Test
    fun `sign out asks for confirmation before clearing the session`() = runTest {
        val sessionRepository = sessions(UserRole.OWNER)
        val viewModel = createViewModel(sessionRepository)

        viewModel.onEvent(ProfileHubUiEvent.LogoutRequested)
        assertTrue(viewModel.uiState.value.isLogoutConfirmVisible)
        assertEquals(0, sessionRepository.clearCallCount)

        viewModel.onEvent(ProfileHubUiEvent.LogoutConfirmed)

        assertFalse(viewModel.uiState.value.isLogoutConfirmVisible)
        assertNull(sessionRepository.observeSession().first())
    }

    @Test
    fun `alert counts ignore toggles the backend does not send`() = runTest {
        preferences.current = TestFixtures.notificationPreferences(
            values = NotificationPreference.entries.associateWith { true },
        )

        assertEquals(AlertCounts(active = 2, total = 2), createViewModel().uiState.value.alertCounts)
    }

    @Test
    fun `nothing loads before the first resume`() = runTest {
        createViewModel(resumed = false)

        assertEquals(0, profiles.callCount)
        assertEquals(0, preferences.getCallCount)
    }

    @Test
    fun `a second resume within 30 seconds loads nothing`() = runTest {
        val viewModel = createViewModel()
        clock.advanceBy(Duration.ofSeconds(10))

        viewModel.onEvent(ProfileHubUiEvent.Resumed)

        assertEquals(1, profiles.callCount)
        assertEquals(1, preferences.getCallCount)
        assertEquals(1, subscriptions.callCount)
        assertEquals(1, devices.getDevicesCallCount)
    }

    @Test
    fun `a resume after 30 seconds reloads`() = runTest {
        val viewModel = createViewModel()
        clock.advanceBy(ProfileHubViewModel.FRESH_FOR)

        viewModel.onEvent(ProfileHubUiEvent.Resumed)

        assertEquals(2, profiles.callCount)
    }

    @Test
    fun `back from Notificaciones reloads at once and shows the new count`() = runTest {
        val viewModel = createViewModel()
        viewModel.onEvent(ProfileHubUiEvent.Open(ProfileDestination.NOTIFICATIONS))
        preferences.set(NotificationPreference.NEW_ORDER, false)
        clock.advanceBy(Duration.ofSeconds(5))

        viewModel.onEvent(ProfileHubUiEvent.Resumed)

        assertEquals(2, preferences.getCallCount)
        assertEquals(AlertCounts(active = 1, total = 2), viewModel.uiState.value.alertCounts)
    }

    @Test
    fun `back from Datos personales or Dispositivos reloads at once`() = runTest {
        val viewModel = createViewModel()

        viewModel.onEvent(ProfileHubUiEvent.Open(ProfileDestination.PERSONAL_DATA))
        viewModel.onEvent(ProfileHubUiEvent.Resumed)
        viewModel.onEvent(ProfileHubUiEvent.Open(ProfileDestination.DEVICES))
        viewModel.onEvent(ProfileHubUiEvent.Resumed)

        assertEquals(3, profiles.callCount)
        assertEquals(3, devices.getDevicesCallCount)
    }

    @Test
    fun `back from Ayuda within 30 seconds loads nothing, and the next resume is plain again`() = runTest {
        val viewModel = createViewModel()
        viewModel.onEvent(ProfileHubUiEvent.Open(ProfileDestination.NOTIFICATIONS))
        viewModel.onEvent(ProfileHubUiEvent.Resumed)
        viewModel.onEvent(ProfileHubUiEvent.Open(ProfileDestination.HELP))

        viewModel.onEvent(ProfileHubUiEvent.Resumed)
        viewModel.onEvent(ProfileHubUiEvent.Resumed)

        assertEquals(2, profiles.callCount)
    }

    @Test
    fun `retry always reloads`() = runTest {
        val viewModel = createViewModel()

        viewModel.onEvent(ProfileHubUiEvent.Retry)

        assertEquals(2, profiles.callCount)
    }
}
