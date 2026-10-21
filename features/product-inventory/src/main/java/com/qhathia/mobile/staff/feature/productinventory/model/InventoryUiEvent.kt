package com.qhathia.mobile.staff.feature.productinventory.model

import com.qhathia.mobile.staff.core.domain.model.Availability
import com.qhathia.mobile.staff.core.domain.model.DiscountDraft

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

    /** A row's actions menu was opened; looks up whether that product has a discount. */
    data class RowActionsOpened(val productId: String) : InventoryUiEvent

    /**
     * "Registrar descuento" / "Editar descuento" in a row's actions menu; opens the discount
     * dialog for that product.
     */
    data class EditDiscountClicked(val productId: String) : InventoryUiEvent

    /** The merchant changed the mode or the value in the discount dialog. */
    data class DiscountDraftChanged(val draft: DiscountDraft) : InventoryUiEvent

    /** "Aplicar" in the discount dialog. There is no form here, so this saves the discount. */
    data object DiscountApplied : InventoryUiEvent
    data object DiscountDismissed : InventoryUiEvent

    /** "Eliminar producto" in a row's actions menu; asks for confirmation before anything is sent. */
    data class DeleteProductClicked(val productId: String) : InventoryUiEvent

    /** "Eliminar" in the confirmation dialog. */
    data object DeleteConfirmed : InventoryUiEvent

    /** "Cancelar" or a tap outside the confirmation dialog. Ignored while the delete is in flight. */
    data object DeleteDismissed : InventoryUiEvent

    /**
     * "Ver producto" on the snackbar shown after a registration; brings the row of the product that
     * was just created into view, dropping the search and filters if they hide it.
     */
    data class ViewRegisteredProductClicked(val productId: String) : InventoryUiEvent
}
