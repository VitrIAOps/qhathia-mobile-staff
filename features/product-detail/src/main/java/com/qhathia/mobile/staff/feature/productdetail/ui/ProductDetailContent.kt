package com.qhathia.mobile.staff.feature.productdetail.ui

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
import com.qhathia.mobile.staff.core.domain.model.DiscountType
import com.qhathia.mobile.staff.core.ui.component.QhathiaDiscountToggleRow
import com.qhathia.mobile.staff.core.ui.component.productpreview.QhathiaProductPreviewDialog
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme
import com.qhathia.mobile.staff.feature.productdetail.R
import com.qhathia.mobile.staff.feature.productdetail.model.ProductDetailUiEvent
import com.qhathia.mobile.staff.feature.productdetail.model.ProductDetailUiState
import com.qhathia.mobile.staff.feature.productdetail.model.toPreviewData
import kotlinx.collections.immutable.persistentListOf

/**
 * Stateless detail/edit form — a pure function of [uiState]. Mirrors the registration screen's
 * layout (header over a white scrollable card) so the two flows read as one, with the photos section
 * read-only and a loading/error gate in front of the form.
 */
@Composable
fun ProductDetailContent(
    uiState: ProductDetailUiState,
    onEvent: (ProductDetailUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
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
            ProductDetailHeader(
                onBack = { onEvent(ProductDetailUiEvent.CancelRequested) },
                onOpenOrderRequests = { onEvent(ProductDetailUiEvent.OrderRequestsRequested) },
                bellEnabled = !uiState.isSaving,
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = Dimens.SpaceMd),
                shape = RoundedCornerShape(Dimens.SpaceMd),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
            ) {
                when {
                    uiState.isLoading -> CenteredSlot { CircularProgressIndicator() }
                    uiState.isError -> CenteredSlot {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
                        ) {
                            Text(
                                text = stringResource(R.string.detail_load_error),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            TextButton(onClick = { onEvent(ProductDetailUiEvent.Retry) }) {
                                Text(stringResource(R.string.detail_retry))
                            }
                        }
                    }
                    else -> EditForm(uiState = uiState, onEvent = onEvent)
                }
            }
        }

        if (uiState.isSaving) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        uiState.discountDraft?.let { draft ->
            DiscountDialog(
                draft = draft,
                onDraftChange = { onEvent(ProductDetailUiEvent.DiscountDraftChanged(it)) },
                onApply = { onEvent(ProductDetailUiEvent.DiscountApplied) },
                onDismiss = { onEvent(ProductDetailUiEvent.DiscountDismissed) },
            )
        }

        if (uiState.showDiscardDialog) {
            DiscardChangesDialog(
                onConfirm = { onEvent(ProductDetailUiEvent.ConfirmDiscard) },
                onDismiss = { onEvent(ProductDetailUiEvent.DismissDiscard) },
            )
        }

        if (uiState.showPreview) {
            val previewData = remember(uiState) { uiState.toPreviewData() }
            previewData?.let { data ->
                QhathiaProductPreviewDialog(
                    data = data,
                    onDismiss = { onEvent(ProductDetailUiEvent.PreviewDismissed) },
                )
            }
        }
    }
}

