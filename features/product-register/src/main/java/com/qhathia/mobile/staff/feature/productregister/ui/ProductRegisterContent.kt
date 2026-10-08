package com.qhathia.mobile.staff.feature.productregister.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.staff.core.domain.model.Category
import com.qhathia.mobile.staff.core.domain.model.Discount
import com.qhathia.mobile.staff.core.domain.model.DiscountDraft
import com.qhathia.mobile.staff.core.domain.model.DiscountType
import com.qhathia.mobile.staff.core.domain.model.SelectedImage
import com.qhathia.mobile.staff.core.ui.component.QhathiaDiscountToggleRow
import com.qhathia.mobile.staff.core.ui.component.productpreview.QhathiaProductPreviewDialog
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme
import com.qhathia.mobile.staff.feature.productregister.R
import com.qhathia.mobile.staff.feature.productregister.model.ProductRegisterUiEvent
import com.qhathia.mobile.staff.feature.productregister.model.ProductRegisterUiState
import com.qhathia.mobile.staff.feature.productregister.model.toPreviewData
import kotlinx.collections.immutable.persistentListOf

/**
 * Stateless registration form — a pure function of [uiState]. A header over a gray background sits
 * above a white scrollable card holding the photos section, fields, category chips and the
 * cancel/submit actions, following the Figma mockup. A translucent loader overlays the form while
 * submitting, and the discard-confirmation dialog is shown when `showDiscardDialog` is set.
 */
@Composable
fun ProductRegisterContent(
    uiState: ProductRegisterUiState,
    onEvent: (ProductRegisterUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabled = !uiState.isSubmitting

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = Dimens.SpaceMd),
        ) {
            ProductRegisterHeader(onBack = { onEvent(ProductRegisterUiEvent.CancelRequested) })

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = Dimens.SpaceMd),
                shape = RoundedCornerShape(Dimens.SpaceMd),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                        .padding(Dimens.SpaceLg),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpaceLg),
                ) {
                    ImagePickerSection(
                        images = uiState.images,
                        onPick = { onEvent(ProductRegisterUiEvent.ImagesPicked(it)) },
                        onRemove = { onEvent(ProductRegisterUiEvent.RemoveImage(it)) },
                        enabled = enabled,
                    )
                    RegisterNameField(
                        value = uiState.name,
                        onValueChange = { onEvent(ProductRegisterUiEvent.NameChanged(it)) },
                        enabled = enabled,
                        errorRes = uiState.nameError,
                    )
                    RegisterDescriptionField(
                        value = uiState.description,
                        onValueChange = { onEvent(ProductRegisterUiEvent.DescriptionChanged(it)) },
                        enabled = enabled,
                        onGenerateWithAi = { onEvent(ProductRegisterUiEvent.GenerateDescriptionWithAi) },
                        isOptimizeMode = uiState.isOptimizeMode,
                        isAiLoading = uiState.isAiLoading,
                        isAiActionEnabled = uiState.isAiActionEnabled,
                    )
                    LabeledField(label = stringResource(R.string.register_category_label)) {
                        CategorySelector(
                            categories = uiState.categories,
                            selectedCategoryId = uiState.selectedCategoryId,
                            isLoading = uiState.isCategoriesLoading,
                            isError = uiState.isCategoriesError,
                            onSelect = { onEvent(ProductRegisterUiEvent.SelectCategory(it)) },
                            onRetry = { onEvent(ProductRegisterUiEvent.RetryCatalog) },
                            enabled = enabled,
                        )
                    }
                    RegisterPriceField(
                        value = uiState.price,
                        onValueChange = { onEvent(ProductRegisterUiEvent.PriceChanged(it)) },
                        enabled = enabled,
                        errorRes = uiState.priceError,
                    )
                    QhathiaDiscountToggleRow(
                        checked = uiState.discount != null || uiState.discountDraft != null,
                        onCheckedChange = { onEvent(ProductRegisterUiEvent.DiscountToggled(it)) },
                        enabled = uiState.isDiscountToggleEnabled,
                        summary = uiState.discount?.summaryText(),
                        finalPrice = uiState.discountedPrice,
                        onSummaryClick = { onEvent(ProductRegisterUiEvent.DiscountSummaryClicked) },
                    )
                    RegisterQuantityField(
                        value = uiState.quantity,
                        onValueChange = { onEvent(ProductRegisterUiEvent.QuantityChanged(it)) },
                        enabled = enabled,
                        errorRes = uiState.quantityError,
                    )
                    RegisterTagsField(
                        value = uiState.tags,
                        onValueChange = { onEvent(ProductRegisterUiEvent.TagsChanged(it)) },
                        enabled = enabled,
                    )
                    PreviewAction(
                        enabled = uiState.isPreviewEnabled,
                        // While submitting the whole form is locked; the hint is about missing data.
                        showHint = !uiState.isPreviewEnabled && enabled,
                        onClick = { onEvent(ProductRegisterUiEvent.PreviewRequested) },
                    )

                    Spacer(Modifier.height(Dimens.SpaceSm))

                    FormActions(
                        isSubmitEnabled = uiState.isSubmitEnabled,
                        isSubmitting = uiState.isSubmitting,
                        onCancel = { onEvent(ProductRegisterUiEvent.CancelRequested) },
                        onSubmit = { onEvent(ProductRegisterUiEvent.Submit) },
                    )
                }
            }
        }

        if (uiState.isSubmitting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        if (uiState.showDiscardDialog) {
            DiscardChangesDialog(
                onConfirm = { onEvent(ProductRegisterUiEvent.ConfirmDiscard) },
                onDismiss = { onEvent(ProductRegisterUiEvent.DismissDiscard) },
            )
        }

        uiState.discountDraft?.let { draft ->
            DiscountDialog(
                draft = draft,
                onDraftChange = { onEvent(ProductRegisterUiEvent.DiscountDraftChanged(it)) },
                onApply = { onEvent(ProductRegisterUiEvent.DiscountApplied) },
                onDismiss = { onEvent(ProductRegisterUiEvent.DiscountDismissed) },
            )
        }

        if (uiState.showAiReplaceDialog) {
            AiReplaceDialog(
                onConfirm = { onEvent(ProductRegisterUiEvent.ConfirmAiReplace) },
                onDismiss = { onEvent(ProductRegisterUiEvent.DismissAiReplace) },
            )
        }

        if (uiState.showPreview) {
            val previewData = remember(uiState) { uiState.toPreviewData() }
            previewData?.let { data ->
                QhathiaProductPreviewDialog(
                    data = data,
                    onDismiss = { onEvent(ProductRegisterUiEvent.PreviewDismissed) },
                )
            }
        }
    }
}

