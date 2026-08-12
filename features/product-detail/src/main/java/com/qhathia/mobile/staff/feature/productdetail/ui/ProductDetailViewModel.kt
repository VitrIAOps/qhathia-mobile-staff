package com.qhathia.mobile.staff.feature.productdetail.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qhathia.mobile.staff.core.common.validation.isValidProductName
import com.qhathia.mobile.staff.core.common.validation.parsePrice
import com.qhathia.mobile.staff.core.common.validation.parseQuantity
import com.qhathia.mobile.staff.core.domain.model.ProductDetail
import com.qhathia.mobile.staff.core.domain.model.ProductUpdate
import com.qhathia.mobile.staff.core.domain.model.UpdateProductError
import com.qhathia.mobile.staff.core.domain.model.UpdateProductException
import com.qhathia.mobile.staff.core.domain.usecase.GetCategoriesUseCase
import com.qhathia.mobile.staff.core.domain.usecase.GetProductDetailUseCase
import com.qhathia.mobile.staff.core.domain.usecase.OptimizeDescriptionUseCase
import com.qhathia.mobile.staff.core.domain.usecase.UpdateProductUseCase
import com.qhathia.mobile.staff.feature.productdetail.R
import com.qhathia.mobile.staff.feature.productdetail.model.ProductDetailUiEffect
import com.qhathia.mobile.staff.feature.productdetail.model.ProductDetailUiEvent
import com.qhathia.mobile.staff.feature.productdetail.model.ProductDetailUiState
import com.qhathia.mobile.staff.feature.productdetail.model.parseTags
import com.qhathia.mobile.staff.feature.productdetail.navigation.PRODUCT_DETAIL_ARG_ID
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.async
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
 * Drives the product detail/edit screen: loads the product and the category catalog on init, seeds
 * the form from the loaded product, validates each field as the merchant edits it, and on save
 * replaces the product through [UpdateProductUseCase]. A dirty form routes cancel/back through the
 * discard-confirmation dialog.
 *
 * The product id arrives as a route argument via [SavedStateHandle], so the screen never receives
 * the product itself across the navigation boundary.
 */
