package com.qhathia.mobile.staff.feature.productinventory.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qhathia.mobile.staff.core.domain.model.Availability
import com.qhathia.mobile.staff.core.domain.model.Discount
import com.qhathia.mobile.staff.core.domain.model.DiscountDraft
import com.qhathia.mobile.staff.core.domain.model.Product
import com.qhathia.mobile.staff.core.domain.model.UpdateProductError
import com.qhathia.mobile.staff.core.domain.model.UpdateProductException
import com.qhathia.mobile.staff.core.domain.usecase.DeleteProductUseCase
import com.qhathia.mobile.staff.core.domain.usecase.GetCategoriesUseCase
import com.qhathia.mobile.staff.core.domain.usecase.GetProductDiscountUseCase
import com.qhathia.mobile.staff.core.domain.usecase.GetProductsUseCase
import com.qhathia.mobile.staff.core.domain.usecase.SaveProductDiscountUseCase
import com.qhathia.mobile.staff.feature.productinventory.R
import com.qhathia.mobile.staff.feature.productinventory.model.InventoryUiEffect
import com.qhathia.mobile.staff.feature.productinventory.model.InventoryUiEvent
import com.qhathia.mobile.staff.feature.productinventory.model.InventoryUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.Job
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
 *
 * A row's actions menu edits that product's discount in place. Opening the menu looks up whether
 * the product has one, so the option reads "Registrar descuento" or "Editar descuento"; choosing
 * it opens the dialog prefilled, and applying saves it straight away. The same menu deletes the
 * product, after a confirmation, since the delete cannot be undone.
 */
