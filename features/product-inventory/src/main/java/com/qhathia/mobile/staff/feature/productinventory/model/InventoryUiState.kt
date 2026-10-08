package com.qhathia.mobile.staff.feature.productinventory.model

import com.qhathia.mobile.staff.core.domain.model.Availability
import com.qhathia.mobile.staff.core.domain.model.Category
import com.qhathia.mobile.staff.core.domain.model.Discount
import com.qhathia.mobile.staff.core.domain.model.DiscountDraft
import com.qhathia.mobile.staff.core.domain.model.Product
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf

/**
 * Immutable snapshot of the inventory screen. [loadedProducts] are what the current server-side
 * filters returned; [visibleProducts] is that list narrowed by the client-side name [searchQuery].
 * A null [selectedCategoryId] means the "Todas" chip; a null [selectedAvailability] means no
 * availability sub-filter.
 *
 * "Editar descuento" on a row first loads that product's discount ([isDiscountLoading]) and then
 * opens the dialog: [discountDraft] is non-null while it is open, for [discountProductId].
 *
 * The list endpoint carries no discount data, so whether a product has one is only learned when
 * its actions menu is opened. [knownDiscounts] remembers the answers (a null value means "looked
 * up, has none") and [discountLookupProductId] is the product being looked up right now.
 */
data class InventoryUiState(
    val loadedProducts: ImmutableList<Product> = persistentListOf(),
    val visibleProducts: ImmutableList<Product> = persistentListOf(),
    val categories: ImmutableList<Category> = persistentListOf(),
    val selectedCategoryId: String? = null,
    val selectedAvailability: Availability? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val knownDiscounts: ImmutableMap<String, Discount?> = persistentMapOf(),
    val discountLookupProductId: String? = null,
    val discountProductId: String? = null,
    val discountDraft: DiscountDraft? = null,
    val isDiscountLoading: Boolean = false,
    val isDiscountSaving: Boolean = false,
) {
    /** Number of products currently shown after filters + search. */
    val productCount: Int get() = visibleProducts.size

    /**
     * Whether the product has a discount: true / false once looked up, null while that is still
     * unknown (never looked up, in flight, or the lookup failed).
     */
    fun hasDiscount(productId: String): Boolean? =
        if (knownDiscounts.containsKey(productId)) knownDiscounts[productId] != null else null
}
