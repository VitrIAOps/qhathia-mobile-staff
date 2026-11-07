package com.qhathia.mobile.staff.feature.devicelinking.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.qhathia.mobile.staff.core.ui.component.QhathiaButton
import com.qhathia.mobile.staff.core.ui.component.QhathiaTextField
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.feature.devicelinking.R
import com.qhathia.mobile.staff.feature.devicelinking.model.PAIRING_CODE_LENGTH
import com.qhathia.mobile.staff.feature.devicelinking.model.PairingPhase

/**
 * Near-full-screen dialog to pair a tablet, in three steps ([phase]):
 * - entering the code and the tablet's name, with Vincular and Rechazar;
 * - waiting for the tablet to exchange the approved code (it cannot be dismissed);
 * - the tablet did not answer: undo the approval or keep waiting.
 *
 * [errorText] is shown inline in any step, and the dialog stays open so the merchant can act on it.
 */
@Composable
internal fun PairDeviceDialog(
    phase: PairingPhase,
    code: String,
    name: String,
    canSubmit: Boolean,
    isPairing: Boolean,
    isRejecting: Boolean,
    isUndoing: Boolean,
    errorText: String?,
    onCodeChange: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onReject: () -> Unit,
    onUndo: () -> Unit,
    onKeepWaiting: () -> Unit,
    onDismiss: () -> Unit,
) {
    val dismissable = phase == PairingPhase.EnteringCode
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = dismissable,
            dismissOnClickOutside = dismissable,
        ),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(Dimens.SpaceMd),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .padding(Dimens.SpaceMd)
                    .verticalScroll(rememberScrollState()),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.device_pair_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    if (dismissable) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.device_pair_close_cd),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(Dimens.SpaceLg))

                when (phase) {
                    PairingPhase.EnteringCode -> EnteringCodeBody(
                        code = code,
                        name = name,
                        canSubmit = canSubmit,
                        isPairing = isPairing,
                        isRejecting = isRejecting,
                        errorText = errorText,
                        onCodeChange = onCodeChange,
                        onNameChange = onNameChange,
                        onSubmit = onSubmit,
                        onReject = onReject,
                    )
                    is PairingPhase.WaitingForTablet -> WaitingBody()
                    is PairingPhase.TabletDidNotRespond -> NotRespondingBody(
                        isUndoing = isUndoing,
                        errorText = errorText,
                        onUndo = onUndo,
                        onKeepWaiting = onKeepWaiting,
                    )
                }
            }
        }
    }
}

@Composable
private fun EnteringCodeBody(
    code: String,
    name: String,
    canSubmit: Boolean,
    isPairing: Boolean,
    isRejecting: Boolean,
    errorText: String?,
    onCodeChange: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onReject: () -> Unit,
) {
    val busy = isPairing || isRejecting
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd)) {
        QhathiaTextField(
            value = code,
            onValueChange = onCodeChange,
            placeholder = stringResource(R.string.device_pair_code_placeholder),
            enabled = !busy,
            isError = errorText != null,
            errorText = errorText,
            helperText = stringResource(R.string.device_pair_code_helper, code.length, PAIRING_CODE_LENGTH),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                keyboardType = KeyboardType.Ascii,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { if (canSubmit) onSubmit() }),
            modifier = Modifier.fillMaxWidth(),
        )
        QhathiaTextField(
            value = name,
            onValueChange = onNameChange,
            placeholder = stringResource(R.string.device_pair_name_placeholder),
            enabled = !busy,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Dimens.SpaceSm))
        QhathiaButton(
            text = stringResource(R.string.device_pair_submit),
            onClick = onSubmit,
            enabled = canSubmit,
            loading = isPairing,
        )
        TextButton(
            onClick = onReject,
            enabled = canSubmit,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.device_pair_reject))
        }
    }
}

@Composable
private fun WaitingBody() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.SpaceXl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
    ) {
        CircularProgressIndicator()
        Text(
            text = stringResource(R.string.device_pair_waiting),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

@Composable
private fun NotRespondingBody(
    isUndoing: Boolean,
    errorText: String?,
    onUndo: () -> Unit,
    onKeepWaiting: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd)) {
        Text(
            text = stringResource(R.string.device_pair_not_responding),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        errorText?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        QhathiaButton(
            text = stringResource(R.string.device_pair_undo),
            onClick = onUndo,
            loading = isUndoing,
        )
        TextButton(
            onClick = onKeepWaiting,
            enabled = !isUndoing,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.device_pair_keep_waiting))
        }
    }
}