/**
 * The "Vista previa" button below the tags field (Figma `1478:16307`). While the form lacks the
 * name, price or photo a preview needs, the button is disabled and [showHint] says what is missing.
 */
@Composable
private fun PreviewAction(
    enabled: Boolean,
    showHint: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
    ) {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            shape = RoundedCornerShape(Dimens.ButtonRadius),
            border = BorderStroke(
                width = 1.5.dp,
                color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            ),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
            modifier = Modifier
                .fillMaxWidth()
                .height(Dimens.ButtonHeight),
        ) {
            Text(stringResource(R.string.register_preview), style = MaterialTheme.typography.labelLarge)
        }
        if (showHint) {
            Text(
                text = stringResource(R.string.register_preview_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FormActions(
    isSubmitEnabled: Boolean,
    isSubmitting: Boolean,
    onCancel: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.SpaceSm),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            onClick = onCancel,
            enabled = !isSubmitting,
            modifier = Modifier.weight(1f),
        ) {
            Text(stringResource(R.string.register_cancel))
        }
        Button(
            onClick = onSubmit,
            enabled = isSubmitEnabled,
            shape = RoundedCornerShape(Dimens.ButtonRadius),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            modifier = Modifier
                .weight(1.4f)
                .height(Dimens.ButtonHeight),
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.height(20.dp),
                )
            } else {
                Text(stringResource(R.string.register_submit), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun DiscardChangesDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.register_discard_title)) },
        text = { Text(stringResource(R.string.register_discard_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.register_discard_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.register_discard_dismiss))
            }
        },
    )
}

@Composable
private fun AiReplaceDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.register_ai_replace_title)) },
        text = { Text(stringResource(R.string.register_ai_replace_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.register_ai_replace_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.register_ai_replace_dismiss))
            }
        },
    )
}

// ---- Previews ----

