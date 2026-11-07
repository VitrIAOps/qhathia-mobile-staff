package com.qhathia.mobile.staff.feature.profile.security

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qhathia.mobile.staff.core.domain.model.ChangePasswordError
import com.qhathia.mobile.staff.core.domain.model.ChangePasswordException
import com.qhathia.mobile.staff.core.domain.usecase.ChangePasswordUseCase
import com.qhathia.mobile.staff.feature.profile.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The backend accepts new passwords of 8–100 characters, as on registration. */
internal val PASSWORD_LENGTH = 8..100

/**
 * Cambiar contraseña. Field errors are string resources; [bannerErrorRes] carries failures that
 * belong to no field (connectivity, unknown). [isDiscardDialogVisible] is the "¿Descartar cambios?"
 * asked when leaving with something typed.
 */
data class ChangePasswordUiState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmation: String = "",
    @param:StringRes val currentPasswordErrorRes: Int? = null,
    @param:StringRes val newPasswordErrorRes: Int? = null,
    @param:StringRes val bannerErrorRes: Int? = null,
    val isSubmitting: Boolean = false,
    val isDiscardDialogVisible: Boolean = false,
) {
    /** Nothing is loaded here, so any typing is a change worth asking about. */
    val isDirty: Boolean
        get() = currentPassword.isNotEmpty() || newPassword.isNotEmpty() || confirmation.isNotEmpty()

    /** Shown as soon as the confirmation diverges, not only on submit. */
    val isConfirmationMismatch: Boolean
        get() = confirmation.isNotEmpty() && confirmation != newPassword

    val canSubmit: Boolean
        get() = currentPassword.isNotEmpty() &&
            newPassword.length in PASSWORD_LENGTH &&
            confirmation == newPassword &&
            !isSubmitting
}

sealed interface ChangePasswordUiEvent {
    data class CurrentPasswordChanged(val value: String) : ChangePasswordUiEvent
    data class NewPasswordChanged(val value: String) : ChangePasswordUiEvent
    data class ConfirmationChanged(val value: String) : ChangePasswordUiEvent
    data object Submit : ChangePasswordUiEvent

    /** System back or the top bar's back. */
    data object BackRequested : ChangePasswordUiEvent
    data object DiscardConfirmed : ChangePasswordUiEvent
    data object DiscardDismissed : ChangePasswordUiEvent
}

sealed interface ChangePasswordUiEffect {
    data object PasswordChanged : ChangePasswordUiEffect
    data object NavigateBack : ChangePasswordUiEffect
}

/**
 * Validates and submits a password change. The data layer sends this device's refresh token along,
 * so a success keeps the merchant signed in here while the other devices are signed out.
 */
@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val changePasswordUseCase: ChangePasswordUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChangePasswordUiState())
    val uiState: StateFlow<ChangePasswordUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<ChangePasswordUiEffect>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val uiEffect: SharedFlow<ChangePasswordUiEffect> = _uiEffect.asSharedFlow()

    fun onEvent(event: ChangePasswordUiEvent) {
        when (event) {
            is ChangePasswordUiEvent.CurrentPasswordChanged -> _uiState.update {
                it.copy(currentPassword = event.value, currentPasswordErrorRes = null, bannerErrorRes = null)
            }
            is ChangePasswordUiEvent.NewPasswordChanged -> _uiState.update {
                it.copy(newPassword = event.value, newPasswordErrorRes = null, bannerErrorRes = null)
            }
            is ChangePasswordUiEvent.ConfirmationChanged -> _uiState.update {
                it.copy(confirmation = event.value, bannerErrorRes = null)
            }
            ChangePasswordUiEvent.Submit -> submit()
            ChangePasswordUiEvent.BackRequested -> onBackRequested()
            ChangePasswordUiEvent.DiscardConfirmed -> {
                _uiState.update { it.copy(isDiscardDialogVisible = false) }
                _uiEffect.tryEmit(ChangePasswordUiEffect.NavigateBack)
            }
            ChangePasswordUiEvent.DiscardDismissed -> _uiState.update { it.copy(isDiscardDialogVisible = false) }
        }
    }

    /** Leaving mid-request would drop its result, so back waits; typed passwords ask first. */
    private fun onBackRequested() {
        val state = _uiState.value
        when {
            state.isSubmitting -> Unit
            state.isDirty -> _uiState.update { it.copy(isDiscardDialogVisible = true) }
            else -> _uiEffect.tryEmit(ChangePasswordUiEffect.NavigateBack)
        }
    }

    private fun submit() {
        val state = _uiState.value
        if (!state.canSubmit) return
        _uiState.update { it.copy(isSubmitting = true, bannerErrorRes = null) }
        viewModelScope.launch {
            changePasswordUseCase(state.currentPassword, state.newPassword)
                .onSuccess {
                    _uiState.update { it.copy(isSubmitting = false) }
                    _uiEffect.tryEmit(ChangePasswordUiEffect.PasswordChanged)
                }
                .onFailure { throwable ->
                    val error = (throwable as? ChangePasswordException)?.error
                    _uiState.update { it.withError(error).copy(isSubmitting = false) }
                }
        }
    }

    private fun ChangePasswordUiState.withError(error: ChangePasswordError?): ChangePasswordUiState =
        when (error) {
            ChangePasswordError.WrongCurrent ->
                copy(currentPasswordErrorRes = R.string.change_password_error_wrong_current)
            ChangePasswordError.SameAsCurrent ->
                copy(newPasswordErrorRes = R.string.change_password_error_same)
            ChangePasswordError.Validation ->
                copy(newPasswordErrorRes = R.string.change_password_error_length)
            ChangePasswordError.Network ->
                copy(bannerErrorRes = R.string.change_password_error_network)
            is ChangePasswordError.Unknown, null ->
                copy(bannerErrorRes = R.string.change_password_error_unknown)
        }
}
