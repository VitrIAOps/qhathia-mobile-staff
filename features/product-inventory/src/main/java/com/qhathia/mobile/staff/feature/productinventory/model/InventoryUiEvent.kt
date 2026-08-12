package com.qhathia.mobile.staff.feature.productinventory.model

import com.qhathia.mobile.staff.core.domain.model.Availability

/** User intents from the inventory screen. */
sealed interface InventoryUiEvent {
    data object Load : InventoryUiEvent
    data object Retry : InventoryUiEvent
    data class SelectCategory(val categoryId: String?) : InventoryUiEvent
    data class SelectAvailability(val availability: Availability) : InventoryUiEvent
    data object ClearAvailability : InventoryUiEvent
    data class Search(val query: String) : InventoryUiEvent
    data object RegisterProductClicked : InventoryUiEvent

    /** A product row was tapped; opens its detail/edit screen. */
    data class ProductClicked(val productId: String) : InventoryUiEvent
}