/**
 * The "Vista previa" button below the tags field, mirroring the registration form. While the form
 * lacks the name, price or photo a preview needs, the button is disabled and [showHint] says what
 * is missing.
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
            Text(stringResource(R.string.detail_preview), style = MaterialTheme.typography.labelLarge)
        }
        if (showHint) {
            Text(
                text = stringResource(R.string.detail_preview_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EditForm(
    uiState: ProductDetailUiState,
    onEvent: (ProductDetailUiEvent) -> Unit,
) {
    val enabled = !uiState.isSaving
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(Dimens.SpaceLg),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceLg),
    ) {
        LabeledField(label = stringResource(R.string.detail_photos_label)) {
            ProductImagesRow(imageUrls = uiState.imageUrls)
        }
        DetailNameField(
            value = uiState.name,
            onValueChange = { onEvent(ProductDetailUiEvent.NameChanged(it)) },
            enabled = enabled,
            errorRes = uiState.nameError,
        )
        DetailDescriptionField(
            value = uiState.description,
            onValueChange = { onEvent(ProductDetailUiEvent.DescriptionChanged(it)) },
            enabled = enabled,
            onOptimizeWithAi = { onEvent(ProductDetailUiEvent.OptimizeDescriptionWithAi) },
            isAiLoading = uiState.isAiLoading,
            isAiActionEnabled = uiState.isAiActionEnabled,
        )
        LabeledField(label = stringResource(R.string.detail_category_label)) {
            CategorySelector(
                categories = uiState.categories,
                selectedCategoryId = uiState.selectedCategoryId,
                isLoading = uiState.isCategoriesLoading,
                isError = uiState.isCategoriesError,
                onSelect = { onEvent(ProductDetailUiEvent.SelectCategory(it)) },
                onRetry = { onEvent(ProductDetailUiEvent.RetryCatalog) },
                enabled = enabled,
            )
        }
        DetailPriceField(
            value = uiState.price,
            onValueChange = { onEvent(ProductDetailUiEvent.PriceChanged(it)) },
            enabled = enabled,
            errorRes = uiState.priceError,
        )
        Column {
            QhathiaDiscountToggleRow(
                checked = uiState.discount != null || uiState.discountDraft != null,
                onCheckedChange = { onEvent(ProductDetailUiEvent.DiscountToggled(it)) },
                enabled = uiState.isDiscountToggleEnabled,
                isLoading = uiState.isDiscountLoading,
                summary = uiState.discount?.summaryText(),
                finalPrice = uiState.discountedPrice,
                onSummaryClick = { onEvent(ProductDetailUiEvent.DiscountSummaryClicked) },
            )
            if (uiState.isDiscountError) {
                DiscountLoadError(
                    enabled = enabled,
                    onRetry = { onEvent(ProductDetailUiEvent.RetryDiscount) },
                )
            }
        }
        DetailQuantityField(
            value = uiState.quantity,
            onValueChange = { onEvent(ProductDetailUiEvent.QuantityChanged(it)) },
            enabled = enabled,
            errorRes = uiState.quantityError,
        )
        DetailTagsField(
            value = uiState.tags,
            onValueChange = { onEvent(ProductDetailUiEvent.TagsChanged(it)) },
            enabled = enabled,
        )
        PreviewAction(
            enabled = uiState.isPreviewEnabled,
            // While saving the whole form is locked; the hint is about missing data.
            showHint = !uiState.isPreviewEnabled && enabled,
            onClick = { onEvent(ProductDetailUiEvent.PreviewRequested) },
        )

        Spacer(Modifier.height(Dimens.SpaceSm))

        FormActions(
            isSaveEnabled = uiState.isSaveEnabled,
            isSaving = uiState.isSaving,
            onCancel = { onEvent(ProductDetailUiEvent.CancelRequested) },
            onSave = { onEvent(ProductDetailUiEvent.Save) },
        )
    }
}

/** Shown under the disabled discount toggle when the product's discount could not be loaded. */
@Composable
private fun DiscountLoadError(
    enabled: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.detail_discount_load_error),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onRetry, enabled = enabled) {
            Text(stringResource(R.string.detail_retry))
        }
    }
}

@Composable
private fun CenteredSlot(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.SpaceLg),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun FormActions(
    isSaveEnabled: Boolean,
    isSaving: Boolean,
    onCancel: () -> Unit,
    onSave: () -> Unit,
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
            enabled = !isSaving,
            modifier = Modifier.weight(1f),
        ) {
            Text(stringResource(R.string.detail_cancel))
        }
        Button(
            onClick = onSave,
            enabled = isSaveEnabled,
            shape = RoundedCornerShape(Dimens.ButtonRadius),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            modifier = Modifier
                .weight(1.4f)
                .height(Dimens.ButtonHeight),
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.height(20.dp),
                )
            } else {
                Text(stringResource(R.string.detail_save), style = MaterialTheme.typography.labelLarge)
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
        title = { Text(stringResource(R.string.detail_discard_title)) },
        text = { Text(stringResource(R.string.detail_discard_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.detail_discard_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.detail_discard_dismiss))
            }
        },
    )
}

// ---- Previews ----

