package com.qhathia.mobile.staff.feature.orderrequests.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.qhathia.mobile.staff.feature.orderrequests.model.SubmitError

/**
 * Why the dialog's own action did not go through, in red inside the dialog: the screen's snackbar
 * would be drawn under the dialog's scrim. Announced politely when it appears.
 */
@Composable
internal fun DialogSubmitError(error: SubmitError, modifier: Modifier = Modifier) {
    val text = if (error.arg != null) {
        stringResource(error.messageRes, error.arg)
    } else {
        stringResource(error.messageRes)
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
    )
}
