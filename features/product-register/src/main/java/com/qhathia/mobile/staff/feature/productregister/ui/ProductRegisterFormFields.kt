package com.qhathia.mobile.staff.feature.productregister.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import com.qhathia.mobile.staff.core.ui.component.QhathiaTextField
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.feature.productregister.R

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
internal fun RegisterNameField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    errorRes: Int?,
    modifier: Modifier = Modifier,
) {
    LabeledField(label = stringResource(R.string.register_name_label), modifier = modifier) {
        QhathiaTextField(
            label = stringResource(R.string.register_name_label),
            value = value,
            onValueChange = onValueChange,
            placeholder = stringResource(R.string.register_name_placeholder),
            enabled = enabled,
            isError = errorRes != null,
            errorText = errorRes?.let { stringResource(it) },
        )
    }
}

@Composable
internal fun RegisterDescriptionField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    onGenerateWithAi: () -> Unit,
    isOptimizeMode: Boolean,
    isAiLoading: Boolean,
    isAiActionEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FieldLabel(
                text = stringResource(R.string.register_description_label),
                modifier = Modifier.weight(1f),
            )
            GenerateWithAiButton(
                enabled = isAiActionEnabled,
                isLoading = isAiLoading,
                labelRes = if (isOptimizeMode) {
                    R.string.register_description_ai_optimize
                } else {
                    R.string.register_description_ai
                },
                onClick = onGenerateWithAi,
            )
        }
        QhathiaTextField(
            label = stringResource(R.string.register_description_label),
            value = value,
            onValueChange = onValueChange,
            placeholder = stringResource(R.string.register_description_placeholder),
            enabled = enabled,
            singleLine = false,
            minLines = 4,
        )
    }
}

/**
 * "Generar con IA" / "Optimizar con IA" pill shown next to the description label. Shows a small
 * spinner in place of the icon while an AI request is in flight.
 */
@Composable
private fun GenerateWithAiButton(
    enabled: Boolean,
    isLoading: Boolean,
    @androidx.annotation.StringRes labelRes: Int,
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
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
internal fun RegisterPriceField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    errorRes: Int?,
    modifier: Modifier = Modifier,
) {
    LabeledField(label = stringResource(R.string.register_price_label), modifier = modifier) {
        QhathiaTextField(
            label = stringResource(R.string.register_price_label),
            value = value,
            onValueChange = onValueChange,
            placeholder = stringResource(R.string.register_price_placeholder),
            enabled = enabled,
            isError = errorRes != null,
            errorText = errorRes?.let { stringResource(it) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            visualTransformation = PrefixTransformation("${stringResource(R.string.register_price_prefix)} "),
        )
    }
}

@Composable
internal fun RegisterQuantityField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    errorRes: Int?,
    modifier: Modifier = Modifier,
) {
    LabeledField(label = stringResource(R.string.register_quantity_label), modifier = modifier) {
        QhathiaTextField(
            label = stringResource(R.string.register_quantity_label),
            value = value,
            onValueChange = onValueChange,
            placeholder = stringResource(R.string.register_quantity_placeholder),
            enabled = enabled,
            isError = errorRes != null,
            errorText = errorRes?.let { stringResource(it) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )
    }
}

@Composable
internal fun RegisterTagsField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    LabeledField(label = stringResource(R.string.register_tags_label), modifier = modifier) {
        QhathiaTextField(
            label = stringResource(R.string.register_tags_label),
            value = value,
            onValueChange = onValueChange,
            placeholder = stringResource(R.string.register_tags_placeholder),
            enabled = enabled,
        )
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
