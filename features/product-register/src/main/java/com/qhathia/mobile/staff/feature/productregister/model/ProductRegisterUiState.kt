package com.qhathia.mobile.staff.feature.productregister.model

import androidx.annotation.StringRes
import com.qhathia.mobile.staff.core.common.validation.isValidPrice
import com.qhathia.mobile.staff.core.common.validation.isValidProductName
import com.qhathia.mobile.staff.core.common.validation.isValidQuantity
import com.qhathia.mobile.staff.core.common.validation.parsePrice
import com.qhathia.mobile.staff.core.domain.model.Category
import com.qhathia.mobile.staff.core.domain.model.Discount
import com.qhathia.mobile.staff.core.domain.model.DiscountDraft
import com.qhathia.mobile.staff.core.domain.model.SelectedImage
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Immutable snapshot of the product registration form. Field errors are string-resource ids so all
 * user-facing text stays externalized. Tags are entered as a free-text, comma-separated string (per
 * the Figma mockup). [isSubmitEnabled] derives the submit gate from the raw field values (name,
 * price > 0, quantity ≥ 0, a category, ≥ 1 image) regardless of which errors are surfaced. Tags are
 * optional: an empty field registers the product with no tags.
 *
 * [discount] is the optional discount the merchant applied; it is only sent once the product exists.
 * [discountDraft] is non-null while the "Aplicar descuento" dialog is open. [showPreview] is true
 * while the "Vista previa" dialog is open; it reads the form but never changes it.
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
    val discount: Discount? = null,
    val discountDraft: DiscountDraft? = null,
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
    val showPreview: Boolean = false,
) {
    val isSubmitEnabled: Boolean
        get() = !isSubmitting &&
            isValidProductName(name) &&
            isValidPrice(price) &&
            isValidQuantity(quantity) &&
            selectedCategoryId != null &&
            images.isNotEmpty()

    /** What the product costs with [discount] applied; null without a discount or a valid price. */
    val discountedPrice: Double?
        get() {
            val applied = discount ?: return null
            return parsePrice(price)?.let { applied.applyTo(it) }
        }

    /**
     * The applied discount no longer fits the price typed: only a fixed amount above the price.
     * Submitting removes it with a message instead of creating a product priced at zero.
     */
    val isDiscountOverPrice: Boolean
        get() {
            val applied = discount ?: return false
            return parsePrice(price)?.let { !applied.fits(it) } == true
        }

    /** A discount needs a price to be taken off, so the toggle stays locked until one is entered. */
    val isDiscountToggleEnabled: Boolean
        get() = !isSubmitting && isValidPrice(price)

    /**
     * A preview needs something to show: a name, a price and a photo. Everything else is optional,
     * so the product can be previewed well before the form can be submitted.
     */
    val isPreviewEnabled: Boolean
        get() = !isSubmitting &&
            isValidProductName(name) &&
            isValidPrice(price) &&
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
