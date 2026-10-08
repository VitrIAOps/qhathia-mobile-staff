package com.qhathia.mobile.staff.core.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.qhathia.mobile.staff.core.ui.R
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * DS "Aplicar descuento" dialog: the original price struck through above the resulting price, a
 * Porcentaje / Monto selector, the value input (with quick percentage options in percentage mode)
 * and the Cancelar / Aplicar actions.
 *
 * Stateless and rule-free: the caller owns the editing state and tells the dialog what the
 * [resultingPrice] is, whether the value can be applied and which [errorText] to show. While
 * [isLoading] the inputs are locked and the dialog cannot be dismissed.
 */
@Composable
fun QhathiaDiscountDialog(
    originalPrice: Double,
    resultingPrice: Double,
    isPercentage: Boolean,
    input: String,
    quickPercentages: ImmutableList<Int>,
    isApplyEnabled: Boolean,
    onModeChange: (isPercentage: Boolean) -> Unit,
    onInputChange: (String) -> Unit,
    onQuickPercentage: (Int) -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    errorText: String? = null,
    isLoading: Boolean = false,
) {
    Dialog(onDismissRequest = { if (!isLoading) onDismiss() }) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Dimens.CardRadius),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(Dimens.SpaceLg),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.discount_dialog_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(Modifier.height(Dimens.SpaceLg))

                val originalPriceCd = stringResource(R.string.discount_original_price_cd, originalPrice)
                Text(
                    text = stringResource(R.string.discount_original_price_format, originalPrice),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textDecoration = TextDecoration.LineThrough,
                    modifier = Modifier.semantics { contentDescription = originalPriceCd },
                )
                val resultingPriceCd = stringResource(R.string.discount_resulting_price_cd, resultingPrice)
                Text(
                    text = stringResource(R.string.discount_price_format, resultingPrice),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { contentDescription = resultingPriceCd },
                )

                Spacer(Modifier.height(Dimens.SpaceMd))

                QhathiaSegmentedControl(
                    options = DiscountModes,
                    selected = isPercentage,
                    onSelect = { if (!isLoading) onModeChange(it) },
                    label = { percentage ->
                        stringResource(
                            if (percentage) R.string.discount_mode_percentage else R.string.discount_mode_amount,
                        )
                    },
                )

                Spacer(Modifier.height(Dimens.SpaceLg))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
                ) {
                    Text(
                        text = stringResource(
                            if (isPercentage) R.string.discount_percentage_label else R.string.discount_amount_label,
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    QhathiaTextField(
                        value = input,
                        onValueChange = onInputChange,
                        placeholder = stringResource(
                            if (isPercentage) {
                                R.string.discount_percentage_placeholder
                            } else {
                                R.string.discount_amount_placeholder
                            },
                        ),
                        enabled = !isLoading,
                        isError = errorText != null,
                        errorText = errorText,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (isPercentage) KeyboardType.Number else KeyboardType.Decimal,
                            imeAction = ImeAction.Done,
                        ),
                    )
                }

                if (isPercentage) {
                    Spacer(Modifier.height(Dimens.SpaceMd))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
                    ) {
                        quickPercentages.forEach { percentage ->
                            QuickPercentageOption(
                                percentage = percentage,
                                selected = input.trim() == percentage.toString(),
                                enabled = !isLoading,
                                onClick = { onQuickPercentage(percentage) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(Dimens.SpaceXl))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isLoading,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = Dimens.ButtonHeight),
                    ) {
                        Text(stringResource(R.string.discount_cancel))
                    }
                    QhathiaButton(
                        text = stringResource(R.string.discount_apply),
                        onClick = onApply,
                        enabled = isApplyEnabled,
                        loading = isLoading,
                        modifier = Modifier.weight(1.4f),
                    )
                }
            }
        }
    }
}

/** `true` is percentage mode; the order is the order the DS shows the two segments in. */
private val DiscountModes = persistentListOf(true, false)

@Composable
private fun QuickPercentageOption(
    percentage: Int,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(Dimens.FieldRadius),
        border = BorderStroke(1.dp, if (selected) colors.primary else colors.outline),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) colors.primaryContainer else colors.surface,
            contentColor = if (selected) colors.onPrimaryContainer else colors.onSurface,
        ),
        modifier = modifier.heightIn(min = Dimens.FieldHeight),
    ) {
        Text(
            text = stringResource(R.string.discount_quick_option, percentage),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private val PreviewQuickPercentages = persistentListOf(10, 20, 50)

@Preview(name = "Discount dialog — percentage", showBackground = true)
@Preview(name = "Discount dialog — percentage dark", showBackground = true, uiMode = 0x20)
@Composable
private fun QhathiaDiscountDialogPercentagePreview() {
    QhathiaTheme {
        QhathiaDiscountDialog(
            originalPrice = 189.0,
            resultingPrice = 151.2,
            isPercentage = true,
            input = "20",
            quickPercentages = PreviewQuickPercentages,
            isApplyEnabled = true,
            onModeChange = {},
            onInputChange = {},
            onQuickPercentage = {},
            onApply = {},
            onDismiss = {},
        )
    }
}

@Preview(name = "Discount dialog — amount, empty", showBackground = true)
@Composable
private fun QhathiaDiscountDialogAmountPreview() {
    QhathiaTheme {
        QhathiaDiscountDialog(
            originalPrice = 189.0,
            resultingPrice = 189.0,
            isPercentage = false,
            input = "",
            quickPercentages = PreviewQuickPercentages,
            isApplyEnabled = false,
            onModeChange = {},
            onInputChange = {},
            onQuickPercentage = {},
            onApply = {},
            onDismiss = {},
        )
    }
}

@Preview(name = "Discount dialog — error", showBackground = true)
@Preview(name = "Discount dialog — error dark", showBackground = true, uiMode = 0x20)
@Composable
private fun QhathiaDiscountDialogErrorPreview() {
    QhathiaTheme {
        QhathiaDiscountDialog(
            originalPrice = 189.0,
            resultingPrice = 189.0,
            isPercentage = false,
            input = "250",
            quickPercentages = PreviewQuickPercentages,
            isApplyEnabled = false,
            onModeChange = {},
            onInputChange = {},
            onQuickPercentage = {},
            onApply = {},
            onDismiss = {},
            errorText = stringResource(R.string.discount_error_amount_above_price),
        )
    }
}

@Preview(name = "Discount dialog — saving", showBackground = true)
@Composable
private fun QhathiaDiscountDialogLoadingPreview() {
    QhathiaTheme {
        QhathiaDiscountDialog(
            originalPrice = 189.0,
            resultingPrice = 94.5,
            isPercentage = true,
            input = "50",
            quickPercentages = PreviewQuickPercentages,
            isApplyEnabled = true,
            onModeChange = {},
            onInputChange = {},
            onQuickPercentage = {},
            onApply = {},
            onDismiss = {},
            isLoading = true,
        )
    }
}
