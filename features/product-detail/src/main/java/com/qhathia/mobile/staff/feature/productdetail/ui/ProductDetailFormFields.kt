package com.qhathia.mobile.staff.feature.productdetail.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.staff.core.domain.model.Category
import com.qhathia.mobile.staff.core.ui.component.QhathiaTextField
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.feature.productdetail.R
import kotlinx.collections.immutable.ImmutableList

// These field primitives intentionally mirror the ones in :features:product-register rather than
// importing them: feature modules must not depend on each other, and :core:ui is deliberately free
// of domain types, so CategorySelector (which takes Category) cannot live there either.

/** Section label shown above each form field, matching the DS form style. */
@Composable
internal fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier,
    )
}

/** A labeled field block: a [FieldLabel] above the supplied [field]. */
@Composable
internal fun LabeledField(
    label: String,
    modifier: Modifier = Modifier,
    field: @Composable () -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
        FieldLabel(label)
        field()
    }
}

@Composable
internal fun DetailNameField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    @StringRes errorRes: Int?,
    modifier: Modifier = Modifier,
) {
    LabeledField(label = stringResource(R.string.detail_name_label), modifier = modifier) {
        QhathiaTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = stringResource(R.string.detail_name_placeholder),
            enabled = enabled,
            isError = errorRes != null,
            errorText = errorRes?.let { stringResource(it) },
        )
    }
}

@Composable
internal fun DetailDescriptionField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    onOptimizeWithAi: () -> Unit,
    isAiLoading: Boolean,
    isAiActionEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            FieldLabel(
                text = stringResource(R.string.detail_description_label),
                modifier = Modifier.weight(1f),
            )
            OptimizeWithAiButton(
                enabled = isAiActionEnabled,
                isLoading = isAiLoading,
                onClick = onOptimizeWithAi,
            )
        }
        QhathiaTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = stringResource(R.string.detail_description_placeholder),
            enabled = enabled,
            singleLine = false,
            minLines = 4,
        )
    }
}

/**
 * "Optimizar con IA" pill next to the description label. The detail screen only ever optimizes an
 * existing description — drafting from a photo belongs to registration — so the label is fixed.
 */
@Composable
private fun OptimizeWithAiButton(
    enabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, primary),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = primary),
        contentPadding = PaddingValues(horizontal = Dimens.SpaceSm, vertical = 0.dp),
        modifier = modifier.height(Dimens.SpaceLg),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = primary,
                strokeWidth = 1.5.dp,
                modifier = Modifier.size(12.dp),
            )
        } else {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(12.dp),
            )
        }
        Spacer(Modifier.width(Dimens.SpaceXs))
        Text(
            text = stringResource(R.string.detail_description_ai),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
internal fun DetailPriceField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    @StringRes errorRes: Int?,
    modifier: Modifier = Modifier,
) {
    LabeledField(label = stringResource(R.string.detail_price_label), modifier = modifier) {
        QhathiaTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = stringResource(R.string.detail_price_placeholder),
            enabled = enabled,
            isError = errorRes != null,
            errorText = errorRes?.let { stringResource(it) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            visualTransformation = PrefixTransformation("${stringResource(R.string.detail_price_prefix)} "),
        )
    }
}

@Composable
internal fun DetailQuantityField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    @StringRes errorRes: Int?,
    modifier: Modifier = Modifier,
    isConsult: Boolean = false,
) {
    LabeledField(label = stringResource(R.string.detail_quantity_label), modifier = modifier) {
        QhathiaTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = stringResource(
                if (isConsult) R.string.detail_quantity_consult_placeholder else R.string.detail_quantity_placeholder,
            ),
            helperText = if (isConsult) stringResource(R.string.detail_quantity_consult_hint) else null,
            enabled = enabled,
            isError = errorRes != null,
            errorText = errorRes?.let { stringResource(it) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
    }
}

@Composable
internal fun DetailTagsField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    LabeledField(label = stringResource(R.string.detail_tags_label), modifier = modifier) {
        QhathiaTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = stringResource(R.string.detail_tags_placeholder),
            enabled = enabled,
        )
    }
}

/**
 * Single-select category picker (required): wrapping pill chips, with loading and retry states. The
 * selected chip is filled with the brand accent and shows a check; the rest are outlined.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CategorySelector(
    categories: ImmutableList<Category>,
    selectedCategoryId: String?,
    isLoading: Boolean,
    isError: Boolean,
    onSelect: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    Box(modifier = modifier) {
        when {
            isLoading -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.SpaceSm),
                contentAlignment = Alignment.CenterStart,
            ) {
                CircularProgressIndicator()
            }
            isError -> TextButton(onClick = onRetry) {
                Text(stringResource(R.string.detail_retry))
            }
            else -> FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
            ) {
                categories.forEach { category ->
                    val selected = category.id == selectedCategoryId
                    FilterChip(
                        selected = selected,
                        onClick = { onSelect(category.id) },
                        enabled = enabled,
                        shape = CircleShape,
                        label = { Text(category.name) },
                        leadingIcon = if (selected) {
                            { Icon(Icons.Filled.Check, contentDescription = null) }
                        } else {
                            null
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = colors.surface,
                            labelColor = colors.onSurface,
                            selectedContainerColor = colors.secondary,
                            selectedLabelColor = colors.onSecondary,
                            selectedLeadingIconColor = colors.onSecondary,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = enabled,
                            selected = selected,
                            borderColor = colors.secondary,
                            selectedBorderColor = colors.secondary,
                        ),
                    )
                }
            }
        }
    }
}

/**
 * Prepends a fixed [prefix] (e.g. the `S/` currency symbol) to the field's text for display. When
 * the field is empty the prefix is omitted so it does not render under the placeholder.
 */
private class PrefixTransformation(private val prefix: String) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        if (text.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }
        val transformed = AnnotatedString(prefix) + text
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = offset + prefix.length
            override fun transformedToOriginal(offset: Int): Int =
                (offset - prefix.length).coerceAtLeast(0)
        }
        return TransformedText(transformed, mapping)
    }
}
