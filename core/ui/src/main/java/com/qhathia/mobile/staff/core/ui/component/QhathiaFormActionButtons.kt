package com.qhathia.mobile.staff.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme

/** From this font scale the two buttons are stacked, the main action on top (S17-06). */
private const val StackFontScale = 1.5f

/** A label shrinks to this before it would break a word. */
private val MinLabelSize = 10.sp

/**
 * "Cancelar" and the form's main action ("Agregar", "Guardar"). Labels stay on one line and shrink
 * instead of breaking a word ("Cancela-r"); at large text the buttons are stacked.
 */
@Composable
fun QhathiaFormActionButtons(
    cancelLabel: String,
    submitLabel: String,
    isSubmitEnabled: Boolean,
    isSubmitting: Boolean,
    onCancel: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cancel: @Composable (Modifier) -> Unit = { m ->
        TextButton(onClick = onCancel, enabled = !isSubmitting, modifier = m.heightIn(min = Dimens.ButtonHeight)) {
            OneLineLabel(cancelLabel, MaterialTheme.typography.labelLarge)
        }
    }
    val submit: @Composable (Modifier) -> Unit = { m ->
        Button(
            onClick = onSubmit,
            enabled = isSubmitEnabled,
            shape = RoundedCornerShape(Dimens.ButtonRadius),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            modifier = m.heightIn(min = Dimens.ButtonHeight),
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.height(20.dp),
                )
            } else {
                OneLineLabel(submitLabel, MaterialTheme.typography.labelLarge)
            }
        }
    }
    if (LocalDensity.current.fontScale >= StackFontScale) {
        Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)) {
            submit(Modifier.fillMaxWidth())
            cancel(Modifier.fillMaxWidth())
        }
    } else {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            cancel(Modifier.weight(1f))
            submit(Modifier.weight(1.4f))
        }
    }
}

@Composable
private fun OneLineLabel(text: String, style: TextStyle) {
    Text(
        text = text,
        style = style,
        textAlign = TextAlign.Center,
        maxLines = 1,
        softWrap = false,
        autoSize = TextAutoSize.StepBased(minFontSize = MinLabelSize, maxFontSize = style.fontSize),
    )
}

@Preview(name = "Form actions — light", showBackground = true, widthDp = 360)
@Preview(name = "Form actions — dark", showBackground = true, uiMode = 0x20, widthDp = 360)
@Preview(name = "Form actions, font 2.0", showBackground = true, fontScale = 2f, widthDp = 360)
@Composable
private fun QhathiaFormActionButtonsPreview() {
    QhathiaTheme {
        QhathiaFormActionButtons(
            cancelLabel = "Cancelar",
            submitLabel = "Agregar",
            isSubmitEnabled = true,
            isSubmitting = false,
            onCancel = {},
            onSubmit = {},
        )
    }
}
