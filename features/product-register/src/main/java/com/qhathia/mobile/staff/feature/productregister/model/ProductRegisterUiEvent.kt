package com.qhathia.mobile.staff.feature.productregister.model

import com.qhathia.mobile.staff.core.domain.model.SelectedImage

/** User intents from the product registration screen. */
sealed interface ProductRegisterUiEvent {
    data class NameChanged(val value: String) : ProductRegisterUiEvent
    data class PriceChanged(val value: String) : ProductRegisterUiEvent
    data class QuantityChanged(val value: String) : ProductRegisterUiEvent
    data class DescriptionChanged(val value: String) : ProductRegisterUiEvent
    data object GenerateDescriptionWithAi : ProductRegisterUiEvent
    data object ConfirmAiReplace : ProductRegisterUiEvent
    data object DismissAiReplace : ProductRegisterUiEvent
    data class TagsChanged(val value: String) : ProductRegisterUiEvent
    data class SelectCategory(val categoryId: String) : ProductRegisterUiEvent
    data class ImagesPicked(val images: List<SelectedImage>) : ProductRegisterUiEvent
    data class RemoveImage(val uri: String) : ProductRegisterUiEvent
    data object Submit : ProductRegisterUiEvent
    data object CancelRequested : ProductRegisterUiEvent
    data object ConfirmDiscard : ProductRegisterUiEvent
    data object DismissDiscard : ProductRegisterUiEvent
    data object RetryCatalog : ProductRegisterUiEvent
}
