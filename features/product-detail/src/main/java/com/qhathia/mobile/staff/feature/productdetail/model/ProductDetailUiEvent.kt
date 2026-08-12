package com.qhathia.mobile.staff.feature.productdetail.model

/** Everything the product detail/edit screen can report to its ViewModel. */
sealed interface ProductDetailUiEvent {
    /** Reloads the product after a failed load. */
    data object Retry : ProductDetailUiEvent

    /** Reloads the category catalog after a failed load. */
    data object RetryCatalog : ProductDetailUiEvent

    data class NameChanged(val value: String) : ProductDetailUiEvent
    data class PriceChanged(val value: String) : ProductDetailUiEvent
    data class QuantityChanged(val value: String) : ProductDetailUiEvent
    data class DescriptionChanged(val value: String) : ProductDetailUiEvent
    data class TagsChanged(val value: String) : ProductDetailUiEvent
    data class SelectCategory(val categoryId: String) : ProductDetailUiEvent

    /** Rewrites the existing description through the LLM, preserving its facts. */
    data object OptimizeDescriptionWithAi : ProductDetailUiEvent

    data object Save : ProductDetailUiEvent

    /** Back or cancel — routed through the discard dialog when the form is dirty. */
    data object CancelRequested : ProductDetailUiEvent
    data object ConfirmDiscard : ProductDetailUiEvent
    data object DismissDiscard : ProductDetailUiEvent
}