@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val getProductsUseCase: GetProductsUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val getProductDiscountUseCase: GetProductDiscountUseCase,
    private val saveProductDiscountUseCase: SaveProductDiscountUseCase,
    private val deleteProductUseCase: DeleteProductUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryUiState())
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<InventoryUiEffect>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val uiEffect: SharedFlow<InventoryUiEffect> = _uiEffect.asSharedFlow()

    /** A product to scroll to once the load in flight delivers the list it should be in. */
    private var pendingRevealProductId: String? = null

    /** The products request in flight; a newer one replaces it so a stale response never lands. */
    private var productsJob: Job? = null

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
            is InventoryUiEvent.ProductClicked ->
                viewModelScope.launch {
                    _uiEffect.emit(InventoryUiEffect.NavigateToProductDetail(event.productId))
                }
            is InventoryUiEvent.RowActionsOpened -> lookUpDiscount(event.productId)
            is InventoryUiEvent.EditDiscountClicked -> openDiscountEditor(event.productId)
            is InventoryUiEvent.DiscountDraftChanged ->
                _uiState.update {
                    if (it.discountDraft == null || it.isDiscountSaving) it else it.copy(discountDraft = event.draft)
                }
            InventoryUiEvent.DiscountApplied -> saveDiscount()
            InventoryUiEvent.DiscountDismissed ->
                _uiState.update {
                    if (it.isDiscountSaving) it else it.copy(discountDraft = null, discountProductId = null)
                }
            is InventoryUiEvent.ViewRegisteredProductClicked -> revealProduct(event.productId)
            is InventoryUiEvent.DeleteProductClicked ->
                _uiState.update { state ->
                    if (state.isDeleting) return@update state
                    val product = state.loadedProducts.firstOrNull { it.id == event.productId }
                    state.copy(deleteCandidate = product ?: return@update state)
                }
            InventoryUiEvent.DeleteConfirmed -> deleteProduct()
            InventoryUiEvent.DeleteDismissed ->
                _uiState.update { if (it.isDeleting) it else it.copy(deleteCandidate = null, pendingOrders = null) }
        }
    }

    /**
     * Deletes the product awaiting confirmation. The row is removed only once the server agrees —
     * a 404 counts, the product is already gone — and it is dropped locally instead of re-querying,
     * since the answer already says what the list should look like. A product in pending orders keeps
     * the confirmation open with the server's explanation; confirming again forces the delete. Any
     * other failure keeps it.
     */
    private fun deleteProduct() {
        val current = _uiState.value
        if (current.isDeleting) return
        val productId = current.deleteCandidate?.id ?: return
        val force = current.pendingOrders != null
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true) }
            val result = deleteProductUseCase(productId, force)
            val error = (result.exceptionOrNull() as? UpdateProductException)?.error
            val alreadyGone = error == UpdateProductError.NotFound
            if (error is UpdateProductError.HasPendingOrders && !force) {
                _uiState.update { it.copy(isDeleting = false, pendingOrders = error) }
                return@launch
            }
            if (result.isSuccess || alreadyGone) {
                _uiState.update {
                    it.withoutProduct(productId).copy(isDeleting = false, deleteCandidate = null, pendingOrders = null)
                }
                _uiEffect.emit(
                    InventoryUiEffect.ShowMessage(
                        if (alreadyGone) R.string.inventory_product_already_deleted else R.string.inventory_product_deleted,
                    ),
                )
            } else {
                _uiState.update { it.copy(isDeleting = false, deleteCandidate = null, pendingOrders = null) }
                _uiEffect.emit(InventoryUiEffect.ShowError(R.string.inventory_delete_error))
            }
        }
    }

    private fun InventoryUiState.withoutProduct(productId: String) = copy(
        loadedProducts = loadedProducts.filterNot { it.id == productId }.toImmutableList(),
        visibleProducts = visibleProducts.filterNot { it.id == productId }.toImmutableList(),
        knownDiscounts = knownDiscounts.toPersistentMap().remove(productId),
    )

    /**
     * Brings the row of [productId] into view. A product hidden only by the search is revealed by
     * clearing it; one the category/availability filters exclude needs the unfiltered list, so they
     * are dropped and the list reloaded. While a load is in flight the request waits for it.
     */
    private fun revealProduct(productId: String) {
        val current = _uiState.value
        if (current.isLoading) {
            pendingRevealProductId = productId
            return
        }
        val visibleIndex = current.visibleProducts.indexOfFirst { it.id == productId }
        val loadedIndex = current.loadedProducts.indexOfFirst { it.id == productId }
        when {
            visibleIndex >= 0 ->
                viewModelScope.launch { _uiEffect.emit(InventoryUiEffect.ScrollToProduct(visibleIndex)) }
            loadedIndex >= 0 -> {
                _uiState.update { it.copy(searchQuery = "", visibleProducts = it.loadedProducts) }
                viewModelScope.launch { _uiEffect.emit(InventoryUiEffect.ScrollToProduct(loadedIndex)) }
            }
            current.selectedCategoryId != null || current.selectedAvailability != null -> {
                // The reload runs without filters, so it cannot land here again.
                pendingRevealProductId = productId
                _uiState.update {
                    it.copy(selectedCategoryId = null, selectedAvailability = null, searchQuery = "")
                }
                loadProducts()
            }
            else ->
                viewModelScope.launch {
                    _uiEffect.emit(
                        InventoryUiEffect.ShowMessage(R.string.inventory_register_view_product_not_found),
                    )
                }
        }
    }

    /**
     * Learns whether the product has a discount, once, so its menu can offer to register or to edit
     * one. A failure is silent: the menu keeps its default wording and the option still works,
     * loading the discount itself when chosen.
     */
    private fun lookUpDiscount(productId: String) {
        val current = _uiState.value
        if (current.knownDiscounts.containsKey(productId) || current.discountLookupProductId == productId) return
        viewModelScope.launch {
            _uiState.update { it.copy(discountLookupProductId = productId) }
            val result = getProductDiscountUseCase(productId)
            _uiState.update { state ->
                val settled = if (state.discountLookupProductId == productId) {
                    state.copy(discountLookupProductId = null)
                } else {
                    state
                }
                result.fold(
                    onSuccess = { settled.withKnownDiscount(productId, it?.discount) },
                    onFailure = { settled },
                )
            }
        }
    }

    private fun InventoryUiState.withKnownDiscount(productId: String, discount: Discount?) =
        copy(knownDiscounts = knownDiscounts.toPersistentMap().put(productId, discount))

    /**
     * Opens the dialog prefilled with the product's current discount (or empty when it has none),
     * previewing against the price shown on the row. The discount is loaded first unless opening the
     * menu already looked it up.
     */
    private fun openDiscountEditor(productId: String) {
        val current = _uiState.value
        if (current.isDiscountLoading || current.discountDraft != null) return
        val product = current.loadedProducts.firstOrNull { it.id == productId } ?: return
        if (current.knownDiscounts.containsKey(productId)) {
            _uiState.update {
                it.copy(
                    discountProductId = productId,
                    discountDraft = DiscountDraft.from(product.price, it.knownDiscounts[productId]),
                )
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isDiscountLoading = true) }
            getProductDiscountUseCase(productId)
                .onSuccess { stored ->
                    _uiState.update {
                        it.withKnownDiscount(productId, stored?.discount).copy(
                            isDiscountLoading = false,
                            discountProductId = productId,
                            discountDraft = DiscountDraft.from(product.price, stored?.discount),
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isDiscountLoading = false) }
                    _uiEffect.emit(InventoryUiEffect.ShowError(R.string.inventory_discount_load_error))
                }
        }
    }

    private fun saveDiscount() {
        val current = _uiState.value
        if (current.isDiscountSaving) return
        val productId = current.discountProductId ?: return
        val discount = current.discountDraft?.toDiscount() ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isDiscountSaving = true) }
            saveProductDiscountUseCase(productId, discount)
                .onSuccess {
                    _uiState.update {
                        it.withKnownDiscount(productId, discount)
                            .copy(isDiscountSaving = false, discountDraft = null, discountProductId = null)
                    }
                    _uiEffect.emit(InventoryUiEffect.ShowMessage(R.string.inventory_discount_updated))
                    // The row's discounted price comes from the server, so re-read it rather than
                    // computing it here.
                    fetchProducts(quiet = true)
                }
                .onFailure {
                    // The dialog stays open with what was typed so the merchant can try again. A
                    // failed replace may have removed the old discount, so what is known is dropped.
                    _uiState.update {
                        it.copy(
                            isDiscountSaving = false,
                            knownDiscounts = it.knownDiscounts.toPersistentMap().remove(productId),
                        )
                    }
                    _uiEffect.emit(InventoryUiEffect.ShowError(R.string.inventory_discount_save_error))
                }
        }
    }

    /** Re-queries products with the active filters — used after a product is registered or edited. */
    fun refresh() = loadProducts()

    private fun loadProducts() = fetchProducts(quiet = false)

    /**
     * Queries products with the active filters and applies the search to them.
     *
     * A [quiet] fetch keeps the current rows on screen instead of showing the loading state, and on
     * failure leaves them as they are — it only refreshes what is already shown. If it replaces a
     * regular load still in flight, it takes over that load's loading and error handling.
     */
    private fun fetchProducts(quiet: Boolean) {
        val current = _uiState.value
        val visible = !quiet || current.isLoading
        productsJob?.cancel()
        productsJob = viewModelScope.launch {
            if (visible) {
                // A reload follows edits made elsewhere (detail, registration), so what was learned
                // about each product's discount may be stale.
                _uiState.update { it.copy(isLoading = true, isError = false, knownDiscounts = persistentMapOf()) }
            }
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
                    pendingRevealProductId?.let { productId ->
                        pendingRevealProductId = null
                        revealProduct(productId)
                    }
                }
                .onFailure {
                    if (!visible) return@onFailure
                    pendingRevealProductId = null
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
