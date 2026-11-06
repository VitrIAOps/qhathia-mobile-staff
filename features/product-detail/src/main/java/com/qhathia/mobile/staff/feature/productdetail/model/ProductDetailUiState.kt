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
import java.time.Instant

/**
 * Immutable snapshot of the product detail/edit form. Mirrors the registration form's shape — field
 * errors as string-resource ids, tags as free comma-separated text — except that the product has to
 * be loaded first ([isLoading] / [isError]).
 *
 * Photos are a form field like the discount: [loadedImages] is what the product has stored,
 * [photos] is what the strip shows (saved photos plus pending picks), and the difference is only
 * persisted on save. A product keeps between one and [MAX_PHOTOS] photos.
 *
 * The update is partial: [loadedVersion] is the product's version, sent back so a stale edit is
 * caught as a conflict ([conflict] holds the open "Otro usuario cambió este producto" dialog), and
 * [loadedQuantity] is the stock text as seeded, so an untouched stock is not sent and a sale
 * confirmed meanwhile is not undone. [isConsult] marks a product loaded without stock, which can be
 * saved with the stock field empty.
 *
 * The discount is a form field like the rest: [loadedDiscount] is what the product has stored,
 * [discount] is what the form shows, and the difference is only persisted on save. Editing the
 * price never changes [discount]; [isDiscountOverPrice] flags a fixed amount that no longer fits.
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
    val loadedVersion: Long? = null,
    val loadedQuantity: String = "",
    val isConsult: Boolean = false,
    val conflict: SaveConflict? = null,
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
     * optional — clearing them saves the product with none; a "consultar" product may keep its
     * stock empty). Unlike the
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
            (isValidQuantity(quantity) || (isConsult && quantity.isBlank())) &&
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
            if (server != null && isPricingAsLoaded) {
                return server.discountedPrice ?: parsePrice(price)
            }
            return parsePrice(price)?.let { applied.applyTo(it) }
        }

    /**
     * When the discount in force ends, as the backend reports it. Only for the discount loaded with
     * the product and while the backend applies it: an edited or unsaved discount has no end yet,
     * and one the backend does not apply (`discountedPrice` null) is not counting down.
     */
    val discountEndsAt: Instant?
        get() {
            val server = serverPricing ?: return null
            if (discount == null || !isPricingAsLoaded || server.discountedPrice == null) return null
            return server.discountEndsAt
        }

    /** True while the form's price and discount are exactly the ones loaded with the product. */
    private val isPricingAsLoaded: Boolean
        get() = serverPricing?.priceText == price && discount == loadedDiscount

    /**
     * The form's discount no longer fits the price typed: only a fixed amount above the price.
     * Saving removes it with a message instead of pricing the product at zero.
     */
    val isDiscountOverPrice: Boolean
        get() {
            val applied = discount ?: return false
            return parsePrice(price)?.let { !applied.fits(it) } == true
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
 * discount is in force), ending at [discountEndsAt].
 */
data class ServerPricing(
    val priceText: String,
    val discountedPrice: Double?,
    val discountEndsAt: Instant? = null,
)

/**
 * A save rejected because the product changed on the server. [currentVersion] is the server's
 * version when the error carried it; `null` means it has to be read before saving again.
 */
data class SaveConflict(val currentVersion: Long?)

/** The backend's per-product photo limit. */
const val MAX_PHOTOS = 10

/** Splits the free-text tag input into trimmed, non-blank tag values. */
fun parseTags(input: String): List<String> =
    input.split(',').map { it.trim() }.filter { it.isNotEmpty() }
