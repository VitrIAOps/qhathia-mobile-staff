package com.qhathia.mobile.staff.feature.productinventory.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qhathia.mobile.staff.core.domain.model.Availability
import com.qhathia.mobile.staff.core.domain.model.Product
import com.qhathia.mobile.staff.core.domain.usecase.GetCategoriesUseCase
import com.qhathia.mobile.staff.core.domain.usecase.GetProductsUseCase
import com.qhathia.mobile.staff.feature.productinventory.R
import com.qhathia.mobile.staff.feature.productinventory.model.InventoryUiEffect
import com.qhathia.mobile.staff.feature.productinventory.model.InventoryUiEvent
import com.qhathia.mobile.staff.feature.productinventory.model.InventoryUiState
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
 * Drives the inventory screen. Category and availability filters are applied server-side (each
 * change re-queries products); the name search filters the already-loaded list client-side. The
 * product count is derived from the visible list in [InventoryUiState].
 */
@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val getProductsUseCase: GetProductsUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryUiState())
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<InventoryUiEffect>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val uiEffect: SharedFlow<InventoryUiEffect> = _uiEffect.asSharedFlow()

    init {
        onEvent(InventoryUiEvent.Load)
    }

    fun onEvent(event: InventoryUiEvent) {
        when (event) {
            InventoryUiEvent.Load -> {
                loadCategories()
                loadProducts()
            }
            InventoryUiEvent.Retry -> loadProducts()
            is InventoryUiEvent.SelectCategory -> {
                _uiState.update { it.copy(selectedCategoryId = event.categoryId) }
                loadProducts()
            }
            is InventoryUiEvent.SelectAvailability -> {
                _uiState.update { it.copy(selectedAvailability = event.availability) }
                loadProducts()
            }
            InventoryUiEvent.ClearAvailability -> {
                _uiState.update { it.copy(selectedAvailability = null) }
                loadProducts()
            }
            is InventoryUiEvent.Search ->
                _uiState.update { state ->
                    state.copy(
                        searchQuery = event.query,
                        visibleProducts = state.loadedProducts.applySearch(event.query),
                    )
                }
            InventoryUiEvent.RegisterProductClicked ->
                viewModelScope.launch { _uiEffect.emit(InventoryUiEffect.NavigateToRegister) }
        }
    }

    /** Re-queries products with the active filters — used after a successful product registration. */
    fun refresh() = loadProducts()

    private fun loadProducts() {
        val current = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isError = false) }
            getProductsUseCase(current.selectedCategoryId, current.selectedAvailability)
                .onSuccess { products ->
                    _uiState.update { state ->
                        state.copy(
                            loadedProducts = products.toImmutableList(),
                            visibleProducts = products.applySearch(state.searchQuery),
                            isLoading = false,
                            isError = false,
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoading = false, isError = true) }
                    _uiEffect.emit(InventoryUiEffect.ShowError(R.string.inventory_error))
                }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            // Categories degrade gracefully: a failure leaves the "Todas" option and the product
            // list intact rather than blocking inventory browsing.
            getCategoriesUseCase().onSuccess { categories ->
                _uiState.update { it.copy(categories = categories.toImmutableList()) }
            }
        }
    }

    private fun List<Product>.applySearch(query: String) =
        if (query.isBlank()) {
            toImmutableList()
        } else {
            filter { it.name.contains(query.trim(), ignoreCase = true) }.toImmutableList()
        }
}
