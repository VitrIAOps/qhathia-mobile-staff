package com.qhathia.mobile.staff.feature.profile.security

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qhathia.mobile.staff.core.ui.biometric.BiometricAuthenticator
import com.qhathia.mobile.staff.core.ui.biometric.BiometricAvailability
import com.qhathia.mobile.staff.core.ui.biometric.BiometricResult
import com.qhathia.mobile.staff.core.ui.biometric.rememberBiometricAuthenticator
import com.qhathia.mobile.staff.feature.profile.R

/**
 * Stateful Seguridad. It owns the device-facing work the ViewModel must not touch: reading the
 * biometric capability, running the system prompt and opening the enrollment settings.
 *
 * [passwordChanged] is the result handed back by Cambiar contraseña; it is reported once and then
 * cleared through [onPasswordChangedConsumed].
 */
@Composable
fun SecurityScreen(
    passwordChanged: Boolean,
    onPasswordChangedConsumed: () -> Unit,
    onNavigateToChangePassword: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SecurityViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val authenticator = rememberBiometricAuthenticator()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    val currentOnNavigateToChangePassword by rememberUpdatedState(onNavigateToChangePassword)
    val promptTitle = resources.getString(R.string.biometric_prompt_title)
    val promptSubtitle = resources.getString(R.string.biometric_prompt_subtitle)
    val promptNegative = resources.getString(R.string.biometric_prompt_negative)

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                SecurityUiEffect.PromptBiometric -> {
                    val result = authenticator.authenticate(promptTitle, promptSubtitle, promptNegative)
                    viewModel.onEvent(SecurityUiEvent.BiometricPromptFinished(result == BiometricResult.Success))
                    if (result is BiometricResult.Error) {
                        snackbarHostState.showSnackbar(resources.getString(R.string.biometric_error))
                    }
                }
                SecurityUiEffect.OpenEnrollmentSettings -> authenticator.openEnrollmentSettings()
                SecurityUiEffect.NavigateToChangePassword -> currentOnNavigateToChangePassword()
                is SecurityUiEffect.ShowMessage ->
                    snackbarHostState.showSnackbar(resources.getString(effect.messageRes))
            }
        }
    }
    LaunchedEffect(passwordChanged) {
        if (passwordChanged) {
            viewModel.onEvent(SecurityUiEvent.PasswordChanged)
            onPasswordChangedConsumed()
        }
    }
    LifecycleResumeEffect(authenticator) {
        viewModel.onEvent(SecurityUiEvent.Resumed(authenticator.deviceStatus()))
        onPauseOrDispose { }
    }

    Box(modifier = modifier.fillMaxSize()) {
        SecurityContent(
            uiState = uiState,
            onEvent = viewModel::onEvent,
            onBiometricToggle = { enabled ->
                viewModel.onEvent(SecurityUiEvent.BiometricToggled(enabled, authenticator.deviceStatus()))
            },
            onBack = onBack,
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
        )
    }
}

private fun BiometricAuthenticator.deviceStatus(): DeviceBiometricStatus = when (availability()) {
    BiometricAvailability.Available -> DeviceBiometricStatus.AVAILABLE
    BiometricAvailability.NoneEnrolled -> DeviceBiometricStatus.NONE_ENROLLED
    BiometricAvailability.NoHardware -> DeviceBiometricStatus.NO_HARDWARE
    BiometricAvailability.Unavailable -> DeviceBiometricStatus.UNAVAILABLE
}
