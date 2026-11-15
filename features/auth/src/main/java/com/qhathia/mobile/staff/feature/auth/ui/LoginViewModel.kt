package com.qhathia.mobile.staff.feature.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qhathia.mobile.staff.core.common.validation.isValidEmail
import com.qhathia.mobile.staff.core.domain.model.LoginError
import com.qhathia.mobile.staff.core.domain.model.LoginException
import com.qhathia.mobile.staff.core.domain.usecase.LoginUseCase
import com.qhathia.mobile.staff.feature.auth.R
import com.qhathia.mobile.staff.feature.auth.model.LoginUiEffect
import com.qhathia.mobile.staff.feature.auth.model.LoginUiEvent
import com.qhathia.mobile.staff.feature.auth.model.LoginUiState
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

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<LoginUiEffect>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val uiEffect: SharedFlow<LoginUiEffect> = _uiEffect.asSharedFlow()

    fun onEvent(event: LoginUiEvent) {
        when (event) {
            is LoginUiEvent.EmailChanged ->
                _uiState.update {
                    // Wrong credentials are about both fields: editing either one clears that error.
                    val passwordError = it.passwordError.takeUnless { e -> e == R.string.login_error_invalid_credentials }
                    it.copy(email = event.value, emailError = null, passwordError = passwordError)
                }
            is LoginUiEvent.PasswordChanged ->
                _uiState.update { it.copy(password = event.value, passwordError = null) }
            LoginUiEvent.Submit -> submit()
        }
    }

    private fun submit() {
        val current = _uiState.value
        if (current.isSubmitting) return

        val emailError = if (isValidEmail(current.email)) null else R.string.login_error_invalid_email
        val passwordError =
            if (current.password.isNotEmpty()) null else R.string.login_error_empty_password
        if (emailError != null || passwordError != null) {
            _uiState.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, emailError = null, passwordError = null) }
            loginUseCase(current.email, current.password)
                .onSuccess {
                    _uiState.update { it.copy(isSubmitting = false) }
                    _uiEffect.emit(LoginUiEffect.NavigateToHome)
                }
                .onFailure { throwable ->
                    // Wrong credentials stay under the password until a field is edited, instead of a
                    // snackbar that goes away far from the fields (UX-C-15). Other errors stay snackbars.
                    if ((throwable as? LoginException)?.error == LoginError.InvalidCredentials) {
                        _uiState.update {
                            it.copy(isSubmitting = false, passwordError = R.string.login_error_invalid_credentials)
                        }
                    } else {
                        _uiState.update { it.copy(isSubmitting = false) }
                        _uiEffect.emit(LoginUiEffect.ShowError(throwable.toMessageRes()))
                    }
                }
        }
    }

    private fun Throwable.toMessageRes(): Int = when ((this as? LoginException)?.error) {
        LoginError.InvalidCredentials -> R.string.login_error_invalid_credentials
        LoginError.Validation -> R.string.login_error_validation
        LoginError.Network -> R.string.login_error_network
        is LoginError.Unknown, null -> R.string.login_error_unknown
    }
}
