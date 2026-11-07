package com.qhathia.mobile.staff.feature.devicelinking.settings

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.qhathia.mobile.staff.core.ui.component.QhathiaTextField
import com.qhathia.mobile.staff.feature.devicelinking.R

/** Renames the tablet: the current name prefilled, at most 40 characters, Guardar and Cancelar. */
@Composable
internal fun RenameTabletDialog(
    state: RenameDialogState,
    onEvent: (TabletSettingsUiEvent) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onEvent(TabletSettingsUiEvent.RenameDismissed) },
        title = { Text(stringResource(R.string.tablet_settings_rename_title)) },
        text = {
            QhathiaTextField(
                value = state.value,
                onValueChange = { onEvent(TabletSettingsUiEvent.RenameChanged(it)) },
                placeholder = stringResource(R.string.device_pair_name_placeholder),
                enabled = !state.isSaving,
                isError = state.errorRes != null,
                errorText = state.errorRes?.let { stringResource(it) },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { if (state.canSave) onEvent(TabletSettingsUiEvent.RenameConfirmed) },
                ),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onEvent(TabletSettingsUiEvent.RenameConfirmed) },
                enabled = state.canSave,
            ) {
                Text(stringResource(R.string.tablet_settings_rename_save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onEvent(TabletSettingsUiEvent.RenameDismissed) },
                enabled = !state.isSaving,
            ) {
                Text(stringResource(R.string.tablet_settings_rename_cancel))
            }
        },
    )
}
