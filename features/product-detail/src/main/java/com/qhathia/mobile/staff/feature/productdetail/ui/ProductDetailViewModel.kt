package com.qhathia.mobile.staff.feature.productdetail.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qhathia.mobile.staff.core.common.validation.isValidProductName
import com.qhathia.mobile.staff.core.common.validation.parsePrice
import com.qhathia.mobile.staff.core.common.validation.parseQuantity
import com.qhathia.mobile.staff.core.domain.model.DiscountDraft
import com.qhathia.mobile.staff.core.domain.model.ProductDetail
import com.qhathia.mobile.staff.core.domain.model.ProductUpdate
import com.qhathia.mobile.staff.core.domain.model.SelectedImage
import com.qhathia.mobile.staff.core.domain.model.UpdateProductError
import com.qhathia.mobile.staff.core.domain.model.UpdateProductException
import com.qhathia.mobile.staff.core.domain.usecase.DeleteProductUseCase
import com.qhathia.mobile.staff.core.domain.usecase.GetCategoriesUseCase
import com.qhathia.mobile.staff.core.domain.usecase.GetProductDetailUseCase
import com.qhathia.mobile.staff.core.domain.usecase.GetProductDiscountUseCase
import com.qhathia.mobile.staff.core.domain.usecase.OptimizeDescriptionUseCase
import com.qhathia.mobile.staff.core.domain.usecase.RemoveProductDiscountUseCase
import com.qhathia.mobile.staff.core.domain.usecase.SaveProductDiscountUseCase
import com.qhathia.mobile.staff.core.domain.usecase.UpdateProductPhotosUseCase
import com.qhathia.mobile.staff.core.domain.usecase.UpdateProductUseCase
import com.qhathia.mobile.staff.feature.productdetail.R
import com.qhathia.mobile.staff.feature.productdetail.model.FormPhoto
import com.qhathia.mobile.staff.feature.productdetail.model.MAX_PHOTOS
import com.qhathia.mobile.staff.feature.productdetail.model.ProductDetailUiEffect
import com.qhathia.mobile.staff.feature.productdetail.model.ProductDetailUiEvent
import com.qhathia.mobile.staff.feature.productdetail.model.ProductDetailUiState
import com.qhathia.mobile.staff.feature.productdetail.model.ServerPricing
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
 * The product's discount is loaded alongside it and edited as one more form field: applying or
 * removing it only changes the form, and the change is sent after the product update when the
 * merchant saves.
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
    private val getProductDiscountUseCase: GetProductDiscountUseCase,
    private val saveProductDiscountUseCase: SaveProductDiscountUseCase,
    private val removeProductDiscountUseCase: RemoveProductDiscountUseCase,
    private val deleteProductUseCase: DeleteProductUseCase,
    private val updateProductPhotosUseCase: UpdateProductPhotosUseCase,
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
            ProductDetailUiEvent.RetryDiscount -> loadDiscount()
            is ProductDetailUiEvent.DiscountToggled -> onDiscountToggled(event.enabled)
            ProductDetailUiEvent.DiscountSummaryClicked -> openDiscountDraft()
            is ProductDetailUiEvent.DiscountDraftChanged ->
                _uiState.update { if (it.discountDraft == null) it else it.copy(discountDraft = event.draft) }
            ProductDetailUiEvent.DiscountApplied -> onDiscountApplied()
            ProductDetailUiEvent.DiscountDismissed ->
                _uiState.update { it.copy(discountDraft = null) }
            is ProductDetailUiEvent.PhotosPicked -> onPhotosPicked(event.images)
            is ProductDetailUiEvent.PhotoRemoved -> onPhotoRemoved(event.key)
            ProductDetailUiEvent.OptimizeDescriptionWithAi -> optimizeDescription()
            // The preview only reads the form, so opening or closing it never marks it dirty.
            ProductDetailUiEvent.PreviewRequested ->
                _uiState.update { if (it.isPreviewEnabled) it.copy(showPreview = true) else it }
            ProductDetailUiEvent.PreviewDismissed ->
                _uiState.update { it.copy(showPreview = false) }
            ProductDetailUiEvent.Save -> save()
            ProductDetailUiEvent.CancelRequested ->
                if (!_uiState.value.isDeleting) onCancelRequested()
            // Disabled in the header while saving; ignored here too so a stray tap cannot leave.
            ProductDetailUiEvent.OrderRequestsRequested ->
                if (!_uiState.value.isSaving && !_uiState.value.isDeleting) leave(Exit.ORDER_REQUESTS)
            ProductDetailUiEvent.ConfirmDiscard -> {
                _uiState.update { it.copy(showDiscardDialog = false) }
                emit(pendingExit.effect)
                pendingExit = Exit.BACK
            }
            ProductDetailUiEvent.DismissDiscard -> {
                _uiState.update { it.copy(showDiscardDialog = false) }
                pendingExit = Exit.BACK
            }
            ProductDetailUiEvent.DeleteRequested ->
                _uiState.update {
                    val canDelete = !it.isLoading && !it.isError && !it.isSaving && !it.isDeleting
                    if (canDelete) it.copy(showDeleteDialog = true) else it
                }
            ProductDetailUiEvent.DeleteConfirmed -> deleteProduct()
            ProductDetailUiEvent.DeleteDismissed ->
                _uiState.update { if (it.isDeleting) it else it.copy(showDeleteDialog = false) }
        }
    }

    /**
     * Deletes the product and leaves the form; unsaved edits go with it, and the confirmation
     * already said so. A 404 also leaves, since the product is gone either way. Any other failure
     * keeps the merchant on the form, edits intact.
     */
    private fun deleteProduct() {
        val current = _uiState.value
        if (!current.showDeleteDialog || current.isDeleting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true) }
            val result = deleteProductUseCase(productId)
            val error = (result.exceptionOrNull() as? UpdateProductException)?.error
            when {
                result.isSuccess -> _uiEffect.emit(ProductDetailUiEffect.NavigateBackDeleted(alreadyDeleted = false))
                error == UpdateProductError.NotFound ->
                    _uiEffect.emit(ProductDetailUiEffect.NavigateBackDeleted(alreadyDeleted = true))
                else -> {
                    _uiState.update { it.copy(isDeleting = false, showDeleteDialog = false) }
                    _uiEffect.emit(ProductDetailUiEffect.ShowError(R.string.detail_delete_error))
                }
            }
        }
    }

    /**
     * Loads the product, the catalog and the product's discount concurrently. A catalog or discount
     * failure is non-fatal — it only disables that control, whose own retry is wired separately —
     * but a product failure puts the whole screen in its error state.
     */
    private fun load() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoading = true, isError = false, isCategoriesLoading = true, isCategoriesError = false)
            }
            val categoriesDeferred = async { getCategoriesUseCase() }
            val discountDeferred = async { getProductDiscountUseCase(productId) }
            val productResult = getProductDetailUseCase(productId)
            val categoriesResult = categoriesDeferred.await()
            val discountResult = discountDeferred.await()

            _uiState.update { state ->
                val withCatalog = state.copy(
                    isCategoriesLoading = false,
                    categories = categoriesResult.getOrDefault(emptyList()).toImmutableList(),
                    isCategoriesError = categoriesResult.isFailure,
                    loadedDiscount = discountResult.getOrNull()?.discount,
                    discount = discountResult.getOrNull()?.discount,
                    discountDraft = null,
                    isDiscountLoading = false,
                    isDiscountError = discountResult.isFailure,
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

    private fun loadDiscount() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDiscountLoading = true, isDiscountError = false) }
            val result = getProductDiscountUseCase(productId)
            _uiState.update { state ->
                state.copy(
                    isDiscountLoading = false,
                    isDiscountError = result.isFailure,
                    loadedDiscount = result.getOrNull()?.discount,
                    discount = result.getOrNull()?.discount,
                )
            }
        }
    }

    /**
     * Seeds the form from the loaded product. A null stock renders as an empty quantity field, which
     * the merchant must fill before saving — the backend derives availability from it either way.
     * The backend's discounted price is kept against the exact price text the form starts with, so
     * it is shown only while that price is untouched.
     */
    private fun ProductDetailUiState.seededFrom(product: ProductDetail) = copy(
        name = product.name,
        price = formatPrice(product.price),
        serverPricing = ServerPricing(
            priceText = formatPrice(product.price),
            discountedPrice = product.discountedPrice,
            discountEndsAt = product.discountEndsAt,
        ),
        quantity = product.stock?.toString().orEmpty(),
        description = product.description.orEmpty(),
        tags = product.tags.joinToString(", "),
        selectedCategoryId = product.categoryId,
        photos = product.images.map { FormPhoto.Saved(it) }.toImmutableList(),
        loadedImages = product.images.toImmutableList(),
        allowDiscountInquiry = product.allowDiscountInquiry,
        position = product.position,
        nameError = null,
        priceError = null,
        quantityError = null,
        isLoading = false,
        isError = false,
        isDirty = false,
    )

    /**
     * Adds picked photos to the form as pending. Only JPG and PNG are kept, a photo already on the
     * form is ignored, and only as many as fit under [MAX_PHOTOS] are taken; anything left out is
     * explained with a message. Nothing is sent until save.
     */
    private fun onPhotosPicked(picked: List<SelectedImage>) {
        val current = _uiState.value
        if (current.isSaving || current.isDeleting) return
        val (supported, unsupported) = picked.partition { it.mimeType.lowercase() in SUPPORTED_MIME_TYPES }
        val onForm = current.photos.map { it.key }.toSet()
        val fresh = supported.filter { it.uri !in onForm }.distinctBy { it.uri }
        val room = (MAX_PHOTOS - current.photos.size).coerceAtLeast(0)
        val accepted = fresh.take(room)
        if (accepted.isNotEmpty()) {
            _uiState.update { state ->
                state.copy(
                    photos = (state.photos + accepted.map { FormPhoto.Pending(it) }).toImmutableList(),
                    isDirty = true,
                )
            }
        }
        when {
            fresh.size > accepted.size -> emit(ProductDetailUiEffect.ShowError(R.string.detail_photos_limit_reached))
            unsupported.isNotEmpty() -> emit(ProductDetailUiEffect.ShowError(R.string.detail_photos_unsupported))
        }
    }

    /** Removes a photo from the form; the last one stays, since a product keeps at least one. */
    private fun onPhotoRemoved(key: String) {
        _uiState.update { state ->
            if (state.isSaving || state.isDeleting || !state.canRemovePhotos) return@update state
            val remaining = state.photos.filterNot { it.key == key }
            if (remaining.size == state.photos.size) state else state.copy(photos = remaining.toImmutableList(), isDirty = true)
        }
    }

    private fun onNameChanged(value: String) {
        val error = if (value.isNotBlank() && !isValidProductName(value)) R.string.detail_error_name else null
        _uiState.update { it.copy(name = value, nameError = error, isDirty = true) }
    }

    private fun onPriceChanged(value: String) {
        val error = if (value.isNotBlank() && parsePrice(value) == null) R.string.detail_error_price else null
        val newPrice = parsePrice(value)
        _uiState.update { state ->
            state.copy(
                price = value,
                priceError = error,
                // A discount that no longer fits the price (or has no price to apply to) is dropped.
                discount = state.discount?.takeIf { newPrice != null && it.fits(newPrice) },
                isDirty = true,
            )
        }
    }

    private fun onDiscountToggled(enabled: Boolean) {
        if (enabled) {
            openDiscountDraft()
        } else {
            _uiState.update { it.copy(discount = null, discountDraft = null, isDirty = true) }
        }
    }

    /** Opens the discount dialog for the price on the form, prefilled with the form's discount. */
    private fun openDiscountDraft() {
        val current = _uiState.value
        if (!current.isDiscountToggleEnabled) return
        val price = parsePrice(current.price) ?: return
        _uiState.update { it.copy(discountDraft = DiscountDraft.from(price, it.discount)) }
    }

    /** Applying only changes the form; the discount is persisted when the merchant saves. */
    private fun onDiscountApplied() {
        val discount = _uiState.value.discountDraft?.toDiscount() ?: return
        _uiState.update { it.copy(discount = discount, discountDraft = null, isDirty = true) }
    }

    private fun onQuantityChanged(value: String) {
        val error = if (value.isNotBlank() && parseQuantity(value) == null) {
            R.string.detail_error_quantity
        } else {
            null
        }
        _uiState.update { it.copy(quantity = value, quantityError = error, isDirty = true) }
    }

    private fun onCancelRequested() = leave(Exit.BACK)

    /**
     * Back, cancel and the header bell all leave the form the same way: straight away when nothing
     * changed, through the discard dialog otherwise. [pendingExit] remembers where to go once the
     * merchant confirms.
     */
    private fun leave(exit: Exit) {
        if (_uiState.value.isDirty) {
            pendingExit = exit
            _uiState.update { it.copy(showDiscardDialog = true) }
        } else {
            emit(exit.effect)
        }
    }

    private var pendingExit = Exit.BACK

    private enum class Exit(val effect: ProductDetailUiEffect) {
        BACK(ProductDetailUiEffect.NavigateBackCancelled),
        ORDER_REQUESTS(ProductDetailUiEffect.NavigateToOrderRequests),
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
                    if (persistPhotoChanges(current).isFailure) {
                        // The product is saved; the photos are re-read so the strip shows what the
                        // server has, and the form stays dirty so saving again retries what is left.
                        // The discount is not attempted: one failure, one message.
                        resyncPhotos(current, uploaded = false)
                        // The server may now price the product differently from what was loaded.
                        _uiState.update { it.copy(isSaving = false, serverPricing = null) }
                        emit(ProductDetailUiEffect.ShowError(R.string.detail_error_photos_save))
                        return@onSuccess
                    }
                    if (persistDiscountChange(current).isSuccess) {
                        _uiState.update { it.copy(isSaving = false, isDirty = false) }
                        emit(ProductDetailUiEffect.NavigateBackSaved)
                    } else {
                        // The product is saved; the form stays dirty so saving again retries the
                        // discount. A failed replace may have left the product with no discount,
                        // so what is stored is read again rather than assumed.
                        // Photos that did go through must not be uploaded again on retry.
                        if (current.isPhotosChanged) resyncPhotos(current, uploaded = true)
                        val stored = getProductDiscountUseCase(productId)
                        _uiState.update { state ->
                            state.copy(
                                isSaving = false,
                                serverPricing = null,
                                loadedDiscount = stored.fold(
                                    onSuccess = { it?.discount },
                                    onFailure = { state.loadedDiscount },
                                ),
                            )
                        }
                        emit(ProductDetailUiEffect.ShowError(R.string.detail_error_discount_save))
                    }
                }
                .onFailure { throwable ->
                    _uiState.update { it.copy(isSaving = false) }
                    val error = (throwable as? UpdateProductException)?.error
                    emit(ProductDetailUiEffect.ShowError(error.toMessageRes()))
                }
        }
    }

    /** Deletes the removed photos and uploads the added ones; nothing when photos did not change. */
    private suspend fun persistPhotoChanges(state: ProductDetailUiState): Result<Unit> {
        if (!state.isPhotosChanged) return Result.success(Unit)
        return updateProductPhotosUseCase(
            productId = productId,
            removedImageIds = state.removedImageIds,
            addedImages = state.addedImages.map { it.image },
        )
    }

    /**
     * Rebuilds the photo strip from the server after a save that touched photos did not fully go
     * through. Saved photos are the server's, minus those the merchant removed that still exist (so
     * they are deleted on retry). The upload is a single all-or-nothing request, so pending picks
     * stay pending unless it [uploaded] — then they are on the server already. If the
     * re-read itself fails the strip is left as it was: a retry deletes what is left (already-gone
     * photos count as deleted) and uploads again.
     */
    private suspend fun resyncPhotos(submitted: ProductDetailUiState, uploaded: Boolean) {
        val server = getProductDetailUseCase(productId).getOrNull()?.images ?: return
        val stillToRemove = submitted.removedImageIds.toSet()
        _uiState.update { state ->
            val saved = server.filterNot { it.id in stillToRemove }.map { FormPhoto.Saved(it) }
            val pending = if (uploaded) emptyList() else state.photos.filterIsInstance<FormPhoto.Pending>()
            state.copy(
                photos = (saved + pending).toImmutableList(),
                loadedImages = server.toImmutableList(),
            )
        }
    }

    /**
     * Sends the form's discount change, if there is one: a set discount replaces whatever the
     * product had, a cleared one removes it. Nothing is sent when the discount was not touched or
     * could not be loaded in the first place.
     */
    private suspend fun persistDiscountChange(state: ProductDetailUiState): Result<Unit> {
        if (state.isDiscountError || !state.isDiscountChanged) return Result.success(Unit)
        val discount = state.discount
        return if (discount != null) {
            saveProductDiscountUseCase(productId, discount).map { }
        } else {
            removeProductDiscountUseCase(productId)
        }
    }

    private companion object {
        val SUPPORTED_MIME_TYPES = setOf("image/jpeg", "image/jpg", "image/png")
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
