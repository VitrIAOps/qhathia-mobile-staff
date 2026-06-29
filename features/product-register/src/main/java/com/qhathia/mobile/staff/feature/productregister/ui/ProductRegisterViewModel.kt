package com.qhathia.mobile.staff.feature.productregister.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qhathia.mobile.staff.core.common.validation.isValidProductName
import com.qhathia.mobile.staff.core.common.validation.parsePrice
import com.qhathia.mobile.staff.core.common.validation.parseQuantity
import com.qhathia.mobile.staff.core.domain.model.NewProduct
import com.qhathia.mobile.staff.core.domain.model.RegisterProductError
import com.qhathia.mobile.staff.core.domain.model.RegisterProductException
import com.qhathia.mobile.staff.core.domain.model.SelectedImage
import com.qhathia.mobile.staff.core.domain.usecase.GetCategoriesUseCase
import com.qhathia.mobile.staff.core.domain.usecase.RegisterProductUseCase
import com.qhathia.mobile.staff.feature.productregister.R
import com.qhathia.mobile.staff.feature.productregister.model.ProductRegisterUiEffect
import com.qhathia.mobile.staff.feature.productregister.model.ProductRegisterUiEvent
import com.qhathia.mobile.staff.feature.productregister.model.ProductRegisterUiState
import com.qhathia.mobile.staff.feature.productregister.model.parseTags
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the product registration screen: loads categories on init, validates each field as the
 * merchant edits it, and on submit orchestrates registration through [RegisterProductUseCase],
 * branching the result into success / plan-limit / error effects. Tags are entered as free text
 * (comma-separated). A dirty form routes cancel/back through the discard-confirmation dialog.
 */
@HiltViewModel
class ProductRegisterViewModel @Inject constructor(
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val registerProductUseCase: RegisterProductUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductRegisterUiState())
    val uiState: StateFlow<ProductRegisterUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<ProductRegisterUiEffect>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val uiEffect: SharedFlow<ProductRegisterUiEffect> = _uiEffect.asSharedFlow()

    init {
        loadCategories()
    }

    fun onEvent(event: ProductRegisterUiEvent) {
        when (event) {
            is ProductRegisterUiEvent.NameChanged -> onNameChanged(event.value)
            is ProductRegisterUiEvent.PriceChanged -> onPriceChanged(event.value)
            is ProductRegisterUiEvent.QuantityChanged -> onQuantityChanged(event.value)
            is ProductRegisterUiEvent.DescriptionChanged ->
                _uiState.update { it.copy(description = event.value, isDirty = true) }
            // AI description generation is out of scope for now — the button is present but inert.
            ProductRegisterUiEvent.GenerateDescriptionWithAi -> Unit
            is ProductRegisterUiEvent.TagsChanged ->
                _uiState.update { it.copy(tags = event.value, isDirty = true) }
            is ProductRegisterUiEvent.SelectCategory ->
                _uiState.update { it.copy(selectedCategoryId = event.categoryId, isDirty = true) }
            is ProductRegisterUiEvent.ImagesPicked -> onImagesPicked(event.images)
            is ProductRegisterUiEvent.RemoveImage -> onRemoveImage(event.uri)
            ProductRegisterUiEvent.Submit -> submit()
            ProductRegisterUiEvent.CancelRequested -> onCancelRequested()
            ProductRegisterUiEvent.ConfirmDiscard -> {
                _uiState.update { it.copy(showDiscardDialog = false) }
                emit(ProductRegisterUiEffect.NavigateBackCancelled)
            }
            ProductRegisterUiEvent.DismissDiscard ->
                _uiState.update { it.copy(showDiscardDialog = false) }
            ProductRegisterUiEvent.RetryCatalog -> loadCategories()
        }
    }

    private fun onNameChanged(value: String) {
        val error = if (value.isNotBlank() && !isValidProductName(value)) R.string.register_error_name else null
        _uiState.update { it.copy(name = value, nameError = error, isDirty = true) }
    }

    private fun onPriceChanged(value: String) {
        val error = if (value.isNotBlank() && parsePrice(value) == null) R.string.register_error_price else null
        _uiState.update { it.copy(price = value, priceError = error, isDirty = true) }
    }

    private fun onQuantityChanged(value: String) {
        val error = if (value.isNotBlank() && parseQuantity(value) == null) {
            R.string.register_error_quantity
        } else {
            null
        }
        _uiState.update { it.copy(quantity = value, quantityError = error, isDirty = true) }
    }

    private fun onImagesPicked(picked: List<SelectedImage>) {
        val (valid, invalid) = picked.partition { it.mimeType.lowercase() in SUPPORTED_MIME_TYPES }
        if (valid.isNotEmpty()) {
            _uiState.update { state ->
                val existingUris = state.images.map { it.uri }.toSet()
                val merged = state.images + valid.filter { it.uri !in existingUris }
                state.copy(images = merged.toImmutableList(), isDirty = true)
            }
        }
        if (invalid.isNotEmpty()) {
            emit(ProductRegisterUiEffect.ShowError(R.string.register_error_image_format))
        }
    }

    private fun onRemoveImage(uri: String) {
        _uiState.update { state ->
            state.copy(images = state.images.filterNot { it.uri == uri }.toImmutableList(), isDirty = true)
        }
    }

    private fun onCancelRequested() {
        if (_uiState.value.isDirty) {
            _uiState.update { it.copy(showDiscardDialog = true) }
        } else {
            emit(ProductRegisterUiEffect.NavigateBackCancelled)
        }
    }

    private fun submit() {
        val current = _uiState.value
        if (!current.isSubmitEnabled) return
        val price = parsePrice(current.price) ?: return
        val quantity = parseQuantity(current.quantity) ?: return
        val categoryId = current.selectedCategoryId ?: return

        val product = NewProduct(
            name = current.name.trim(),
            price = price,
            quantity = quantity,
            description = current.description.trim().ifBlank { null },
            categoryId = categoryId,
            tags = parseTags(current.tags),
            images = current.images.toList(),
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            registerProductUseCase(product)
                .onSuccess {
                    _uiState.update { it.copy(isSubmitting = false) }
                    emit(ProductRegisterUiEffect.NavigateBackWithSuccess)
                }
                .onFailure { throwable ->
                    _uiState.update { it.copy(isSubmitting = false) }
                    val error = (throwable as? RegisterProductException)?.error
                    if (error == RegisterProductError.PlanLimitReached) {
                        emit(ProductRegisterUiEffect.NavigateBackWithPlanLimit)
                    } else {
                        emit(ProductRegisterUiEffect.ShowError(error.toMessageRes()))
                    }
                }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCategoriesLoading = true, isCategoriesError = false) }
            val categories = getCategoriesUseCase()
            _uiState.update { state ->
                state.copy(
                    isCategoriesLoading = false,
                    categories = categories.getOrDefault(emptyList()).toImmutableList(),
                    isCategoriesError = categories.isFailure,
                )
            }
        }
    }

    private fun emit(effect: ProductRegisterUiEffect) {
        viewModelScope.launch { _uiEffect.emit(effect) }
    }

    private fun RegisterProductError?.toMessageRes(): Int = when (this) {
        RegisterProductError.ImageTooLargeOrInvalid -> R.string.register_error_image
        RegisterProductError.Network -> R.string.register_error_network
        RegisterProductError.InvalidInput -> R.string.register_error_validation
        RegisterProductError.PlanLimitReached, is RegisterProductError.Unknown, null ->
            R.string.register_error_unknown
    }

    private companion object {
        val SUPPORTED_MIME_TYPES = setOf("image/jpeg", "image/jpg", "image/png")
    }
}
