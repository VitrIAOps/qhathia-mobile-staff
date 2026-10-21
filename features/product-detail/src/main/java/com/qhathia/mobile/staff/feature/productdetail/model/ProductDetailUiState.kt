package com.qhathia.mobile.staff.feature.productdetail.model

import androidx.annotation.StringRes
import com.qhathia.mobile.staff.core.common.validation.isValidPrice
import com.qhathia.mobile.staff.core.common.validation.isValidProductName
import com.qhathia.mobile.staff.core.common.validation.isValidQuantity
import com.qhathia.mobile.staff.core.common.validation.parsePrice
import com.qhathia.mobile.staff.core.domain.model.Category
import com.qhathia.mobile.staff.core.domain.model.Discount
import com.qhathia.mobile.staff.core.domain.model.DiscountDraft
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
 *
 * The discount is a form field like the rest: [loadedDiscount] is what the product has stored,
 * [discount] is what the form shows, and the difference is only persisted on save.
 * [discountDraft] is non-null while the "Aplicar descuento" dialog is open. [showPreview] is true
 * while the "Vista previa" dialog is open; it reads the form but never changes it.
 *
 * "Eliminar producto" asks first: [showDeleteDialog] while the confirmation is open, [isDeleting]
 * while its request is in flight (the form is locked meanwhile).
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
    val loadedDiscount: Discount? = null,
    val discount: Discount? = null,
    val discountDraft: DiscountDraft? = null,
    val isDiscountLoading: Boolean = false,
    val isDiscountError: Boolean = false,
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
    val showPreview: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val isDeleting: Boolean = false,
) {
    /**
     * The save gate: the product must be loaded and every required field valid (tags are
     * optional — clearing them saves the product with none). Unlike the
     * registration form there is no image requirement — the merchant may be editing a product whose
     * photos live on the backend.
     */
    val isSaveEnabled: Boolean
        get() = !isLoading &&
            !isError &&
            !isSaving &&
            !isDeleting &&
            isDirty &&
            isValidProductName(name) &&
            isValidPrice(price) &&
            isValidQuantity(quantity) &&
            selectedCategoryId != null

    /** What the product costs with [discount] applied; null without a discount or a valid price. */
    val discountedPrice: Double?
        get() {
            val applied = discount ?: return null
            return parsePrice(price)?.let { applied.applyTo(it) }
        }

    /** True when saving has a discount change to persist. */
    val isDiscountChanged: Boolean
        get() = discount != loadedDiscount

    /**
     * The discount can only be edited once it is known (loaded without error) and there is a valid
     * price to take it off.
     */
    val isDiscountToggleEnabled: Boolean
        get() = !isSaving && !isDiscountLoading && !isDiscountError && isValidPrice(price)

    /**
     * A preview needs something to show: a name, a price and a photo. Photos are read-only here, so
     * a product registered without any cannot be previewed from this screen.
     */
    val isPreviewEnabled: Boolean
        get() = !isLoading &&
            !isError &&
            !isSaving &&
            isValidProductName(name) &&
            isValidPrice(price) &&
            imageUrls.isNotEmpty()

    /** The AI assist optimizes the existing description, so it needs one and no request in flight. */
    val isAiActionEnabled: Boolean
        get() = description.isNotBlank() && !isAiLoading && !isSaving && !isLoading
}

/** Splits the free-text tag input into trimmed, non-blank tag values. */
fun parseTags(input: String): List<String> =
    input.split(',').map { it.trim() }.filter { it.isNotEmpty() }
