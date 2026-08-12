package com.qhathia.mobile.staff.feature.productdetail.model

import androidx.annotation.StringRes
import com.qhathia.mobile.staff.core.common.validation.isValidPrice
import com.qhathia.mobile.staff.core.common.validation.isValidProductName
import com.qhathia.mobile.staff.core.common.validation.isValidQuantity
import com.qhathia.mobile.staff.core.domain.model.Category
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Immutable snapshot of the product detail/edit form. Mirrors the registration form's shape — field
 * errors as string-resource ids, tags as free comma-separated text — with two differences: the
 * product has to be loaded first ([isLoading] / [isError]), and photos are read-only remote URLs, so
 * there is no "at least one image" gate on saving.
 *
 * [allowDiscountInquiry] and [position] are not editable but are carried through from the loaded
 * product, because `PUT /products/{id}` is a full replace that would otherwise reset them.
 */
data class ProductDetailUiState(
    val name: String = "",
    val price: String = "",
    val quantity: String = "",
    val description: String = "",
    val tags: String = "",
    val categories: ImmutableList<Category> = persistentListOf(),
    val selectedCategoryId: String? = null,
    val imageUrls: ImmutableList<String> = persistentListOf(),
    val allowDiscountInquiry: Boolean = false,
    val position: Int = 0,
    @param:StringRes val nameError: Int? = null,
    @param:StringRes val priceError: Int? = null,
    @param:StringRes val quantityError: Int? = null,
    val isLoading: Boolean = true,
    val isError: Boolean = false,
    val isCategoriesLoading: Boolean = false,
    val isCategoriesError: Boolean = false,
    val isSaving: Boolean = false,
    val isAiLoading: Boolean = false,
    val isDirty: Boolean = false,
    val showDiscardDialog: Boolean = false,
) {
    /**
     * The save gate: the product must be loaded and every editable field valid. Unlike the
     * registration form there is no image requirement — the merchant may be editing a product whose
     * photos live on the backend.
     */
    val isSaveEnabled: Boolean
        get() = !isLoading &&
            !isError &&
            !isSaving &&
            isDirty &&
            isValidProductName(name) &&
            isValidPrice(price) &&
            isValidQuantity(quantity) &&
            selectedCategoryId != null &&
            parseTags(tags).isNotEmpty()

    /** The AI assist optimizes the existing description, so it needs one and no request in flight. */
    val isAiActionEnabled: Boolean
        get() = description.isNotBlank() && !isAiLoading && !isSaving && !isLoading
}

/** Splits the free-text tag input into trimmed, non-blank tag values. */
fun parseTags(input: String): List<String> =
    input.split(',').map { it.trim() }.filter { it.isNotEmpty() }
