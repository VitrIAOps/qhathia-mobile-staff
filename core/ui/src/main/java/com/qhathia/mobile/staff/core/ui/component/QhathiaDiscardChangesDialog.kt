package com.qhathia.mobile.staff.core.ui.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.qhathia.mobile.staff.core.ui.R
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme

/**
 * "¿Descartar cambios?", asked when leaving a form with unsaved changes. "Descartar" ([onConfirm])
 * leaves and is drawn in the error colour, so it never looks like the safe "Seguir editando"
 * (UX-C-14); that one and dismissing ([onDismiss]) stay with the typing intact. [message] replaces
 * the default text when the form knows more (e.g. part of a save already failed).
 */
@Composable
fun QhathiaDiscardChangesDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    message: String? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.discard_changes_title)) },
        text = { Text(message ?: stringResource(R.string.discard_changes_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.discard_changes_confirm),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.discard_changes_dismiss))
            }
        },
    )
}

@Preview(name = "Discard changes", showBackground = true)
@Preview(name = "Discard changes dark", showBackground = true, uiMode = 0x20)
@Composable
private fun QhathiaDiscardChangesDialogPreview() {
    QhathiaTheme {
        QhathiaDiscardChangesDialog(onConfirm = {}, onDismiss = {})
    }
}
