package com.qhathia.mobile.staff.feature.productinventory.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme
import com.qhathia.mobile.staff.feature.productinventory.R

/**
 * Confirms "Eliminar producto". It names the product, because the menu was opened from a list of
 * look-alike rows, and says plainly that there is no way back. While the request is in flight the
 * buttons are disabled and "Eliminar" shows progress; the ViewModel ignores dismissals meanwhile.
 */
@Composable
internal fun DeleteProductDialog(
    productName: String,
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.inventory_delete_title)) },
        text = { Text(stringResource(R.string.inventory_delete_message, productName)) },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isDeleting) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.error,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp),
                    )
                } else {
                    Text(
                        text = stringResource(R.string.inventory_delete_confirm),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isDeleting) {
                Text(stringResource(R.string.inventory_delete_cancel))
            }
        },
    )
}

@Preview(name = "Delete product — deleting", showBackground = true)
@Preview(name = "Delete product — deleting dark", showBackground = true, uiMode = 0x20)
@Composable
private fun DeleteProductDialogDeletingPreview() {
    QhathiaTheme {
        DeleteProductDialog(
            productName = "Taladro Percutor Bosch GSB 13 RE",
            isDeleting = true,
            onConfirm = {},
            onDismiss = {},
        )
    }
}
