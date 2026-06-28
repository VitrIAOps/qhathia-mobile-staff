package com.qhathia.mobile.staff.feature.productinventory.model

import com.qhathia.mobile.staff.core.domain.model.Availability
import com.qhathia.mobile.staff.core.domain.model.Category
import com.qhathia.mobile.staff.core.domain.model.Product
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Immutable snapshot of the inventory screen. [loadedProducts] are what the current server-side
 * filters returned; [visibleProducts] is that list narrowed by the client-side name [searchQuery].
 * A null [selectedCategoryId] means the "Todas" chip; a null [selectedAvailability] means no
 * availability sub-filter.
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
) {
    /** Number of products currently shown after filters + search. */
    val productCount: Int get() = visibleProducts.size
}
