package com.qhathia.mobile.staff.feature.profile.security

import app.cash.turbine.test
import com.qhathia.mobile.staff.core.domain.usecase.GetActiveSessionsUseCase
import com.qhathia.mobile.staff.core.domain.usecase.GetUserProfileUseCase
import com.qhathia.mobile.staff.core.domain.usecase.ObserveBiometricEnabledUseCase
import com.qhathia.mobile.staff.core.domain.usecase.RevokeOtherSessionsUseCase
import com.qhathia.mobile.staff.core.domain.usecase.RevokeSessionUseCase
import com.qhathia.mobile.staff.core.domain.usecase.SetBiometricEnabledUseCase
import com.qhathia.mobile.staff.core.testing.FakeAccountSecurityRepository
import com.qhathia.mobile.staff.core.testing.FakeAppPreferencesRepository
import com.qhathia.mobile.staff.core.testing.FakeSessionRepository
import com.qhathia.mobile.staff.core.testing.FakeUserProfileRepository
import com.qhathia.mobile.staff.core.testing.MainDispatcherExtension
import com.qhathia.mobile.staff.core.testing.MutableClock
import com.qhathia.mobile.staff.core.testing.TestFixtures
import com.qhathia.mobile.staff.feature.profile.R
import com.qhathia.mobile.staff.feature.profile.common.valueOrNull
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.io.IOException

class SecurityViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcher = MainDispatcherExtension()

    private val profiles = FakeUserProfileRepository()
    private val security = FakeAccountSecurityRepository()
    private val prefs = FakeAppPreferencesRepository()

    private fun createViewModel() = SecurityViewModel(
        getUserProfileUseCase = GetUserProfileUseCase(profiles),
        getActiveSessionsUseCase = GetActiveSessionsUseCase(security, FakeSessionRepository(TestFixtures.session())),
        revokeSessionUseCase = RevokeSessionUseCase(security),
        revokeOtherSessionsUseCase = RevokeOtherSessionsUseCase(security),
        observeBiometricEnabledUseCase = ObserveBiometricEnabledUseCase(prefs),
        setBiometricEnabledUseCase = SetBiometricEnabledUseCase(prefs),
        clock = MutableClock(),
    )

    private val SecurityViewModel.sessionIds: List<String>
        get() = uiState.value.sessions.valueOrNull.orEmpty().map { it.sessionId }

    @Test
    fun `this device is listed first even when the backend did not flag it`() = runTest {
        security.sessionsResult = Result.success(
            listOf(
                TestFixtures.activeSession(sessionId = "other", isCurrent = false),
                TestFixtures.activeSession(sessionId = "session-1", isCurrent = false),
            ),
        )

        val viewModel = createViewModel()

        assertEquals(listOf("session-1", "other"), viewModel.sessionIds)
        assertTrue(viewModel.uiState.value.sessions.valueOrNull!!.first().isCurrent)
        assertTrue(viewModel.uiState.value.canRevokeOthers)
    }

    @Test
    fun `closing another session asks first and removes it on success`() = runTest {
        val viewModel = createViewModel()
        val other = viewModel.uiState.value.sessions.valueOrNull!!.first { !it.isCurrent }

        viewModel.onEvent(SecurityUiEvent.RevokeRequested(other))
        assertTrue(security.revokedSessionIds.isEmpty())
        viewModel.onEvent(SecurityUiEvent.RevokeConfirmed)

        assertEquals(listOf(other.sessionId), security.revokedSessionIds)
        assertEquals(listOf("session-1"), viewModel.sessionIds)
        assertFalse(viewModel.uiState.value.canRevokeOthers)
    }

    @Test
    fun `a failed close keeps the row and reports it`() = runTest {
        security.revokeResult = Result.failure(IOException("offline"))
        val viewModel = createViewModel()
        val other = viewModel.uiState.value.sessions.valueOrNull!!.first { !it.isCurrent }

        viewModel.uiEffect.test {
            viewModel.onEvent(SecurityUiEvent.RevokeRequested(other))
            viewModel.onEvent(SecurityUiEvent.RevokeConfirmed)
            assertEquals(SecurityUiEffect.ShowMessage(R.string.security_revoke_error), awaitItem())
        }
        assertTrue(other.sessionId in viewModel.sessionIds)
        assertTrue(viewModel.uiState.value.revokingIds.isEmpty())
    }

    @Test
    fun `this device's own row cannot be closed from the list`() = runTest {
        val viewModel = createViewModel()
        val mine = viewModel.uiState.value.sessions.valueOrNull!!.first { it.isCurrent }

        viewModel.onEvent(SecurityUiEvent.RevokeRequested(mine))

        assertNull(viewModel.uiState.value.pendingRevoke)
    }

    @Test
    fun `closing all other sessions keeps only this device`() = runTest {
        val viewModel = createViewModel()

        viewModel.onEvent(SecurityUiEvent.RevokeOthersRequested)
        viewModel.onEvent(SecurityUiEvent.RevokeOthersConfirmed)

        assertEquals(1, security.revokeOthersCallCount)
        assertEquals(listOf("session-1"), viewModel.sessionIds)
    }

    @Test
    fun `enabling fingerprint prompts first and only then turns it on`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiEffect.test {
            viewModel.onEvent(SecurityUiEvent.BiometricToggled(true, DeviceBiometricStatus.AVAILABLE))
            assertEquals(SecurityUiEffect.PromptBiometric, awaitItem())
        }
        assertFalse(prefs.biometricEnabled)

        viewModel.onEvent(SecurityUiEvent.BiometricPromptFinished(succeeded = false))
        assertFalse(prefs.biometricEnabled)

        viewModel.onEvent(SecurityUiEvent.BiometricPromptFinished(succeeded = true))
        assertTrue(prefs.biometricEnabled)
        assertTrue(viewModel.uiState.value.biometricEnabled)
    }

    @Test
    fun `without an enrolled fingerprint the toggle stays off and points to the settings`() = runTest {
        val viewModel = createViewModel()

        viewModel.onEvent(SecurityUiEvent.BiometricToggled(true, DeviceBiometricStatus.NONE_ENROLLED))

        assertEquals(BiometricHint.NONE_ENROLLED, viewModel.uiState.value.biometricHint)
        assertFalse(prefs.biometricEnabled)

        viewModel.uiEffect.test {
            viewModel.onEvent(SecurityUiEvent.OpenEnrollmentSettings)
            assertEquals(SecurityUiEffect.OpenEnrollmentSettings, awaitItem())
        }
        assertNull(viewModel.uiState.value.biometricHint)
    }

    @Test
    fun `a device without a sensor explains it and never enables`() = runTest {
        val viewModel = createViewModel()

        viewModel.onEvent(SecurityUiEvent.BiometricToggled(true, DeviceBiometricStatus.NO_HARDWARE))

        assertEquals(BiometricHint.NOT_AVAILABLE, viewModel.uiState.value.biometricHint)
        assertFalse(prefs.biometricEnabled)
    }

    @Test
    fun `resuming after the enrollment was removed turns fingerprint off`() = runTest {
        prefs.setBiometricEnabled(true)
        val viewModel = createViewModel()

        viewModel.onEvent(SecurityUiEvent.Resumed(DeviceBiometricStatus.NONE_ENROLLED))

        assertFalse(prefs.biometricEnabled)
    }

    @Test
    fun `turning fingerprint off needs no prompt`() = runTest {
        prefs.setBiometricEnabled(true)
        val viewModel = createViewModel()

        viewModel.onEvent(SecurityUiEvent.BiometricToggled(false, DeviceBiometricStatus.AVAILABLE))

        assertFalse(prefs.biometricEnabled)
    }

    @Test
    fun `a changed password is confirmed and its date reloaded`() = runTest {
        val viewModel = createViewModel()
        val loads = profiles.callCount

        viewModel.uiEffect.test {
            viewModel.onEvent(SecurityUiEvent.PasswordChanged)
            assertEquals(SecurityUiEffect.ShowMessage(R.string.security_password_changed), awaitItem())
        }
        assertEquals(loads + 1, profiles.callCount)
    }

    // ---- Sessions grouped by device, closing a group, the silent clear says so ----

    private fun sim(id: String, minutesAgo: Long) =
        TestFixtures.activeSession(sessionId = id, deviceLabel = "qa-sim-tablet", lastUsedAt = TestFixtures.NOW.minusSeconds(minutesAgo * 60))

    @Test
    fun `sessions are grouped by device with this phone first and the newest device next`() = runTest {
        security.sessionsResult = Result.success(
            listOf(
                TestFixtures.activeSession(sessionId = "session-1", deviceLabel = "Redmi Note 12", isCurrent = true),
                TestFixtures.activeSession(sessionId = "tab", deviceLabel = "Tablet vitrina", lastUsedAt = TestFixtures.NOW.minusSeconds(3_600)),
                sim("s1", 5),
                sim("s2", 1),
                sim("s3", 30),
            ),
        )

        val state = createViewModel().uiState.value

        assertEquals("session-1", state.currentSession?.sessionId)
        assertEquals(listOf("qa-sim-tablet", "Tablet vitrina"), state.otherGroups.map { it.label })
        assertEquals(3, state.otherGroups.first().sessions.size)
        assertEquals(TestFixtures.NOW.minusSeconds(60), state.otherGroups.first().lastUsedAt)
        assertEquals(1, state.otherGroups.last().sessions.size)
    }

    @Test
    fun `closing a device closes each of its sessions and reads the list again`() = runTest {
        security.sessionsResult = Result.success(
            listOf(TestFixtures.activeSession(sessionId = "session-1", isCurrent = true), sim("s1", 5), sim("s2", 1)),
        )
        val viewModel = createViewModel()
        val group = viewModel.uiState.value.otherGroups.single()
        viewModel.onEvent(SecurityUiEvent.RevokeGroupRequested(group))
        assertTrue(security.revokedSessionIds.isEmpty())
        security.sessionsResult = Result.success(listOf(TestFixtures.activeSession(sessionId = "session-1", isCurrent = true)))

        viewModel.onEvent(SecurityUiEvent.RevokeGroupConfirmed)

        assertEquals(setOf("s1", "s2"), security.revokedSessionIds.toSet())
        assertTrue(viewModel.uiState.value.otherGroups.isEmpty())
        assertNull(viewModel.uiState.value.pendingRevokeGroup)
        assertTrue(viewModel.uiState.value.revokingIds.isEmpty())
    }

    @Test
    fun `a failed close in a group says so`() = runTest {
        security.sessionsResult = Result.success(listOf(sim("s1", 5), sim("s2", 1)))
        security.revokeResult = Result.failure(java.io.IOException("offline"))
        val viewModel = createViewModel()
        viewModel.onEvent(SecurityUiEvent.RevokeGroupRequested(viewModel.uiState.value.otherGroups.single()))

        viewModel.uiEffect.test {
            viewModel.onEvent(SecurityUiEvent.RevokeGroupConfirmed)
            assertEquals(SecurityUiEffect.ShowMessage(R.string.security_revoke_error), awaitItem())
        }
    }

    @Test
    fun `turning fingerprint sign-in off because the fingerprint is gone says why`() = runTest {
        prefs.setBiometricEnabled(true)
        val viewModel = createViewModel()

        viewModel.uiEffect.test {
            viewModel.onEvent(SecurityUiEvent.Resumed(DeviceBiometricStatus.NONE_ENROLLED))
            assertEquals(SecurityUiEffect.ShowMessage(R.string.security_biometric_disabled_gone), awaitItem())
        }
        assertFalse(prefs.biometricEnabled)
    }
}
