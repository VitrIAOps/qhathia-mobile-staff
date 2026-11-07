package com.qhathia.mobile.staff.feature.productregister.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qhathia.mobile.staff.core.common.validation.MAX_PRODUCT_PHOTOS
import com.qhathia.mobile.staff.core.common.validation.isValidProductName
import com.qhathia.mobile.staff.core.common.validation.parsePrice
import com.qhathia.mobile.staff.core.common.validation.parseQuantity
import com.qhathia.mobile.staff.core.domain.model.DiscountDraft
import com.qhathia.mobile.staff.core.domain.model.NewProduct
import com.qhathia.mobile.staff.core.domain.model.ProductDraft
import com.qhathia.mobile.staff.core.domain.model.RegisterProductError
import com.qhathia.mobile.staff.core.domain.model.RegisterProductException
import com.qhathia.mobile.staff.core.domain.model.SelectedImage
import com.qhathia.mobile.staff.core.domain.usecase.DraftProductFromImageUseCase
import com.qhathia.mobile.staff.core.domain.usecase.GetCategoriesUseCase
import com.qhathia.mobile.staff.core.domain.usecase.OptimizeDescriptionUseCase
import com.qhathia.mobile.staff.core.domain.usecase.RegisterProductUseCase
import com.qhathia.mobile.staff.core.domain.usecase.SaveProductDiscountUseCase
import com.qhathia.mobile.staff.feature.productregister.R
import com.qhathia.mobile.staff.feature.productregister.model.ProductRegisterUiEffect
import com.qhathia.mobile.staff.feature.productregister.model.ProductRegisterUiEvent
import com.qhathia.mobile.staff.feature.productregister.model.ProductRegisterUiState
import com.qhathia.mobile.staff.feature.productregister.model.parseTags
import com.qhathia.mobile.staff.feature.productregister.navigation.PRODUCT_REGISTER_ARG_NAME
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
import java.util.UUID
import javax.inject.Inject

/**
 * Drives the product registration screen: loads categories on init, validates each field as the
 * merchant edits it, and on submit orchestrates registration through [RegisterProductUseCase],
 * branching the result into success / plan-limit / error effects. Tags are entered as free text
 * (comma-separated). A dirty form routes cancel/back through the discard-confirmation dialog.
 *
 * An optional discount is kept on the form and only sent once registration returns the new
 * product's id, since a discount cannot exist before its product does.
 *
 * The form has one idempotency key, kept in [SavedStateHandle] so it survives process death, and
 * sent on every submit: a retry after a lost answer gets the product already created instead of a
 * duplicate. While submitting nothing leaves the screen, since leaving cancels the request.
 *
 * The form may open with a name ([PRODUCT_REGISTER_ARG_NAME]), which does not count as a change.
 */