private fun sampleCategories() = persistentListOf(
    Category(id = "c1", name = "Herramienta"),
    Category(id = "c2", name = "Hogar"),
    Category(id = "c3", name = "Gasfitería"),
    Category(id = "c4", name = "Accesorios"),
    Category(id = "c5", name = "Iluminación"),
)

private fun filledState() = ProductRegisterUiState(
    name = "Taladro inalámbrico",
    price = "189.90",
    quantity = "12",
    description = "Estilo vintage, ideal para sala",
    tags = "Importado, Premium",
    categories = sampleCategories(),
    selectedCategoryId = "c2",
    images = persistentListOf(SelectedImage(uri = "content://sample/1", mimeType = "image/jpeg")),
)

@Preview(name = "Register — empty", showBackground = true)
@Preview(name = "Register — empty dark", showBackground = true, uiMode = 0x20)
@Composable
private fun ProductRegisterContentEmptyPreview() {
    QhathiaTheme {
        ProductRegisterContent(
            uiState = ProductRegisterUiState(categories = sampleCategories()),
            onEvent = {},
        )
    }
}

@Preview(name = "Register — filled valid", showBackground = true)
@Composable
private fun ProductRegisterContentFilledPreview() {
    QhathiaTheme {
        ProductRegisterContent(uiState = filledState(), onEvent = {})
    }
}

// The preview button sits at the bottom of the card, so these are tall enough to show it.
@Preview(name = "Register — preview available", showBackground = true, heightDp = 1200)
@Preview(name = "Register — preview available dark", showBackground = true, heightDp = 1200, uiMode = 0x20)
@Composable
private fun ProductRegisterContentPreviewEnabledPreview() {
    QhathiaTheme {
        ProductRegisterContent(uiState = filledState(), onEvent = {})
    }
}

@Preview(name = "Register — preview unavailable", showBackground = true, heightDp = 1200)
@Preview(name = "Register — preview unavailable dark", showBackground = true, heightDp = 1200, uiMode = 0x20)
@Composable
private fun ProductRegisterContentPreviewDisabledPreview() {
    QhathiaTheme {
        ProductRegisterContent(
            uiState = filledState().copy(images = persistentListOf()),
            onEvent = {},
        )
    }
}

@Preview(name = "Register — discount applied", showBackground = true)
@Preview(name = "Register — discount applied dark", showBackground = true, uiMode = 0x20)
@Composable
private fun ProductRegisterContentDiscountPreview() {
    QhathiaTheme {
        ProductRegisterContent(
            uiState = filledState().copy(discount = Discount(DiscountType.PERCENTAGE, 20.0)),
            onEvent = {},
        )
    }
}

@Preview(name = "Register — discount dialog", showBackground = true)
@Composable
private fun ProductRegisterContentDiscountDialogPreview() {
    QhathiaTheme {
        ProductRegisterContent(
            uiState = filledState().copy(discountDraft = DiscountDraft(price = 189.9, input = "20")),
            onEvent = {},
        )
    }
}

@Preview(name = "Register — validation error", showBackground = true)
@Composable
private fun ProductRegisterContentErrorPreview() {
    QhathiaTheme {
        ProductRegisterContent(
            uiState = filledState().copy(
                price = "0",
                priceError = R.string.register_error_price,
                quantity = "-3",
                quantityError = R.string.register_error_quantity,
            ),
            onEvent = {},
        )
    }
}

@Preview(name = "Register — submitting", showBackground = true)
@Composable
private fun ProductRegisterContentSubmittingPreview() {
    QhathiaTheme {
        ProductRegisterContent(
            uiState = filledState().copy(isSubmitting = true),
            onEvent = {},
        )
    }
}

@Preview(name = "Register — AI loading", showBackground = true)
@Preview(name = "Register — AI loading dark", showBackground = true, uiMode = 0x20)
@Composable
private fun ProductRegisterContentAiLoadingPreview() {
    QhathiaTheme {
        ProductRegisterContent(
            uiState = filledState().copy(description = "", tags = "", isAiLoading = true),
            onEvent = {},
        )
    }
}

@Preview(name = "Register — AI replace dialog", showBackground = true)
@Preview(name = "Register — AI replace dialog dark", showBackground = true, uiMode = 0x20)
@Composable
private fun ProductRegisterContentAiReplaceDialogPreview() {
    QhathiaTheme {
        ProductRegisterContent(
            uiState = filledState().copy(showAiReplaceDialog = true),
            onEvent = {},
        )
    }
}
