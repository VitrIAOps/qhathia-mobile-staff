package com.qhathia.mobile.staff.feature.productregister.model

import androidx.annotation.StringRes
import com.qhathia.mobile.staff.core.common.validation.isValidPrice
import com.qhathia.mobile.staff.core.common.validation.isValidProductName
import com.qhathia.mobile.staff.core.common.validation.isValidQuantity
import com.qhathia.mobile.staff.core.domain.model.Category
import com.qhathia.mobile.staff.core.domain.model.SelectedImage
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Immutable snapshot of the product registration form. Field errors are string-resource ids so all
 * user-facing text stays externalized. Tags are entered as a free-text, comma-separated string (per
 * the Figma mockup). [isSubmitEnabled] derives the submit gate from the raw field values (name,
 * price > 0, quantity ≥ 0, a category, ≥ 1 tag, ≥ 1 image) regardless of which errors are surfaced.
 */
data class ProductRegisterUiState(
    val name: String = "",
    val price: String = "",
    val quantity: String = "",
    val description: String = "",
    val tags: String = "",
    val categories: ImmutableList<Category> = persistentListOf(),
    val selectedCategoryId: String? = null,
    val images: ImmutableList<SelectedImage> = persistentListOf(),
    @param:StringRes val nameError: Int? = null,
    @param:StringRes val priceError: Int? = null,
    @param:StringRes val quantityError: Int? = null,
    val isCategoriesLoading: Boolean = false,
    val isCategoriesError: Boolean = false,
    val isSubmitting: Boolean = false,
    val isAiLoading: Boolean = false,
    val isDirty: Boolean = false,
    val showDiscardDialog: Boolean = false,
    val showAiReplaceDialog: Boolean = false,
) {
    val isSubmitEnabled: Boolean
        get() = !isSubmitting &&
            isValidProductName(name) &&
            isValidPrice(price) &&
            isValidQuantity(quantity) &&
            selectedCategoryId != null &&
            parseTags(tags).isNotEmpty() &&
            images.isNotEmpty()

    /** The AI assist action is available once an image is present and no request is in flight. */
    val isAiActionEnabled: Boolean
        get() = images.isNotEmpty() && !isAiLoading && !isSubmitting

    /**
     * True when the AI-fillable fields (description, category, tags) are all already provided, so the
     * action optimizes the existing description instead of drafting from the image.
     */
    val isOptimizeMode: Boolean
        get() = description.isNotBlank() &&
            selectedCategoryId != null &&
            parseTags(tags).isNotEmpty()
}

/** Splits the free-text tag input into trimmed, non-blank tag values. */
fun parseTags(input: String): List<String> =
    input.split(',').map { it.trim() }.filter { it.isNotEmpty() }
