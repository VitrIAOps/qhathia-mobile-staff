package com.qhathia.mobile.staff.feature.productregister.model

import com.qhathia.mobile.staff.core.domain.model.DiscountDraft
import com.qhathia.mobile.staff.core.domain.model.SelectedImage

/** User intents from the product registration screen. */
sealed interface ProductRegisterUiEvent {
    data class NameChanged(val value: String) : ProductRegisterUiEvent
    data class PriceChanged(val value: String) : ProductRegisterUiEvent
    data class QuantityChanged(val value: String) : ProductRegisterUiEvent
    /** The "Descuento" switch: on opens the discount dialog, off removes the applied discount. */
    data class DiscountToggled(val enabled: Boolean) : ProductRegisterUiEvent

    /** The applied discount's summary was tapped; reopens the dialog to change it. */
    data object DiscountSummaryClicked : ProductRegisterUiEvent

    /** The merchant changed the mode or the value in the discount dialog. */
    data class DiscountDraftChanged(val draft: DiscountDraft) : ProductRegisterUiEvent
    data object DiscountApplied : ProductRegisterUiEvent
    data object DiscountDismissed : ProductRegisterUiEvent
    data class DescriptionChanged(val value: String) : ProductRegisterUiEvent
    data object GenerateDescriptionWithAi : ProductRegisterUiEvent
    data object ConfirmAiReplace : ProductRegisterUiEvent
    data object DismissAiReplace : ProductRegisterUiEvent
    data class TagsChanged(val value: String) : ProductRegisterUiEvent
    data class SelectCategory(val categoryId: String) : ProductRegisterUiEvent
    data class ImagesPicked(val images: List<SelectedImage>) : ProductRegisterUiEvent
    data class RemoveImage(val uri: String) : ProductRegisterUiEvent

    /** "Vista previa" was tapped; opens the preview of the form as it stands. */
    data object PreviewRequested : ProductRegisterUiEvent
    data object PreviewDismissed : ProductRegisterUiEvent
    data object Submit : ProductRegisterUiEvent
    data object CancelRequested : ProductRegisterUiEvent

    /** The header bell — leaves for Solicitudes through the same discard guard as back. */
    data object OrderRequestsRequested : ProductRegisterUiEvent
    data object ConfirmDiscard : ProductRegisterUiEvent
    data object DismissDiscard : ProductRegisterUiEvent
    data object RetryCatalog : ProductRegisterUiEvent
}
