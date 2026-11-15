package com.qhathia.mobile.staff.feature.orderrequests.ui

import com.qhathia.mobile.staff.feature.orderrequests.model.SubmitError
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.border
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme
import com.qhathia.mobile.staff.feature.orderrequests.R
import com.qhathia.mobile.staff.feature.orderrequests.model.RejectionReason
import com.qhathia.mobile.staff.feature.orderrequests.model.RejectionUiState

/**
 * Second step of the detail dialog: the motive behind a rejection.
 *
 * The backend stores `reason` as free text, so what leaves this screen is the chosen option's own
 * label — or, for [RejectionReason.OTHER], whatever the merchant typed. Resolving that text here
 * rather than in the ViewModel keeps the localized strings on the UI side.
 */
@Composable
internal fun OrderRejectForm(
    state: RejectionUiState,
    isSubmitting: Boolean,
    onSelectReason: (RejectionReason) -> Unit,
    onOtherReasonChange: (String) -> Unit,
    onCancel: () -> Unit,
    onConfirm: (String) -> Unit,
    modifier: Modifier = Modifier,
    submitError: SubmitError? = null,
) {
    val colors = MaterialTheme.colorScheme
    val labels = RejectionReason.entries.associateWith { stringResource(it.labelRes) }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXl),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
        ) {
            Text(
                text = stringResource(R.string.order_reject_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface,
            )
            Text(
                text = state.orderCode?.let {
                    stringResource(R.string.order_reject_subtitle, it)
                } ?: stringResource(R.string.order_reject_subtitle_no_code),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
        ) {
            RejectionReason.entries.forEach { reason ->
                ReasonRow(
                    selected = state.selectedReason == reason,
                    onSelect = { onSelectReason(reason) },
                    label = labels.getValue(reason),
                )
            }
            // "Otro motivo" gets its own full-width field under the options, focused with the
            // keyboard up: inside the row it was a one-line blank that cut the reason.
            if (state.selectedReason == RejectionReason.OTHER) {
                OtherReasonField(
                    value = state.otherReason,
                    onValueChange = onOtherReasonChange,
                    modifier = Modifier.padding(horizontal = Dimens.SpaceMd),
                )
            }
        }

        submitError?.let { DialogSubmitError(it) }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceLg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = onCancel,
                enabled = !isSubmitting,
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = stringResource(R.string.order_action_cancel),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.primary,
                )
            }
            Button(
                onClick = {
                    val reason = if (state.selectedReason == RejectionReason.OTHER) {
                        state.otherReason.trim()
                    } else {
                        state.selectedReason?.let { labels.getValue(it) }
                    }
                    if (reason != null) onConfirm(reason)
                },
                enabled = state.canSubmit && !isSubmitting,
                shape = RoundedCornerShape(Dimens.FieldRadius),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary,
                    contentColor = colors.onPrimary,
                ),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = Dimens.ButtonHeight),
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        color = colors.onPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp),
                    )
                } else {
                    Text(
                        text = stringResource(R.string.order_action_confirm_rejection),
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

/**
 * One radio option. The whole row is the target — `selectable` with a null-click radio is the
 * accessible pairing, and it gives the 48dp touch height the 34dp DS row would not have.
 */
@Composable
private fun ReasonRow(
    selected: Boolean,
    onSelect: () -> Unit,
    label: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onSelect,
                role = Role.RadioButton,
            )
            .padding(horizontal = Dimens.SpaceMd, vertical = Dimens.SpaceSm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
    ) {
        // Left at its natural size: the control draws at a required 20dp, so pinning it to the
        // DS's 16dp box would shrink the layout slot without shrinking the glyph.
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.secondary,
                unselectedColor = MaterialTheme.colorScheme.outlineVariant,
            ),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** The typed motive behind "Otro motivo": full width, up to three lines, focused on appearing. */
@Composable
private fun OtherReasonField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        minLines = 2,
        maxLines = OTHER_REASON_MAX_LINES,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
        textStyle = LocalTextStyle.current.merge(
            MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
        ),
        cursorBrush = SolidColor(colors.primary),
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .border(1.dp, colors.primary, RoundedCornerShape(Dimens.FieldRadius))
                    .padding(horizontal = Dimens.SpaceMd, vertical = Dimens.SpaceSm),
                contentAlignment = Alignment.TopStart,
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = stringResource(R.string.order_reject_other_placeholder),
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onSurfaceVariant,
                    )
                }
                innerTextField()
            }
        },
    )
}

private const val OTHER_REASON_MAX_LINES = 3

// ---- Previews ----

@Preview(name = "Reject form — light", showBackground = true)
@Preview(name = "Reject form — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun OrderRejectFormPreview() {
    QhathiaTheme {
        OrderRejectForm(
            state = RejectionUiState(
                orderCode = "458795",
                selectedReason = RejectionReason.INSUFFICIENT_QUANTITY,
            ),
            isSubmitting = false,
            onSelectReason = {},
            onOtherReasonChange = {},
            onCancel = {},
            onConfirm = {},
            modifier = Modifier.padding(Dimens.SpaceLg),
        )
    }
}

@Preview(name = "Reject form — other reason", showBackground = true)
@Composable
private fun OrderRejectFormOtherPreview() {
    QhathiaTheme {
        OrderRejectForm(
            state = RejectionUiState(
                orderCode = "458795",
                selectedReason = RejectionReason.OTHER,
                otherReason = "El cliente pidió otro modelo",
            ),
            isSubmitting = false,
            onSelectReason = {},
            onOtherReasonChange = {},
            onCancel = {},
            onConfirm = {},
            modifier = Modifier.padding(Dimens.SpaceLg),
        )
    }
}
