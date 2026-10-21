package com.qhathia.mobile.staff.feature.devicelinking.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.feature.devicelinking.R

/**
 * Sets, changes or removes a tablet's exit PIN. The PIN is typed twice, masked, and never shown back
 * — the backend only says whether one is set. "Quitar PIN" appears when one is set and asks again
 * before sending, since without a PIN anyone can leave kiosk mode.
 */
@Composable
internal fun ExitPinDialog(
    state: ExitPinDialogState,
    hasPin: Boolean,
    onEvent: (TabletSettingsUiEvent) -> Unit,
) {
    if (state.isConfirmingRemoval) {
        RemovePinConfirmation(isSaving = state.isSaving, onEvent = onEvent)
        return
    }
    AlertDialog(
        onDismissRequest = { onEvent(TabletSettingsUiEvent.DismissPinDialog) },
        title = { Text(stringResource(R.string.tablet_pin_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
                Text(
                    text = stringResource(R.string.tablet_pin_dialog_body),
                    style = MaterialTheme.typography.bodyMedium,
                )
                PinField(
                    value = state.pin,
                    label = stringResource(R.string.tablet_pin_new),
                    enabled = !state.isSaving,
                    imeAction = ImeAction.Next,
                    onValueChange = { onEvent(TabletSettingsUiEvent.PinChanged(it)) },
                )
                PinField(
                    value = state.confirmation,
                    label = stringResource(R.string.tablet_pin_repeat),
                    enabled = !state.isSaving,
                    imeAction = ImeAction.Done,
                    isError = state.isMismatch,
                    supportingText = if (state.isMismatch) stringResource(R.string.tablet_pin_mismatch) else null,
                    onValueChange = { onEvent(TabletSettingsUiEvent.PinConfirmationChanged(it)) },
                )
                state.errorRes?.let {
                    Text(
                        text = stringResource(it),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (hasPin) {
                    TextButton(
                        onClick = { onEvent(TabletSettingsUiEvent.RemovePinRequested) },
                        enabled = !state.isSaving,
                    ) {
                        Text(stringResource(R.string.tablet_pin_remove), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onEvent(TabletSettingsUiEvent.SavePin) }, enabled = state.canSave) {
                if (state.isSaving) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    Text(stringResource(R.string.tablet_pin_save))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { onEvent(TabletSettingsUiEvent.DismissPinDialog) }, enabled = !state.isSaving) {
                Text(stringResource(R.string.tablet_pin_cancel))
            }
        },
    )
}

@Composable
private fun PinField(
    value: String,
    label: String,
    enabled: Boolean,
    imeAction: ImeAction,
    onValueChange: (String) -> Unit,
    isError: Boolean = false,
    supportingText: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        isError = isError,
        supportingText = supportingText?.let { { Text(it) } },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = imeAction),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun RemovePinConfirmation(
    isSaving: Boolean,
    onEvent: (TabletSettingsUiEvent) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onEvent(TabletSettingsUiEvent.RemovePinCancelled) },
        title = { Text(stringResource(R.string.tablet_pin_remove_title)) },
        text = { Text(stringResource(R.string.tablet_pin_remove_message)) },
        confirmButton = {
            TextButton(onClick = { onEvent(TabletSettingsUiEvent.RemovePinConfirmed) }, enabled = !isSaving) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.error,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp),
                    )
                } else {
                    Text(stringResource(R.string.tablet_pin_remove_confirm), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { onEvent(TabletSettingsUiEvent.RemovePinCancelled) }, enabled = !isSaving) {
                Text(stringResource(R.string.tablet_pin_cancel))
            }
        },
    )
}
