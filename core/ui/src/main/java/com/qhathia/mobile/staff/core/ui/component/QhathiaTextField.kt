package com.qhathia.mobile.staff.core.ui.component

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.staff.core.ui.theme.Dimens

/** Placeholders are a hint, not content: faded so an empty field never reads as a filled one. */
private const val PlaceholderAlpha = 0.5f

/**
 * Design-system text field: placeholder-only (no label), optional leading icon, 8dp radius,
 * with `Empty / Filled / Focused / Error / Disabled` border states from the Qhathia DS.
 * When [isPassword] is true the value is masked and a visibility toggle is shown.
 *
 * @param errorText shown (in the error color) when [isError]; otherwise [helperText] is shown.
 * @param keyboardActions what the keyboard's action key does (e.g. submit on Done).
 */
@Composable
fun QhathiaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    leadingIconDescription: String? = null,
    enabled: Boolean = true,
    isError: Boolean = false,
    helperText: String? = null,
    errorText: String? = null,
    isPassword: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    singleLine: Boolean = true,
    minLines: Int = 1,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    val colors = MaterialTheme.colorScheme
    val borderColor: Color = when {
        !enabled -> colors.outline.copy(alpha = 0.4f)
        isError -> colors.error
        isFocused -> colors.primary
        value.isNotEmpty() -> colors.outlineVariant
        else -> colors.outline
    }
    val contentAlpha = if (enabled) 1f else 0.4f

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = Dimens.FieldHeight)
                .background(colors.surface, RoundedCornerShape(Dimens.FieldRadius))
                .border(1.dp, borderColor, RoundedCornerShape(Dimens.FieldRadius))
                .padding(horizontal = Dimens.SpaceMd, vertical = if (singleLine) 0.dp else Dimens.SpaceSm),
            contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart,
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = singleLine,
                minLines = minLines,
                interactionSource = interactionSource,
                textStyle = LocalTextStyle.current.merge(
                    MaterialTheme.typography.bodyMedium.copy(
                        color = colors.onSurface.copy(alpha = contentAlpha),
                    ),
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(colors.primary),
                keyboardOptions = if (isPassword) {
                    keyboardOptions.copy(keyboardType = KeyboardType.Password)
                } else {
                    keyboardOptions
                },
                keyboardActions = keyboardActions,
                visualTransformation = if (isPassword && !passwordVisible) {
                    PasswordVisualTransformation()
                } else {
                    visualTransformation
                },
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (leadingIcon != null) {
                            Icon(
                                imageVector = leadingIcon,
                                contentDescription = leadingIconDescription,
                                tint = colors.onSurfaceVariant.copy(alpha = contentAlpha),
                                modifier = Modifier.padding(end = Dimens.SpaceSm),
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            if (value.isEmpty()) {
                                Text(
                                    text = placeholder,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.onSurfaceVariant.copy(alpha = PlaceholderAlpha * contentAlpha),
                                )
                            }
                            innerTextField()
                        }
                        if (isPassword) {
                            IconButton(
                                onClick = { passwordVisible = !passwordVisible },
                                enabled = enabled,
                            ) {
                                Icon(
                                    imageVector = if (passwordVisible) {
                                        Icons.Filled.VisibilityOff
                                    } else {
                                        Icons.Filled.Visibility
                                    },
                                    contentDescription = if (passwordVisible) {
                                        "Hide password"
                                    } else {
                                        "Show password"
                                    },
                                    tint = colors.onSurfaceVariant.copy(alpha = contentAlpha),
                                )
                            }
                        }
                    }
                },
            )
        }

        val supporting = if (isError) errorText else helperText
        if (!supporting.isNullOrEmpty()) {
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodySmall,
                color = if (isError) colors.error else colors.onSurfaceVariant,
            )
        }
    }
}
