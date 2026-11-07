package com.qhathia.mobile.staff.feature.profile.notifications

import app.cash.turbine.test
import com.qhathia.mobile.staff.core.domain.model.NotificationPreference
import com.qhathia.mobile.staff.core.domain.model.NotificationPreferences
import com.qhathia.mobile.staff.core.domain.repository.NotificationPreferencesRepository
import com.qhathia.mobile.staff.core.domain.usecase.GetNotificationPreferencesUseCase
import com.qhathia.mobile.staff.core.domain.usecase.SetNotificationPreferenceUseCase
import com.qhathia.mobile.staff.core.testing.FakeNotificationPreferencesRepository
import com.qhathia.mobile.staff.core.testing.MainDispatcherExtension
import com.qhathia.mobile.staff.core.testing.TestFixtures
import com.qhathia.mobile.staff.feature.profile.common.LoadState
import com.qhathia.mobile.staff.feature.profile.common.valueOrNull
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.io.IOException

class NotificationPreferencesViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val repository = FakeNotificationPreferencesRepository()

    private fun createViewModel(repo: NotificationPreferencesRepository = repository) =
        NotificationPreferencesViewModel(
            getNotificationPreferencesUseCase = GetNotificationPreferencesUseCase(repo),
            setNotificationPreferenceUseCase = SetNotificationPreferenceUseCase(repo),
        )

    private val NotificationPreferencesViewModel.prefs: NotificationPreferences
        get() = uiState.value.preferences.valueOrNull!!

    @Test
    fun `loads the toggles on start`() = runTest {
        val viewModel = createViewModel()

        assertFalse(viewModel.prefs.isEnabled(NotificationPreference.LOW_BATTERY))
        assertEquals(6, viewModel.prefs.activeCount)
    }

    @Test
    fun `a failed load shows the error and retry recovers`() = runTest {
        repository.getFailure = IOException("offline")
        val viewModel = createViewModel()
        assertEquals(LoadState.Error, viewModel.uiState.value.preferences)

        repository.getFailure = null
        viewModel.onEvent(NotificationPreferencesUiEvent.Retry)

        assertTrue(viewModel.uiState.value.preferences is LoadState.Loaded)
    }

    @Test
    fun `a toggle saves only that preference and adopts the backend set`() = runTest {
        val viewModel = createViewModel()

        viewModel.onEvent(NotificationPreferencesUiEvent.Toggle(NotificationPreference.WEEKLY_REPORT, false))

        assertEquals(listOf(NotificationPreference.WEEKLY_REPORT to false), repository.saves)
        assertFalse(viewModel.prefs.isEnabled(NotificationPreference.WEEKLY_REPORT))
        assertEquals(5, viewModel.prefs.activeCount)
        assertTrue(viewModel.uiState.value.saving.isEmpty())
    }

    @Test
    fun `a failed save springs back and reports the error`() = runTest {
        repository.setFailure = IOException("offline")
        val viewModel = createViewModel()

        viewModel.uiEffect.test {
            viewModel.onEvent(NotificationPreferencesUiEvent.Toggle(NotificationPreference.NEW_ORDER, false))
            assertEquals(NotificationPreferencesUiEffect.ShowSaveError, awaitItem())
        }
        assertTrue(viewModel.prefs.isEnabled(NotificationPreference.NEW_ORDER))
        assertTrue(viewModel.uiState.value.saving.isEmpty())
    }

    @Test
    fun `a toggle is locked while its save is in flight, the others stay usable`() = runTest {
        val gated = GatedRepository()
        val viewModel = createViewModel(gated)

        viewModel.onEvent(NotificationPreferencesUiEvent.Toggle(NotificationPreference.NEW_ORDER, false))
        viewModel.onEvent(NotificationPreferencesUiEvent.Toggle(NotificationPreference.NEW_ORDER, true))
        viewModel.onEvent(NotificationPreferencesUiEvent.Toggle(NotificationPreference.WEEKLY_REPORT, false))

        assertEquals(
            listOf(NotificationPreference.NEW_ORDER, NotificationPreference.WEEKLY_REPORT),
            gated.requested,
        )
        assertEquals(
            setOf(NotificationPreference.NEW_ORDER, NotificationPreference.WEEKLY_REPORT),
            viewModel.uiState.value.saving,
        )

        // NEW_ORDER answers first with a set that does not know about WEEKLY_REPORT yet.
        gated.complete(NotificationPreference.NEW_ORDER)

        assertFalse(viewModel.prefs.isEnabled(NotificationPreference.NEW_ORDER))
        assertFalse(viewModel.prefs.isEnabled(NotificationPreference.WEEKLY_REPORT))
        assertEquals(setOf(NotificationPreference.WEEKLY_REPORT), viewModel.uiState.value.saving)
    }

    /** Holds each save until the test completes it; answers with only that save applied. */
    private class GatedRepository : NotificationPreferencesRepository {
        private val base = TestFixtures.notificationPreferences()
        private val gates = mutableMapOf<NotificationPreference, CompletableDeferred<Unit>>()
        private val pending = mutableMapOf<NotificationPreference, Boolean>()
        val requested = mutableListOf<NotificationPreference>()

        override suspend fun get(): Result<NotificationPreferences> = Result.success(base)

        override suspend fun set(
            preference: NotificationPreference,
            enabled: Boolean,
        ): Result<NotificationPreferences> {
            requested += preference
            pending[preference] = enabled
            gates.getOrPut(preference) { CompletableDeferred() }.await()
            val values = base.values + (preference to enabled)
            return Result.success(base.copy(values = values, activeCount = values.count { it.value }))
        }

        fun complete(preference: NotificationPreference) {
            gates.getValue(preference).complete(Unit)
        }
    }

    @Test
    fun `supported toggles and the warning minutes survive a toggle`() = runTest {
        repository.current = TestFixtures.notificationPreferences(orderExpiringWarningMinutes = 15)
        val viewModel = createViewModel()
        assertFalse(viewModel.prefs.isSupported(NotificationPreference.LOW_BATTERY))
        assertEquals(15, viewModel.prefs.orderExpiringWarningMinutes)

        viewModel.onEvent(NotificationPreferencesUiEvent.Toggle(NotificationPreference.NEW_ORDER, false))

        assertEquals(NotificationPreferences.DEFAULT_SUPPORTED, viewModel.prefs.supported)
        assertEquals(15, viewModel.prefs.orderExpiringWarningMinutes)
        assertEquals(1, viewModel.prefs.supportedActiveCount)
    }
}
