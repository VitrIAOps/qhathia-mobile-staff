package com.qhathia.mobile.staff.feature.devicelinking.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.qhathia.mobile.staff.core.ui.component.QhathiaButton
import com.qhathia.mobile.staff.core.ui.component.QhathiaTextField
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.feature.devicelinking.R

/**
 * Near-full-screen dialog to pair a terminal: a single code input and a submit button. Submit is
 * disabled while the code is blank or a pairing is in flight; [errorText] is shown inline on
 * failure. The dialog stays open on error so the merchant can correct the code and retry.
 */
@Composable
internal fun PairDeviceDialog(
    code: String,
    canSubmit: Boolean,
    isPairing: Boolean,
    errorText: String?,
    onCodeChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(Dimens.SpaceMd),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(modifier = Modifier.padding(Dimens.SpaceMd)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.device_pair_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.device_pair_close_cd),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                Spacer(Modifier.height(Dimens.SpaceLg))

                QhathiaTextField(
                    value = code,
                    onValueChange = onCodeChange,
                    placeholder = stringResource(R.string.device_pair_code_placeholder),
                    enabled = !isPairing,
                    isError = errorText != null,
                    errorText = errorText,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(Dimens.SpaceLg))

                QhathiaButton(
                    text = stringResource(R.string.device_pair_submit),
                    onClick = onSubmit,
                    enabled = canSubmit,
                    loading = isPairing,
                )
            }
        }
    }
}