@HiltViewModel
class ProductRegisterViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val registerProductUseCase: RegisterProductUseCase,
    private val draftProductFromImageUseCase: DraftProductFromImageUseCase,
    private val optimizeDescriptionUseCase: OptimizeDescriptionUseCase,
    private val saveProductDiscountUseCase: SaveProductDiscountUseCase,
) : ViewModel() {

    private val idempotencyKey: String =
        savedStateHandle.get<String>(KEY_IDEMPOTENCY) ?: UUID.randomUUID().toString().also {
            savedStateHandle[KEY_IDEMPOTENCY] = it
        }

    private val _uiState = MutableStateFlow(ProductRegisterUiState())
    val uiState: StateFlow<ProductRegisterUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<ProductRegisterUiEffect>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val uiEffect: SharedFlow<ProductRegisterUiEffect> = _uiEffect.asSharedFlow()

    init {
        // Opened from "Agregar": the searched name is a starting point, not something the merchant
        // typed, so leaving untouched does not ask to discard it.
        savedStateHandle.get<String>(PRODUCT_REGISTER_ARG_NAME)?.trim()?.takeIf { it.isNotEmpty() }?.let { name ->
            _uiState.update { it.copy(name = name) }
        }
        loadCategories()
    }

    fun onEvent(event: ProductRegisterUiEvent) {
        when (event) {
            is ProductRegisterUiEvent.NameChanged -> onNameChanged(event.value)
            is ProductRegisterUiEvent.PriceChanged -> onPriceChanged(event.value)
            is ProductRegisterUiEvent.QuantityChanged -> onQuantityChanged(event.value)
            is ProductRegisterUiEvent.DiscountToggled -> onDiscountToggled(event.enabled)
            ProductRegisterUiEvent.DiscountSummaryClicked -> openDiscountDraft()
            is ProductRegisterUiEvent.DiscountDraftChanged ->
                _uiState.update { if (it.discountDraft == null) it else it.copy(discountDraft = event.draft) }
            ProductRegisterUiEvent.DiscountApplied -> onDiscountApplied()
            ProductRegisterUiEvent.DiscountDismissed ->
                _uiState.update { it.copy(discountDraft = null) }
            is ProductRegisterUiEvent.DescriptionChanged ->
                _uiState.update { it.copy(description = event.value, isDirty = true) }
            ProductRegisterUiEvent.GenerateDescriptionWithAi -> onGenerateWithAi()
            ProductRegisterUiEvent.ConfirmAiReplace -> {
                _uiState.update { it.copy(showAiReplaceDialog = false) }
                draftFromImage()
            }
            ProductRegisterUiEvent.DismissAiReplace ->
                _uiState.update { it.copy(showAiReplaceDialog = false) }
            is ProductRegisterUiEvent.TagsChanged ->
                _uiState.update { it.copy(tags = event.value, isDirty = true) }
            is ProductRegisterUiEvent.SelectCategory ->
                _uiState.update { it.copy(selectedCategoryId = event.categoryId, isDirty = true) }
            is ProductRegisterUiEvent.ImagesPicked -> onImagesPicked(event.images)
            is ProductRegisterUiEvent.RemoveImage -> onRemoveImage(event.uri)
            // The preview only reads the form, so opening or closing it never marks it dirty.
            ProductRegisterUiEvent.PreviewRequested ->
                _uiState.update { if (it.isPreviewEnabled) it.copy(showPreview = true) else it }
            ProductRegisterUiEvent.PreviewDismissed ->
                _uiState.update { it.copy(showPreview = false) }
            ProductRegisterUiEvent.Submit -> submit()
            ProductRegisterUiEvent.CancelRequested ->
                if (!_uiState.value.isSubmitting) onCancelRequested()
            // Disabled in the header while saving; ignored here too so a stray tap cannot leave.
            ProductRegisterUiEvent.OrderRequestsRequested ->
                if (!_uiState.value.isSubmitting) leave(Exit.ORDER_REQUESTS)
            ProductRegisterUiEvent.ConfirmDiscard -> {
                _uiState.update { it.copy(showDiscardDialog = false) }
                emit(pendingExit.effect)
                pendingExit = Exit.BACK
            }
            ProductRegisterUiEvent.DismissDiscard -> {
                _uiState.update { it.copy(showDiscardDialog = false) }
                pendingExit = Exit.BACK
            }
            ProductRegisterUiEvent.RetryCatalog -> loadCategories()
        }
    }

    private fun onNameChanged(value: String) {
        val error = if (value.isNotBlank() && !isValidProductName(value)) R.string.register_error_name else null
        _uiState.update { it.copy(name = value, nameError = error, isDirty = true) }
    }

    /**
     * Never touches the discount: an empty or half-typed price ("150.") would otherwise drop it and
     * the product would be created without it. A fixed amount over the price is caught on submit.
     */
    private fun onPriceChanged(value: String) {
        val error = if (value.isNotBlank() && parsePrice(value) == null) R.string.register_error_price else null
        _uiState.update { it.copy(price = value, priceError = error, isDirty = true) }
    }

    private fun onDiscountToggled(enabled: Boolean) {
        if (enabled) {
            openDiscountDraft()
        } else {
            _uiState.update { it.copy(discount = null, discountDraft = null, isDirty = true) }
        }
    }

    /** Opens the discount dialog for the entered price, prefilled with the applied discount if any. */
    private fun openDiscountDraft() {
        val current = _uiState.value
        if (!current.isDiscountToggleEnabled) return
        val price = parsePrice(current.price) ?: return
        _uiState.update { it.copy(discountDraft = DiscountDraft.from(price, it.discount)) }
    }

    private fun onDiscountApplied() {
        val discount = _uiState.value.discountDraft?.toDiscount() ?: return
        _uiState.update { it.copy(discount = discount, discountDraft = null, isDirty = true) }
    }

    private fun onQuantityChanged(value: String) {
        val error = if (value.isNotBlank() && parseQuantity(value) == null) {
            R.string.register_error_quantity
        } else {
            null
        }
        _uiState.update { it.copy(quantity = value, quantityError = error, isDirty = true) }
    }

    /**
     * Adds the picked photos: only JPG and PNG, no duplicates, and only as many as fit under
     * [MAX_PRODUCT_PHOTOS]. Anything left out is explained with a message (the format first).
     */
    private fun onImagesPicked(picked: List<SelectedImage>) {
        if (_uiState.value.isSubmitting) return
        val (valid, invalid) = picked.partition { it.mimeType.lowercase() in SUPPORTED_MIME_TYPES }
        val current = _uiState.value.images
        val existingUris = current.map { it.uri }.toSet()
        val fresh = valid.filter { it.uri !in existingUris }.distinctBy { it.uri }
        val room = (MAX_PRODUCT_PHOTOS - current.size).coerceAtLeast(0)
        val accepted = fresh.take(room)
        if (accepted.isNotEmpty()) {
            _uiState.update { state ->
                state.copy(images = (state.images + accepted).toImmutableList(), isDirty = true)
            }
        }
        when {
            invalid.isNotEmpty() -> emit(ProductRegisterUiEffect.ShowError(R.string.register_error_image_format))
            fresh.size > accepted.size -> emit(ProductRegisterUiEffect.ShowError(R.string.register_photos_limit_reached))
        }
    }

    private fun onRemoveImage(uri: String) {
        _uiState.update { state ->
            state.copy(images = state.images.filterNot { it.uri == uri }.toImmutableList(), isDirty = true)
        }
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

    private enum class Exit(val effect: ProductRegisterUiEffect) {
        BACK(ProductRegisterUiEffect.NavigateBackCancelled),
        ORDER_REQUESTS(ProductRegisterUiEffect.NavigateToOrderRequests),
    }

    /**
     * Routes the AI action: optimize when the description/category/tags are all filled; otherwise
     * draft from the image, warning first if any of those fields already hold data.
     */
    private fun onGenerateWithAi() {
        val current = _uiState.value
        if (!current.isAiActionEnabled) return
        when {
            current.isOptimizeMode -> optimizeDescription()
            current.description.isNotBlank() ||
                current.selectedCategoryId != null ||
                parseTags(current.tags).isNotEmpty() ->
                _uiState.update { it.copy(showAiReplaceDialog = true) }
            else -> draftFromImage()
        }
    }

    private fun draftFromImage() {
        val image = _uiState.value.images.firstOrNull() ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isAiLoading = true) }
            draftProductFromImageUseCase(image)
                .onSuccess { draft -> applyDraft(draft) }
                .onFailure { emit(ProductRegisterUiEffect.ShowError(R.string.register_error_ai)) }
            _uiState.update { it.copy(isAiLoading = false) }
        }
    }

    /**
     * Applies a draft field-by-field so a fail-open (empty) response is a no-op and a partial
     * response never wipes an existing value with a blank. The suggested category is applied only
     * when it matches one of the loaded categories.
     */
    private fun applyDraft(draft: ProductDraft) {
        _uiState.update { state ->
            val categoryMatches = draft.categoryId != null &&
                state.categories.any { it.id == draft.categoryId }
            val hasChange = draft.description.isNotBlank() || categoryMatches || draft.tags.isNotEmpty()
            state.copy(
                description = if (draft.description.isNotBlank()) draft.description else state.description,
                selectedCategoryId = if (categoryMatches) draft.categoryId else state.selectedCategoryId,
                tags = if (draft.tags.isNotEmpty()) draft.tags.joinToString(", ") else state.tags,
                isDirty = state.isDirty || hasChange,
            )
        }
    }

    private fun optimizeDescription() {
        val currentDescription = _uiState.value.description
        if (currentDescription.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isAiLoading = true) }
            optimizeDescriptionUseCase(currentDescription)
                .onSuccess { optimized ->
                    _uiState.update { it.copy(description = optimized, isDirty = true) }
                }
                .onFailure { emit(ProductRegisterUiEffect.ShowError(R.string.register_error_ai)) }
            _uiState.update { it.copy(isAiLoading = false) }
        }
    }

    private fun submit() {
        val current = _uiState.value
        if (!current.isSubmitEnabled) return
        // Checked before anything is sent: the backend would price the product at zero.
        if (current.isDiscountOverPrice) {
            _uiState.update { it.copy(discount = null, isDirty = true) }
            emit(ProductRegisterUiEffect.ShowError(R.string.register_discount_removed_over_price))
            return
        }
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
            idempotencyKey = idempotencyKey,
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true) }
            registerProductUseCase(product)
                .onSuccess { productId ->
                    val discountSaved = current.discount
                        ?.let { saveProductDiscountUseCase(productId, it).isSuccess }
                        ?: true
                    _uiState.update { it.copy(isSubmitting = false) }
                    emit(
                        if (discountSaved) {
                            ProductRegisterUiEffect.NavigateBackWithSuccess(productId)
                        } else {
                            // The product exists now, so staying on the form would invite a
                            // duplicate; the discount can be retried from the inventory.
                            ProductRegisterUiEffect.NavigateBackWithDiscountFailed(productId)
                        },
                    )
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
        const val KEY_IDEMPOTENCY = "register_idempotency_key"
        val SUPPORTED_MIME_TYPES = setOf("image/jpeg", "image/jpg", "image/png")
    }
}
