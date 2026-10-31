package com.qhathia.mobile.staff.feature.productdetail.model

import androidx.annotation.StringRes
import com.qhathia.mobile.staff.core.common.validation.isValidPrice
import com.qhathia.mobile.staff.core.common.validation.isValidProductName
import com.qhathia.mobile.staff.core.common.validation.isValidQuantity
import com.qhathia.mobile.staff.core.common.validation.parsePrice
import com.qhathia.mobile.staff.core.domain.model.Category
import com.qhathia.mobile.staff.core.domain.model.Discount
import com.qhathia.mobile.staff.core.domain.model.DiscountDraft
import com.qhathia.mobile.staff.core.domain.model.ProductImage
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Immutable snapshot of the product detail/edit form. Mirrors the registration form's shape — field
 * errors as string-resource ids, tags as free comma-separated text — except that the product has to
 * be loaded first ([isLoading] / [isError]).
 *
 * Photos are a form field like the discount: [loadedImages] is what the product has stored,
 * [photos] is what the strip shows (saved photos plus pending picks), and the difference is only
 * persisted on save. A product keeps between one and [MAX_PHOTOS] photos.
 *
 * [allowDiscountInquiry] and [position] are not editable but are carried through from the loaded
 * product, because `PUT /products/{id}` is a full replace that would otherwise reset them.
 *
 * The discount is a form field like the rest: [loadedDiscount] is what the product has stored,
 * [discount] is what the form shows, and the difference is only persisted on save.
 * [discountDraft] is non-null while the "Aplicar descuento" dialog is open. [serverPricing] is what
 * the backend priced for the product as loaded; it is dropped once a save has reached the server.
 * [showPreview] is true while the "Vista previa" dialog is open; it reads the form but never
 * changes it.
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
    val photos: ImmutableList<FormPhoto> = persistentListOf(),
    val loadedImages: ImmutableList<ProductImage> = persistentListOf(),
    val allowDiscountInquiry: Boolean = false,
    val position: Int = 0,
    val loadedDiscount: Discount? = null,
    val discount: Discount? = null,
    val discountDraft: DiscountDraft? = null,
    val serverPricing: ServerPricing? = null,
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

    /**
     * What the product costs with [discount] applied; null without a discount or a valid price.
     *
     * While the price and the discount are the ones loaded, this is the backend's number, so the
     * detail matches the inventory list and the kiosk. A `null` from the backend means no discount
     * is in force right now (for example one that has not started), so the regular price is shown.
     * Once either is edited the backend cannot price the draft, and the live preview is computed here.
     */
    val discountedPrice: Double?
        get() {
            val applied = discount ?: return null
            val server = serverPricing
            if (server != null && price == server.priceText && discount == loadedDiscount) {
                return server.discountedPrice ?: parsePrice(price)
            }
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

    /** Saved photos the merchant removed on the form; deleted on save. */
    val removedImageIds: List<String>
        get() {
            val kept = photos.filterIsInstance<FormPhoto.Saved>().map { it.image.id }.toSet()
            return loadedImages.map { it.id }.filterNot { it in kept }
        }

    /** Photos picked on the form; uploaded on save. */
    val addedImages: List<FormPhoto.Pending>
        get() = photos.filterIsInstance<FormPhoto.Pending>()

    val isPhotosChanged: Boolean
        get() = removedImageIds.isNotEmpty() || addedImages.isNotEmpty()

    val canAddPhotos: Boolean
        get() = photos.size < MAX_PHOTOS

    /** A product keeps at least one photo, so the last one cannot be removed. */
    val canRemovePhotos: Boolean
        get() = photos.size > 1

    /** A preview needs something to show: a name, a price and a photo. */
    val isPreviewEnabled: Boolean
        get() = !isLoading &&
            !isError &&
            !isSaving &&
            isValidProductName(name) &&
            isValidPrice(price) &&
            photos.isNotEmpty()

    /** The AI assist optimizes the existing description, so it needs one and no request in flight. */
    val isAiActionEnabled: Boolean
        get() = description.isNotBlank() && !isAiLoading && !isSaving && !isLoading
}

/**
 * The backend's pricing of the product as loaded: [priceText] is the price exactly as the form was
 * seeded with it, and [discountedPrice] is the server-computed `discountedPrice` (`null` when no
 * discount is in force).
 */
data class ServerPricing(val priceText: String, val discountedPrice: Double?)

/** The backend's per-product photo limit. */
const val MAX_PHOTOS = 10

/** Splits the free-text tag input into trimmed, non-blank tag values. */
fun parseTags(input: String): List<String> =
    input.split(',').map { it.trim() }.filter { it.isNotEmpty() }
