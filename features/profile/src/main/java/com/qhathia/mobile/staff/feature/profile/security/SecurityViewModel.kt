package com.qhathia.mobile.staff.feature.profile.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qhathia.mobile.staff.core.domain.usecase.GetActiveSessionsUseCase
import com.qhathia.mobile.staff.core.domain.usecase.GetUserProfileUseCase
import com.qhathia.mobile.staff.core.domain.usecase.ObserveBiometricEnabledUseCase
import com.qhathia.mobile.staff.core.domain.usecase.RevokeOtherSessionsUseCase
import com.qhathia.mobile.staff.core.domain.usecase.RevokeSessionUseCase
import com.qhathia.mobile.staff.core.domain.usecase.SetBiometricEnabledUseCase
import com.qhathia.mobile.staff.feature.profile.R
import com.qhathia.mobile.staff.feature.profile.common.LoadState
import com.qhathia.mobile.staff.feature.profile.common.afterFailure
import com.qhathia.mobile.staff.feature.profile.common.valueOrNull
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

/**
 * Drives Seguridad: when the password was last changed, the signed-in devices, and the fingerprint
 * setting.
 *
 * Fingerprint sign-in is only turned on after the merchant passes the device's own prompt, so the
 * preference never promises a gate the device cannot open. Without an enrolled biometric the toggle
 * stays off and the merchant is pointed to the device settings; if the enrollment disappears later,
 * the next resume turns the preference off.
 */
