package com.qhathia.mobile.staff.feature.auth.ui

import app.cash.turbine.test
import com.qhathia.mobile.staff.core.domain.usecase.LogoutUseCase
import com.qhathia.mobile.staff.core.domain.usecase.ObserveSessionUseCase
import com.qhathia.mobile.staff.core.domain.usecase.SetBiometricEnabledUseCase
import com.qhathia.mobile.staff.core.domain.usecase.UnregisterPushTokenUseCase
import com.qhathia.mobile.staff.core.testing.FakeAppPreferencesRepository
import com.qhathia.mobile.staff.core.testing.FakeAuthRepository
import com.qhathia.mobile.staff.core.testing.FakeNotificationTokenRepository
import com.qhathia.mobile.staff.core.testing.FakePushTokenProvider
import com.qhathia.mobile.staff.core.testing.FakeSessionRepository
import com.qhathia.mobile.staff.core.testing.MainDispatcherExtension
import com.qhathia.mobile.staff.core.testing.TestFixtures
import com.qhathia.mobile.staff.feature.auth.model.BiometricUnlockUiEffect
import com.qhathia.mobile.staff.feature.auth.model.BiometricUnlockUiEvent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class BiometricUnlockViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val sessions = FakeSessionRepository(TestFixtures.session())
    private val prefs = FakeAppPreferencesRepository(biometricEnabled = true)

    private fun createViewModel() = BiometricUnlockViewModel(
        observeSessionUseCase = ObserveSessionUseCase(sessions),
        setBiometricEnabledUseCase = SetBiometricEnabledUseCase(prefs),
        logoutUseCase = LogoutUseCase(
            sessionRepository = sessions,
            unregisterPushToken = UnregisterPushTokenUseCase(FakePushTokenProvider(), FakeNotificationTokenRepository()),
            authRepository = FakeAuthRepository(),
        ),
    )

    @Test
    fun `greets the locked merchant by first name`() = runTest {
        assertEquals("Juan", createViewModel().uiState.value.userFirstName)
    }

    @Test
    fun `a successful prompt unlocks`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiEffect.test {
            viewModel.onEvent(BiometricUnlockUiEvent.AuthenticationSucceeded)
            assertEquals(BiometricUnlockUiEffect.Unlocked, awaitItem())
        }
    }

    @Test
    fun `a dismissed prompt keeps the gate, a failed one explains why`() = runTest {
        val viewModel = createViewModel()

        viewModel.onEvent(BiometricUnlockUiEvent.AuthenticationFailed("Demasiados intentos"))
        assertEquals("Demasiados intentos", viewModel.uiState.value.lastError)

        viewModel.onEvent(BiometricUnlockUiEvent.AuthenticationCancelled)
        assertNull(viewModel.uiState.value.lastError)
        assertEquals(TestFixtures.session(), sessions.observeSession().first())
    }

    @Test
    fun `using the password signs out and keeps fingerprint sign-in enabled`() = runTest {
        val viewModel = createViewModel()

        viewModel.onEvent(BiometricUnlockUiEvent.UsePassword)

        assertNull(sessions.observeSession().first())
        assertTrue(prefs.biometricEnabled)
    }

    @Test
    fun `a removed enrollment clears the preference and continuing goes to login`() = runTest {
        val viewModel = createViewModel()

        viewModel.onEvent(BiometricUnlockUiEvent.BiometricGone)

        assertTrue(viewModel.uiState.value.isBiometricGone)
        assertFalse(prefs.biometricEnabled)

        viewModel.onEvent(BiometricUnlockUiEvent.UsePassword)
        assertNull(sessions.observeSession().first())
    }
}
