package com.qhathia.mobile.staff.core.ui.component

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
import com.qhathia.mobile.staff.core.ui.R
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme

/**
 * Confirms "Eliminar producto", from the Inventario row menu and from the edit screen alike. It names
 * the product and says plainly that there is no way back (the backend soft-deletes and cannot
 * reactivate). While the request is in flight both buttons are disabled and "Eliminar" shows
 * progress; callers ignore dismissals meanwhile.
 *
 * [hasUnsavedChanges] adds that the edits on the form will be lost too. [pendingOrdersDetail] is the
 * server's explanation after a `409 PRODUCT_IN_PENDING_ORDERS`: it replaces the text, and confirming
 * becomes "Eliminar igual", which the caller sends with `force`.
 */
@Composable
fun QhathiaDeleteProductDialog(
    productName: String,
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    pendingOrdersDetail: String? = null,
    hasUnsavedChanges: Boolean = false,
) {
    val message = when {
        pendingOrdersDetail != null -> pendingOrdersDetail
        hasUnsavedChanges -> stringResource(R.string.delete_product_message_unsaved, productName)
        else -> stringResource(R.string.delete_product_message, productName)
    }
    val confirmLabel = stringResource(
        if (pendingOrdersDetail != null) R.string.delete_product_confirm_force else R.string.delete_product_confirm,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_product_title)) },
        text = { Text(message) },
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
                        text = confirmLabel,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isDeleting) {
                Text(stringResource(R.string.delete_product_cancel))
            }
        },
    )
}

@Preview(name = "Delete product", showBackground = true)
@Preview(name = "Delete product dark", showBackground = true, uiMode = 0x20)
@Composable
private fun QhathiaDeleteProductDialogPreview() {
    QhathiaTheme {
        QhathiaDeleteProductDialog(
            productName = "Taladro Percutor Bosch GSB 13 RE",
            isDeleting = false,
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@Preview(name = "Delete product — deleting", showBackground = true)
@Preview(name = "Delete product — deleting dark", showBackground = true, uiMode = 0x20)
@Composable
private fun QhathiaDeleteProductDialogDeletingPreview() {
    QhathiaTheme {
        QhathiaDeleteProductDialog(
            productName = "Taladro Percutor Bosch GSB 13 RE",
            isDeleting = true,
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@Preview(name = "Delete product — unsaved edits", showBackground = true)
@Composable
private fun QhathiaDeleteProductDialogUnsavedPreview() {
    QhathiaTheme {
        QhathiaDeleteProductDialog(
            productName = "Taladro Percutor Bosch GSB 13 RE",
            isDeleting = false,
            onConfirm = {},
            onDismiss = {},
            hasUnsavedChanges = true,
        )
    }
}

@Preview(name = "Delete product — pending orders", showBackground = true)
@Composable
private fun QhathiaDeleteProductDialogPendingOrdersPreview() {
    QhathiaTheme {
        QhathiaDeleteProductDialog(
            productName = "Taladro Percutor Bosch GSB 13 RE",
            isDeleting = false,
            onConfirm = {},
            onDismiss = {},
            pendingOrdersDetail = "Este producto está en 1 pedido pendiente (#400776). Confírmalo o recházalo antes, o elimínalo igual.",
        )
    }
}
