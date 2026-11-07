package com.qhathia.mobile.staff.feature.profile.security

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qhathia.mobile.staff.core.ui.component.QhathiaButton
import com.qhathia.mobile.staff.core.ui.component.QhathiaDiscardChangesDialog
import com.qhathia.mobile.staff.core.ui.component.QhathiaTextField
import com.qhathia.mobile.staff.core.ui.component.settings.QhathiaGradientTopBar
import com.qhathia.mobile.staff.core.ui.component.settings.QhathiaSettingsSection
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme
import com.qhathia.mobile.staff.feature.profile.R

/**
 * Stateful Cambiar contraseña; [onPasswordChanged] returns to Seguridad with the result. Both backs
 * go through the ViewModel, so typed passwords ask before leaving.
 */
@Composable
fun ChangePasswordScreen(
    onPasswordChanged: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChangePasswordViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnPasswordChanged by rememberUpdatedState(onPasswordChanged)
    val currentOnBack by rememberUpdatedState(onBack)

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                ChangePasswordUiEffect.PasswordChanged -> currentOnPasswordChanged()
                ChangePasswordUiEffect.NavigateBack -> currentOnBack()
            }
        }
    }
    BackHandler(enabled = uiState.isDirty || uiState.isSubmitting) {
        viewModel.onEvent(ChangePasswordUiEvent.BackRequested)
    }

    ChangePasswordContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBack = { viewModel.onEvent(ChangePasswordUiEvent.BackRequested) },
        modifier = modifier,
    )
}

/**
 * Stateless Cambiar contraseña. There is no Figma node for it, so it is assembled from the DS text
 * field, settings card and primary button.
 */
@Composable
fun ChangePasswordContent(
    uiState: ChangePasswordUiState,
    onEvent: (ChangePasswordUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        QhathiaGradientTopBar(title = stringResource(R.string.change_password_title), onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(Dimens.SpaceMd),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceLg),
        ) {
            QhathiaSettingsSection(title = null) {
                Column(
                    modifier = Modifier.padding(vertical = Dimens.SpaceMd),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
                ) {
                    LabelledPasswordField(
                        label = stringResource(R.string.change_password_current),
                        value = uiState.currentPassword,
                        onValueChange = { onEvent(ChangePasswordUiEvent.CurrentPasswordChanged(it)) },
                        errorText = uiState.currentPasswordErrorRes?.let { stringResource(it) },
                        enabled = !uiState.isSubmitting,
                    )
                    LabelledPasswordField(
                        label = stringResource(R.string.change_password_new),
                        value = uiState.newPassword,
                        onValueChange = { onEvent(ChangePasswordUiEvent.NewPasswordChanged(it)) },
                        errorText = uiState.newPasswordErrorRes?.let { stringResource(it) },
                        helperText = stringResource(R.string.change_password_hint),
                        enabled = !uiState.isSubmitting,
                    )
                    LabelledPasswordField(
                        label = stringResource(R.string.change_password_confirm),
                        value = uiState.confirmation,
                        onValueChange = { onEvent(ChangePasswordUiEvent.ConfirmationChanged(it)) },
                        errorText = if (uiState.isConfirmationMismatch) {
                            stringResource(R.string.change_password_error_mismatch)
                        } else {
                            null
                        },
                        enabled = !uiState.isSubmitting,
                        imeAction = ImeAction.Done,
                        onImeAction = { if (uiState.canSubmit) onEvent(ChangePasswordUiEvent.Submit) },
                    )
                }
            }
            uiState.bannerErrorRes?.let { messageRes ->
                Text(
                    text = stringResource(messageRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            QhathiaButton(
                text = stringResource(R.string.change_password_save),
                onClick = { onEvent(ChangePasswordUiEvent.Submit) },
                enabled = uiState.canSubmit,
                loading = uiState.isSubmitting,
            )
        }
    }
    if (uiState.isDiscardDialogVisible) {
        QhathiaDiscardChangesDialog(
            onConfirm = { onEvent(ChangePasswordUiEvent.DiscardConfirmed) },
            onDismiss = { onEvent(ChangePasswordUiEvent.DiscardDismissed) },
        )
    }
}

@Composable
private fun LabelledPasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    errorText: String?,
    enabled: Boolean,
    helperText: String? = null,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: (() -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        QhathiaTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = label,
            isPassword = true,
            enabled = enabled,
            isError = errorText != null,
            errorText = errorText,
            helperText = helperText,
            keyboardOptions = KeyboardOptions(imeAction = imeAction),
            keyboardActions = onImeAction?.let { action -> KeyboardActions(onDone = { action() }) }
                ?: KeyboardActions.Default,
        )
    }
}

@Preview(name = "Cambiar contraseña — light", showBackground = true)
@Preview(name = "Cambiar contraseña — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun ChangePasswordContentPreview() {
    QhathiaTheme {
        ChangePasswordContent(
            uiState = ChangePasswordUiState(
                currentPassword = "MiClaveVieja1",
                newPassword = "MiClaveNueva2",
                confirmation = "MiClaveNueva",
                currentPasswordErrorRes = R.string.change_password_error_wrong_current,
            ),
            onEvent = {},
            onBack = {},
        )
    }
}

@Preview(name = "Cambiar contraseña, submitting — light", showBackground = true)
@Composable
private fun ChangePasswordSubmittingPreview() {
    QhathiaTheme {
        ChangePasswordContent(
            uiState = ChangePasswordUiState(
                currentPassword = "MiClaveVieja1",
                newPassword = "MiClaveNueva2",
                confirmation = "MiClaveNueva2",
                isSubmitting = true,
            ),
            onEvent = {},
            onBack = {},
        )
    }
}