@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getProductDetailUseCase: GetProductDetailUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val updateProductUseCase: UpdateProductUseCase,
    private val optimizeDescriptionUseCase: OptimizeDescriptionUseCase,
) : ViewModel() {

    private val productId: String = checkNotNull(savedStateHandle[PRODUCT_DETAIL_ARG_ID]) {
        "ProductDetailViewModel requires a '$PRODUCT_DETAIL_ARG_ID' route argument"
    }

    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<ProductDetailUiEffect>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val uiEffect: SharedFlow<ProductDetailUiEffect> = _uiEffect.asSharedFlow()

    init {
        load()
    }

    fun onEvent(event: ProductDetailUiEvent) {
        when (event) {
            ProductDetailUiEvent.Retry -> load()
            ProductDetailUiEvent.RetryCatalog -> loadCategories()
            is ProductDetailUiEvent.NameChanged -> onNameChanged(event.value)
            is ProductDetailUiEvent.PriceChanged -> onPriceChanged(event.value)
            is ProductDetailUiEvent.QuantityChanged -> onQuantityChanged(event.value)
            is ProductDetailUiEvent.DescriptionChanged ->
                _uiState.update { it.copy(description = event.value, isDirty = true) }
            is ProductDetailUiEvent.TagsChanged ->
                _uiState.update { it.copy(tags = event.value, isDirty = true) }
            is ProductDetailUiEvent.SelectCategory ->
                _uiState.update { it.copy(selectedCategoryId = event.categoryId, isDirty = true) }
            ProductDetailUiEvent.OptimizeDescriptionWithAi -> optimizeDescription()
            ProductDetailUiEvent.Save -> save()
            ProductDetailUiEvent.CancelRequested -> onCancelRequested()
            ProductDetailUiEvent.ConfirmDiscard -> {
                _uiState.update { it.copy(showDiscardDialog = false) }
                emit(ProductDetailUiEffect.NavigateBackCancelled)
            }
            ProductDetailUiEvent.DismissDiscard ->
                _uiState.update { it.copy(showDiscardDialog = false) }
        }
    }

    /**
     * Loads the product and the catalog concurrently. A catalog failure is non-fatal — it only
     * disables the category chips, whose own retry is wired separately — but a product failure puts
     * the whole screen in its error state.
     */
    private fun load() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, isError = false, isCategoriesLoading = true, isCategoriesError = false)
            }
            val categoriesDeferred = async { getCategoriesUseCase() }
            val productResult = getProductDetailUseCase(productId)
            val categoriesResult = categoriesDeferred.await()

            _uiState.update { state ->
                val withCatalog = state.copy(
                    isCategoriesLoading = false,
                    categories = categoriesResult.getOrDefault(emptyList()).toImmutableList(),
                    isCategoriesError = categoriesResult.isFailure,
                )
                productResult.fold(
                    onSuccess = { product -> withCatalog.seededFrom(product) },
                    onFailure = { withCatalog.copy(isLoading = false, isError = true) },
                )
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

    /**
     * Seeds the form from the loaded product. A null stock renders as an empty quantity field, which
     * the merchant must fill before saving — the backend derives availability from it either way.
     */
    private fun ProductDetailUiState.seededFrom(product: ProductDetail) = copy(
        name = product.name,
        price = formatPrice(product.price),
        quantity = product.stock?.toString().orEmpty(),
        description = product.description.orEmpty(),
        tags = product.tags.joinToString(", "),
        selectedCategoryId = product.categoryId,
        imageUrls = product.imageUrls.toImmutableList(),
        allowDiscountInquiry = product.allowDiscountInquiry,
        position = product.position,
        nameError = null,
        priceError = null,
        quantityError = null,
        isLoading = false,
        isError = false,
        isDirty = false,
    )

    private fun onNameChanged(value: String) {
        val error = if (value.isNotBlank() && !isValidProductName(value)) R.string.detail_error_name else null
        _uiState.update { it.copy(name = value, nameError = error, isDirty = true) }
    }

    private fun onPriceChanged(value: String) {
        val error = if (value.isNotBlank() && parsePrice(value) == null) R.string.detail_error_price else null
        _uiState.update { it.copy(price = value, priceError = error, isDirty = true) }
    }

    private fun onQuantityChanged(value: String) {
        val error = if (value.isNotBlank() && parseQuantity(value) == null) {
            R.string.detail_error_quantity
        } else {
            null
        }
        _uiState.update { it.copy(quantity = value, quantityError = error, isDirty = true) }
    }

    private fun onCancelRequested() {
        if (_uiState.value.isDirty) {
            _uiState.update { it.copy(showDiscardDialog = true) }
        } else {
            emit(ProductDetailUiEffect.NavigateBackCancelled)
        }
    }

    private fun optimizeDescription() {
        val current = _uiState.value
        if (!current.isAiActionEnabled) return
        viewModelScope.launch {
            _uiState.update { it.copy(isAiLoading = true) }
            optimizeDescriptionUseCase(current.description)
                .onSuccess { optimized ->
                    _uiState.update { it.copy(description = optimized, isDirty = true) }
                }
                .onFailure { emit(ProductDetailUiEffect.ShowError(R.string.detail_error_ai)) }
            _uiState.update { it.copy(isAiLoading = false) }
        }
    }

    private fun save() {
        val current = _uiState.value
        if (!current.isSaveEnabled) return
        val price = parsePrice(current.price) ?: return
        val quantity = parseQuantity(current.quantity) ?: return
        val categoryId = current.selectedCategoryId ?: return

        // The endpoint is a full replace, so the two fields the form does not expose travel back
        // unchanged rather than defaulting.
        val product = ProductUpdate(
            name = current.name.trim(),
            price = price,
            stock = quantity,
            description = current.description.trim().ifBlank { null },
            categoryId = categoryId,
            tags = parseTags(current.tags),
            allowDiscountInquiry = current.allowDiscountInquiry,
            position = current.position,
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            updateProductUseCase(productId, product)
                .onSuccess {
                    _uiState.update { it.copy(isSaving = false, isDirty = false) }
                    emit(ProductDetailUiEffect.NavigateBackSaved)
                }
                .onFailure { throwable ->
                    _uiState.update { it.copy(isSaving = false) }
                    val error = (throwable as? UpdateProductException)?.error
                    emit(ProductDetailUiEffect.ShowError(error.toMessageRes()))
                }
        }
    }

    private fun emit(effect: ProductDetailUiEffect) {
        viewModelScope.launch { _uiEffect.emit(effect) }
    }

    private fun UpdateProductError?.toMessageRes(): Int = when (this) {
        UpdateProductError.NotFound -> R.string.detail_error_not_found
        UpdateProductError.Network -> R.string.detail_error_network
        UpdateProductError.InvalidInput -> R.string.detail_error_validation
        is UpdateProductError.Unknown, null -> R.string.detail_error_unknown
    }
}

/** Renders the price without a trailing `.0` for whole amounts, matching how the merchant types it. */
private fun formatPrice(price: Double): String =
    if (price % 1.0 == 0.0) price.toLong().toString() else price.toString()
