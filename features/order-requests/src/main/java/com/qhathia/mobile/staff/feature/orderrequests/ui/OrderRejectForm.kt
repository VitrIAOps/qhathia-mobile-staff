package com.qhathia.mobile.staff.feature.orderrequests.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
                val selected = state.selectedReason == reason
                ReasonRow(
                    selected = selected,
                    onSelect = { onSelectReason(reason) },
                    label = labels.getValue(reason),
                ) {
                    // "Otro motivo: ______" — the blank in the design is the field itself, so it
                    // stays on the row and only accepts text once the option is picked.
                    if (reason == RejectionReason.OTHER) {
                        OtherReasonField(
                            value = state.otherReason,
                            enabled = selected,
                            onValueChange = onOtherReasonChange,
                            onFocus = { onSelectReason(RejectionReason.OTHER) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

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
                    .height(40.dp),
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
    trailing: @Composable RowScope.() -> Unit = {},
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
        trailing()
    }
}

/** The underlined blank of "Otro motivo: ______". */
@Composable
private fun OtherReasonField(
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    onFocus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = modifier) {
        BasicTextField(
            value = value,
            onValueChange = { if (enabled) onValueChange(it) else onFocus() },
            enabled = enabled,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            textStyle = LocalTextStyle.current.merge(
                MaterialTheme.typography.bodyLarge.copy(color = colors.onSurface),
            ),
            cursorBrush = SolidColor(colors.primary),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
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
        HorizontalDivider(
            color = if (enabled) colors.primary else colors.outlineVariant,
        )
    }
}

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