private fun sampleCategories() = persistentListOf(
    Category(id = "c1", name = "Herramienta"),
    Category(id = "c2", name = "Hogar"),
    Category(id = "c3", name = "Gasfitería"),
)

private fun loadedState() = ProductDetailUiState(
    name = "Taladro inalámbrico",
    price = "189.90",
    quantity = "12",
    description = "Estilo vintage, ideal para sala",
    tags = "Importado, Premium",
    categories = sampleCategories(),
    selectedCategoryId = "c2",
    imageUrls = persistentListOf("https://cdn.qhathia.test/1.jpg"),
    isLoading = false,
)

@Preview(name = "Detail — with discount", showBackground = true)
@Preview(name = "Detail — with discount dark", showBackground = true, uiMode = 0x20)
@Composable
private fun ProductDetailContentDiscountPreview() {
    QhathiaTheme {
        ProductDetailContent(
            uiState = loadedState().copy(
                loadedDiscount = Discount(DiscountType.FIXED_AMOUNT, 15.0),
                discount = Discount(DiscountType.FIXED_AMOUNT, 15.0),
            ),
            onEvent = {},
        )
    }
}

@Preview(name = "Detail — discount load error", showBackground = true)
@Composable
private fun ProductDetailContentDiscountErrorPreview() {
    QhathiaTheme {
        ProductDetailContent(uiState = loadedState().copy(isDiscountError = true), onEvent = {})
    }
}

@Preview(name = "Detail — loading", showBackground = true)
@Composable
private fun ProductDetailContentLoadingPreview() {
    QhathiaTheme {
        ProductDetailContent(uiState = ProductDetailUiState(), onEvent = {})
    }
}

@Preview(name = "Detail — error", showBackground = true)
@Composable
private fun ProductDetailContentErrorPreview() {
    QhathiaTheme {
        ProductDetailContent(
            uiState = ProductDetailUiState(isLoading = false, isError = true),
            onEvent = {},
        )
    }
}

@Preview(name = "Detail — loaded", showBackground = true)
@Preview(name = "Detail — loaded dark", showBackground = true, uiMode = 0x20)
@Composable
private fun ProductDetailContentLoadedPreview() {
    QhathiaTheme {
        ProductDetailContent(uiState = loadedState(), onEvent = {})
    }
}

// The preview button sits at the bottom of the card, so these are tall enough to show it.
@Preview(name = "Detail — preview available", showBackground = true, heightDp = 1200)
@Preview(name = "Detail — preview available dark", showBackground = true, heightDp = 1200, uiMode = 0x20)
@Composable
private fun ProductDetailContentPreviewEnabledPreview() {
    QhathiaTheme {
        ProductDetailContent(uiState = loadedState(), onEvent = {})
    }
}

@Preview(name = "Detail — preview unavailable, no photos", showBackground = true, heightDp = 1200)
@Preview(
    name = "Detail — preview unavailable, no photos dark",
    showBackground = true,
    heightDp = 1200,
    uiMode = 0x20,
)
@Composable
private fun ProductDetailContentPreviewDisabledPreview() {
    QhathiaTheme {
        ProductDetailContent(
            uiState = loadedState().copy(imageUrls = persistentListOf()),
            onEvent = {},
        )
    }
}

@Preview(name = "Detail — no photos", showBackground = true)
@Composable
private fun ProductDetailContentNoPhotosPreview() {
    QhathiaTheme {
        ProductDetailContent(
            uiState = loadedState().copy(imageUrls = persistentListOf()),
            onEvent = {},
        )
    }
}

@Preview(name = "Detail — validation error", showBackground = true)
@Composable
private fun ProductDetailContentValidationPreview() {
    QhathiaTheme {
        ProductDetailContent(
            uiState = loadedState().copy(
                price = "0",
                priceError = R.string.detail_error_price,
                quantity = "-3",
                quantityError = R.string.detail_error_quantity,
                isDirty = true,
            ),
            onEvent = {},
        )
    }
}

@Preview(name = "Detail — saving", showBackground = true)
@Composable
private fun ProductDetailContentSavingPreview() {
    QhathiaTheme {
        ProductDetailContent(uiState = loadedState().copy(isSaving = true), onEvent = {})
    }
}