@HiltViewModel
class SecurityViewModel @Inject constructor(
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val getActiveSessionsUseCase: GetActiveSessionsUseCase,
    private val revokeSessionUseCase: RevokeSessionUseCase,
    private val revokeOtherSessionsUseCase: RevokeOtherSessionsUseCase,
    observeBiometricEnabledUseCase: ObserveBiometricEnabledUseCase,
    private val setBiometricEnabledUseCase: SetBiometricEnabledUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SecurityUiState(now = clock.instant()))
    val uiState: StateFlow<SecurityUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<SecurityUiEffect>(
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val uiEffect: SharedFlow<SecurityUiEffect> = _uiEffect.asSharedFlow()

    init {
        observeBiometricEnabledUseCase()
            .onEach { enabled -> _uiState.update { it.copy(biometricEnabled = enabled) } }
            .launchIn(viewModelScope)
        loadPasswordDate()
        loadSessions()
    }

    fun onEvent(event: SecurityUiEvent) {
        when (event) {
            is SecurityUiEvent.Resumed -> onResumed(event.biometric)
            SecurityUiEvent.RetrySessions -> loadSessions()
            SecurityUiEvent.ChangePasswordClicked -> _uiEffect.tryEmit(SecurityUiEffect.NavigateToChangePassword)
            SecurityUiEvent.PasswordChanged -> {
                _uiEffect.tryEmit(SecurityUiEffect.ShowMessage(R.string.security_password_changed))
                loadPasswordDate()
            }
            is SecurityUiEvent.BiometricToggled -> onBiometricToggled(event.enabled, event.biometric)
            is SecurityUiEvent.BiometricPromptFinished -> if (event.succeeded) {
                viewModelScope.launch { setBiometricEnabledUseCase(true) }
            }
            SecurityUiEvent.BiometricHintDismissed -> _uiState.update { it.copy(biometricHint = null) }
            SecurityUiEvent.OpenEnrollmentSettings -> {
                _uiState.update { it.copy(biometricHint = null) }
                _uiEffect.tryEmit(SecurityUiEffect.OpenEnrollmentSettings)
            }
            is SecurityUiEvent.RevokeRequested -> if (!event.session.isCurrent) {
                _uiState.update { it.copy(pendingRevoke = event.session) }
            }
            SecurityUiEvent.RevokeDismissed -> _uiState.update { it.copy(pendingRevoke = null) }
            SecurityUiEvent.RevokeConfirmed -> revokePending()
            is SecurityUiEvent.RevokeGroupRequested -> _uiState.update { it.copy(pendingRevokeGroup = event.group) }
            SecurityUiEvent.RevokeGroupDismissed -> _uiState.update { it.copy(pendingRevokeGroup = null) }
            SecurityUiEvent.RevokeGroupConfirmed -> revokeGroup()
            SecurityUiEvent.RevokeOthersRequested -> _uiState.update { it.copy(isRevokeOthersConfirmVisible = true) }
            SecurityUiEvent.RevokeOthersDismissed -> _uiState.update { it.copy(isRevokeOthersConfirmVisible = false) }
            SecurityUiEvent.RevokeOthersConfirmed -> revokeOthers()
        }
    }

    private fun onResumed(biometric: DeviceBiometricStatus) {
        _uiState.update { it.copy(now = clock.instant()) }
        if (_uiState.value.biometricEnabled && biometric != DeviceBiometricStatus.AVAILABLE) {
            viewModelScope.launch { setBiometricEnabledUseCase(false) }
            // Turning it off is right, but not silently: say why the switch moved.
            _uiEffect.tryEmit(SecurityUiEffect.ShowMessage(R.string.security_biometric_disabled_gone))
        }
    }

    private fun onBiometricToggled(enabled: Boolean, biometric: DeviceBiometricStatus) {
        if (!enabled) {
            viewModelScope.launch { setBiometricEnabledUseCase(false) }
            return
        }
        when (biometric) {
            DeviceBiometricStatus.AVAILABLE -> _uiEffect.tryEmit(SecurityUiEffect.PromptBiometric)
            DeviceBiometricStatus.NONE_ENROLLED ->
                _uiState.update { it.copy(biometricHint = BiometricHint.NONE_ENROLLED) }
            DeviceBiometricStatus.NO_HARDWARE,
            DeviceBiometricStatus.UNAVAILABLE,
            -> _uiState.update { it.copy(biometricHint = BiometricHint.NOT_AVAILABLE) }
        }
    }

    private fun loadPasswordDate() {
        viewModelScope.launch {
            getUserProfileUseCase()
                .onSuccess { profile ->
                    _uiState.update {
                        it.copy(passwordUpdatedAt = LoadState.Loaded(profile.passwordUpdatedAt), now = clock.instant())
                    }
                }
                .onFailure { _uiState.update { it.copy(passwordUpdatedAt = it.passwordUpdatedAt.afterFailure()) } }
        }
    }

    private fun loadSessions() {
        viewModelScope.launch {
            if (_uiState.value.sessions !is LoadState.Loaded) {
                _uiState.update { it.copy(sessions = LoadState.Loading) }
            }
            getActiveSessionsUseCase()
                .onSuccess { sessions -> _uiState.update { it.copy(sessions = LoadState.Loaded(sessions)) } }
                .onFailure { _uiState.update { it.copy(sessions = it.sessions.afterFailure()) } }
        }
    }

    private fun revokePending() {
        val session = _uiState.value.pendingRevoke ?: return
        _uiState.update { it.copy(pendingRevoke = null, revokingIds = it.revokingIds + session.sessionId) }
        viewModelScope.launch {
            revokeSessionUseCase(session.sessionId)
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            sessions = state.sessions.valueOrNull
                                ?.let { list -> LoadState.Loaded(list.filterNot { it.sessionId == session.sessionId }) }
                                ?: state.sessions,
                            revokingIds = state.revokingIds - session.sessionId,
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(revokingIds = it.revokingIds - session.sessionId) }
                    _uiEffect.tryEmit(SecurityUiEffect.ShowMessage(R.string.security_revoke_error))
                }
        }
    }

    /** Closes every session of one device, one after the other, then reads the list again. */
    private fun revokeGroup() {
        val group = _uiState.value.pendingRevokeGroup ?: return
        val ids = group.sessions.map { it.sessionId }
        _uiState.update { it.copy(pendingRevokeGroup = null, revokingIds = it.revokingIds + ids) }
        viewModelScope.launch {
            var failed = false
            ids.forEach { id -> if (revokeSessionUseCase(id).isFailure) failed = true }
            _uiState.update { it.copy(revokingIds = it.revokingIds - ids.toSet()) }
            loadSessions()
            if (failed) _uiEffect.tryEmit(SecurityUiEffect.ShowMessage(R.string.security_revoke_error))
        }
    }

    private fun revokeOthers() {
        if (_uiState.value.isRevokingOthers) return
        _uiState.update { it.copy(isRevokeOthersConfirmVisible = false, isRevokingOthers = true) }
        viewModelScope.launch {
            revokeOtherSessionsUseCase()
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            sessions = state.sessions.valueOrNull
                                ?.let { list -> LoadState.Loaded(list.filter { it.isCurrent }) }
                                ?: state.sessions,
                            isRevokingOthers = false,
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isRevokingOthers = false) }
                    _uiEffect.tryEmit(SecurityUiEffect.ShowMessage(R.string.security_revoke_error))
                }
        }
    }
}
