package com.qhathia.mobile.staff.feature.productdetail.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.qhathia.mobile.staff.core.domain.model.Discount
import com.qhathia.mobile.staff.core.domain.model.DiscountDraft
import com.qhathia.mobile.staff.core.domain.model.DiscountInputError
import com.qhathia.mobile.staff.core.domain.model.DiscountType
import com.qhathia.mobile.staff.core.ui.R as CoreUiR
import com.qhathia.mobile.staff.core.ui.component.QhathiaDiscountDialog
import kotlinx.collections.immutable.toImmutableList

/**
 * Binds the domain [DiscountDraft] to the design-system discount dialog, which only knows
 * primitives: every edit is reported back as the next draft.
 */
@Composable
internal fun DiscountDialog(
    draft: DiscountDraft,
    onDraftChange: (DiscountDraft) -> Unit,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
) {
    QhathiaDiscountDialog(
        originalPrice = draft.price,
        resultingPrice = draft.resultingPrice,
        isPercentage = draft.type == DiscountType.PERCENTAGE,
        input = draft.input,
        quickPercentages = remember { DiscountDraft.QUICK_PERCENTAGES.toImmutableList() },
        isApplyEnabled = draft.isValid,
        onModeChange = { isPercentage ->
            onDraftChange(
                draft.withType(if (isPercentage) DiscountType.PERCENTAGE else DiscountType.FIXED_AMOUNT),
            )
        },
        onInputChange = { onDraftChange(draft.withInput(it)) },
        onQuickPercentage = { onDraftChange(draft.withQuickPercentage(it)) },
        onApply = onApply,
        onDismiss = onDismiss,
        errorText = when (draft.error) {
            DiscountInputError.InvalidPercentage -> stringResource(CoreUiR.string.discount_error_percentage)
            DiscountInputError.InvalidAmount -> stringResource(CoreUiR.string.discount_error_amount)
            DiscountInputError.AmountAbovePrice ->
                stringResource(CoreUiR.string.discount_error_amount_above_price)
            null -> null
        },
    )
}

/** "Descuento de 20%" or "Descuento de 15 soles", shown under the discount toggle. */
@Composable
internal fun Discount.summaryText(): String = stringResource(
    when (type) {
        DiscountType.PERCENTAGE -> CoreUiR.string.discount_summary_percentage
        DiscountType.FIXED_AMOUNT -> CoreUiR.string.discount_summary_amount
    },
    valueText(),
)
