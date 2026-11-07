package com.qhathia.mobile.staff.feature.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qhathia.mobile.staff.core.domain.usecase.LogoutUseCase
import com.qhathia.mobile.staff.core.domain.usecase.ObserveSessionUseCase
import com.qhathia.mobile.staff.core.domain.usecase.SetBiometricEnabledUseCase
import com.qhathia.mobile.staff.feature.auth.model.BiometricUnlockUiEffect
import com.qhathia.mobile.staff.feature.auth.model.BiometricUnlockUiEvent
import com.qhathia.mobile.staff.feature.auth.model.BiometricUnlockUiState
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
import javax.inject.Inject

/**
 * Drives the unlock screen shown over a stored session when fingerprint sign-in is on. It never
 * talks to the sensor itself — the screen runs the system prompt and reports the outcome.
 *
 * The password fallback signs out: the session is cleared, the app falls back to login, and the
 * fingerprint preference is left as it was so the merchant does not have to enable it again.
 *
 * When the enrolled biometric is gone the screen says so and waits for "Continuar". The preference
 * is cleared only after that sign-out: clearing it first would lift the gate into the app before
 * the notice is ever seen. Clearing it afterwards keeps a gate that can never open from greeting
 * the merchant on the next sign-in.
 */
@HiltViewModel
class BiometricUnlockViewModel @Inject constructor(
    observeSessionUseCase: ObserveSessionUseCase,
    private val setBiometricEnabledUseCase: SetBiometricEnabledUseCase,
    private val logoutUseCase: LogoutUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BiometricUnlockUiState())
    val uiState: StateFlow<BiometricUnlockUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<BiometricUnlockUiEffect>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val uiEffect: SharedFlow<BiometricUnlockUiEffect> = _uiEffect.asSharedFlow()

    init {
        observeSessionUseCase()
            .onEach { session ->
                val firstName = session?.user?.fullName?.trim()?.substringBefore(' ')
                _uiState.update { it.copy(userFirstName = firstName?.takeIf { name -> name.isNotEmpty() }) }
            }
            .launchIn(viewModelScope)
    }

    fun onEvent(event: BiometricUnlockUiEvent) {
        when (event) {
            BiometricUnlockUiEvent.AuthenticationSucceeded -> {
                _uiState.update { it.copy(lastError = null) }
                _uiEffect.tryEmit(BiometricUnlockUiEffect.Unlocked)
            }
            BiometricUnlockUiEvent.AuthenticationCancelled ->
                _uiState.update { it.copy(lastError = null) }
            is BiometricUnlockUiEvent.AuthenticationFailed ->
                _uiState.update { it.copy(lastError = event.message) }
            BiometricUnlockUiEvent.BiometricGone ->
                _uiState.update { it.copy(isBiometricGone = true, lastError = null) }
            BiometricUnlockUiEvent.UsePassword -> signOut()
        }
    }

    private fun signOut() {
        if (_uiState.value.isSigningOut) return
        _uiState.update { it.copy(isSigningOut = true) }
        viewModelScope.launch {
            val gone = _uiState.value.isBiometricGone
            // Signing out first: once there is no session, clearing the preference cannot lift the
            // gate into the app.
            logoutUseCase()
            if (gone) setBiometricEnabledUseCase(false)
        }
    }
}
