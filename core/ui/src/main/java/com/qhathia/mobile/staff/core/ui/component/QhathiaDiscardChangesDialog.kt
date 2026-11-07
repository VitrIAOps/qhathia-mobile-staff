package com.qhathia.mobile.staff.core.ui.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.qhathia.mobile.staff.core.ui.R
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme

/**
 * "¿Descartar cambios?", asked when leaving a form with unsaved changes. "Descartar" ([onConfirm])
 * leaves; "Seguir editando" and dismissing ([onDismiss]) stay with the typing intact.
 */
@Composable
fun QhathiaDiscardChangesDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.discard_changes_title)) },
        text = { Text(stringResource(R.string.discard_changes_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.discard_changes_confirm))
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
