package com.qhathia.mobile.staff.feature.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qhathia.mobile.staff.core.ui.biometric.BiometricAuthenticator
import com.qhathia.mobile.staff.core.ui.biometric.BiometricAvailability
import com.qhathia.mobile.staff.core.ui.biometric.BiometricResult
import com.qhathia.mobile.staff.core.ui.biometric.rememberBiometricAuthenticator
import com.qhathia.mobile.staff.core.ui.component.QhathiaButton
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme
import com.qhathia.mobile.staff.feature.auth.R
import com.qhathia.mobile.staff.feature.auth.model.BiometricUnlockUiEffect
import com.qhathia.mobile.staff.feature.auth.model.BiometricUnlockUiEvent
import com.qhathia.mobile.staff.feature.auth.model.BiometricUnlockUiState
import kotlinx.coroutines.launch

/**
 * Stateful unlock gate. It opens the system biometric prompt as soon as it is shown, and again on
 * "Desbloquear con huella"; [onUnlocked] hands control back to the root once the merchant is
 * verified. Signing out needs no callback — clearing the session moves the root to login by itself.
 */
@Composable
fun BiometricUnlockScreen(
    onUnlocked: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BiometricUnlockViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnUnlocked by rememberUpdatedState(onUnlocked)
    val authenticator = rememberBiometricAuthenticator()
    val scope = rememberCoroutineScope()
    val promptTitle = stringResource(R.string.unlock_prompt_title)
    val promptSubtitle = stringResource(R.string.unlock_prompt_subtitle)
    val promptNegative = stringResource(R.string.unlock_prompt_negative)

    val requestUnlock: () -> Unit = {
        scope.launch {
            authenticator.promptOnce(promptTitle, promptSubtitle, promptNegative, viewModel::onEvent)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                BiometricUnlockUiEffect.Unlocked -> currentOnUnlocked()
            }
        }
    }
    LaunchedEffect(Unit) {
        authenticator.promptOnce(promptTitle, promptSubtitle, promptNegative, viewModel::onEvent)
    }

    BiometricUnlockContent(
        uiState = uiState,
        onUnlock = requestUnlock,
        onUsePassword = { viewModel.onEvent(BiometricUnlockUiEvent.UsePassword) },
        modifier = modifier,
    )
}

/** Checks the device first, so a gone biometric is reported instead of opening a dead prompt. */
private suspend fun BiometricAuthenticator.promptOnce(
    title: String,
    subtitle: String,
    negativeText: String,
    onEvent: (BiometricUnlockUiEvent) -> Unit,
) {
    when (availability()) {
        BiometricAvailability.NoneEnrolled,
        BiometricAvailability.NoHardware,
        -> onEvent(BiometricUnlockUiEvent.BiometricGone)
        BiometricAvailability.Unavailable,
        BiometricAvailability.Available,
        -> when (val result = authenticate(title, subtitle, negativeText)) {
            BiometricResult.Success -> onEvent(BiometricUnlockUiEvent.AuthenticationSucceeded)
            BiometricResult.Cancelled -> onEvent(BiometricUnlockUiEvent.AuthenticationCancelled)
            is BiometricResult.Error -> onEvent(BiometricUnlockUiEvent.AuthenticationFailed(result.message))
        }
    }
}

/** Stateless unlock UI: wordmark, greeting, and either the unlock action or the "gone" notice. */
@Composable
fun BiometricUnlockContent(
    uiState: BiometricUnlockUiState,
    onUnlock: () -> Unit,
    onUsePassword: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(Dimens.SpaceLg),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.login_brand),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Icon(
                imageVector = Icons.Rounded.Fingerprint,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(72.dp),
            )
            Text(
                text = uiState.userFirstName
                    ?.let { stringResource(R.string.unlock_greeting_named, it) }
                    ?: stringResource(R.string.unlock_greeting_anonymous),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(
                    if (uiState.isBiometricGone) R.string.unlock_biometric_gone else R.string.unlock_subtitle,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            uiState.lastError?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(Dimens.SpaceSm))
            if (uiState.isBiometricGone) {
                QhathiaButton(
                    text = stringResource(R.string.unlock_continue),
                    onClick = onUsePassword,
                    loading = uiState.isSigningOut,
                )
            } else {
                QhathiaButton(
                    text = stringResource(R.string.unlock_action),
                    onClick = onUnlock,
                    enabled = !uiState.isSigningOut,
                )
                TextButton(onClick = onUsePassword, enabled = !uiState.isSigningOut) {
                    Text(
                        text = stringResource(R.string.unlock_use_password),
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
    }
}

@Preview(name = "Biometric unlock — light", showBackground = true)
@Preview(name = "Biometric unlock — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun BiometricUnlockContentPreview() {
    QhathiaTheme {
        BiometricUnlockContent(
            uiState = BiometricUnlockUiState(userFirstName = "Carla"),
            onUnlock = {},
            onUsePassword = {},
        )
    }
}

@Preview(name = "Biometric unlock, biometric gone — light", showBackground = true)
@Preview(name = "Biometric unlock, biometric gone — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun BiometricUnlockGonePreview() {
    QhathiaTheme {
        BiometricUnlockContent(
            uiState = BiometricUnlockUiState(userFirstName = "Carla", isBiometricGone = true),
            onUnlock = {},
            onUsePassword = {},
        )
    }
}
