package com.qhathia.mobile.staff.feature.orderrequests.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qhathia.mobile.staff.core.domain.model.ConfirmOrderError
import com.qhathia.mobile.staff.core.domain.model.ConfirmOrderException
import com.qhathia.mobile.staff.core.domain.model.OrderPage
import com.qhathia.mobile.staff.core.domain.model.RejectOrderError
import com.qhathia.mobile.staff.core.domain.model.RejectOrderException
import com.qhathia.mobile.staff.core.domain.usecase.ConfirmOrderUseCase
import com.qhathia.mobile.staff.core.domain.usecase.GetOrderDetailUseCase
import com.qhathia.mobile.staff.core.domain.usecase.GetOrdersUseCase
import com.qhathia.mobile.staff.core.domain.usecase.ObservePendingOrderCountUseCase
import com.qhathia.mobile.staff.core.domain.usecase.RejectOrderUseCase
import com.qhathia.mobile.staff.feature.orderrequests.R
import com.qhathia.mobile.staff.feature.orderrequests.model.OrderDetailUiState
import com.qhathia.mobile.staff.feature.orderrequests.model.OrderRequestsFilter
import com.qhathia.mobile.staff.feature.orderrequests.model.OrderRequestsUiEffect
import com.qhathia.mobile.staff.feature.orderrequests.model.OrderRequestsUiEvent
import com.qhathia.mobile.staff.feature.orderrequests.model.OrderRequestsUiState
import com.qhathia.mobile.staff.feature.orderrequests.model.RejectionUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/**
 * Drives the Solicitudes screen.
 *
 * Filtering and search are both server-side: the active tab's status list and the search text go
 * into every `GET /orders`, and loads are funnelled through a single `flatMapLatest` pipeline so a
 * slow response can never overwrite a newer one. Search keystrokes are debounced; a filter change
 * or an explicit refresh is not.
 *
 * Every mutation outcome — success or documented conflict — ends in a reload, so the screen never
 * keeps showing an order the backend has already closed.
 *
 * `Pendientes (N)` reads the app-wide pending count rather than counting for itself, so the label,
 * the Solicitudes tab badge and the Inicio bell are the same number by construction. The count is
 * fed by the data layer: this screen's own unfiltered pending query updates it, and so does every
 * confirm and reject — no extra request is issued for it here.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class OrderRequestsViewModel @Inject constructor(
    private val getOrdersUseCase: GetOrdersUseCase,
    private val getOrderDetailUseCase: GetOrderDetailUseCase,
    private val confirmOrderUseCase: ConfirmOrderUseCase,
    private val rejectOrderUseCase: RejectOrderUseCase,
    observePendingOrderCountUseCase: ObservePendingOrderCountUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderRequestsUiState())
    val uiState: StateFlow<OrderRequestsUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<OrderRequestsUiEffect>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val uiEffect: SharedFlow<OrderRequestsUiEffect> = _uiEffect.asSharedFlow()

    private val searchQuery = MutableStateFlow("")
    private val selectedFilter = MutableStateFlow(OrderRequestsFilter.PENDING)

    /** Bumped to re-run the pipeline without changing the filter or the query. */
    private val reloadTrigger = MutableStateFlow(0)

    /**
     * The order whose confirm/reject is in flight, guarding against a double tap. It covers the
     * card's Rechazar action too, which has no dialog state to hold `isSubmitting`.
     */
    private var mutatingOrderId: String? = null

    init {
        observePendingOrderCountUseCase()
            .onEach { count -> _uiState.update { it.copy(pendingCount = count) } }
            .launchIn(viewModelScope)
        observeLoads()
    }

    fun onEvent(event: OrderRequestsUiEvent) {
        when (event) {
            OrderRequestsUiEvent.Load,
            OrderRequestsUiEvent.Retry,
            OrderRequestsUiEvent.Refresh,
            -> reload()

            is OrderRequestsUiEvent.SelectFilter -> {
                _uiState.update { it.copy(selectedFilter = event.filter) }
                selectedFilter.value = event.filter
            }

            is OrderRequestsUiEvent.Search -> {
                _uiState.update { it.copy(searchQuery = event.query) }
                searchQuery.value = event.query
            }

            is OrderRequestsUiEvent.OpenDetail -> loadDetail(event.orderId)
            OrderRequestsUiEvent.DismissDetail -> _uiState.update { it.copy(detail = null) }
            is OrderRequestsUiEvent.ConfirmOrder -> confirmOrder(event.orderId)

            is OrderRequestsUiEvent.StartRejection -> startRejection(event)
            is OrderRequestsUiEvent.SelectRejectionReason -> updateRejection {
                it.copy(selectedReason = event.reason)
            }
            is OrderRequestsUiEvent.EditOtherRejectionReason -> updateRejection {
                it.copy(otherReason = event.text)
            }
            OrderRequestsUiEvent.CancelRejection -> cancelRejection()
            is OrderRequestsUiEvent.ConfirmRejection -> rejectOrder(event.reason)
        }
    }

    /**
     * Opens the reject form. From a card there is no detail behind it, so the dialog is created on
     * the spot with only the ids the form needs; from the open dialog it just adds the form step.
     */
    private fun startRejection(event: OrderRequestsUiEvent.StartRejection) {
        _uiState.update { state ->
            val rejection = RejectionUiState(orderCode = event.orderCode)
            val detail = state.detail?.takeIf { it.orderId == event.orderId }
                ?: OrderDetailUiState(orderId = event.orderId)
            state.copy(detail = detail.copy(rejection = rejection))
        }
    }

    private fun updateRejection(transform: (RejectionUiState) -> RejectionUiState) {
        _uiState.update { state ->
            val rejection = state.detail?.rejection ?: return@update state
            state.copy(detail = state.detail.copy(rejection = transform(rejection)))
        }
    }

    /** Back to the detail when there is one loaded; otherwise the form *was* the dialog. */
    private fun cancelRejection() {
        _uiState.update { state ->
            val detail = state.detail ?: return@update state
            if (detail.order == null) {
                state.copy(detail = null)
            } else {
                state.copy(detail = detail.copy(rejection = null))
            }
        }
    }

    /**
     * The single load pipeline. Only the query is debounced — and only while it is non-empty, so
     * the first load and a cleared search box both go out immediately. `flatMapLatest` cancels an
     * in-flight request as soon as a newer filter, query or refresh arrives.
     */
    private fun observeLoads() {
        combine(
            searchQuery.debounce { query -> if (query.isEmpty()) 0L else SEARCH_DEBOUNCE_MS },
            selectedFilter,
            reloadTrigger,
        ) { query, filter, _ -> LoadRequest(query = query, filter = filter) }
            .onEach { _uiState.update { state -> state.copy(isLoading = true, isError = false) } }
            .flatMapLatest { request -> flow { emit(load(request)) } }
            .onEach { outcome -> applyOutcome(outcome) }
            .launchIn(viewModelScope)
    }

    /** Issues the list query for the active filter and search text. */
    private suspend fun load(request: LoadRequest): Result<OrderPage> =
        getOrdersUseCase(
            statuses = request.filter.statuses,
            query = request.query.trim().takeIf { it.isNotEmpty() },
            page = FIRST_PAGE,
            size = PAGE_SIZE,
        )

    private fun applyOutcome(outcome: Result<OrderPage>) {
        _uiState.update { state ->
            val page: OrderPage? = outcome.getOrNull()
            state.copy(
                // A failed load keeps the previously loaded orders and timestamp on screen: stale
                // data plus an error beats a blank list.
                orders = page?.orders?.toImmutableList() ?: state.orders,
                lastUpdatedAt = if (page != null) Instant.now() else state.lastUpdatedAt,
                isLoading = false,
                isError = page == null,
            )
        }
        if (outcome.isFailure) {
            viewModelScope.launch {
                _uiEffect.emit(OrderRequestsUiEffect.ShowError(R.string.order_error_load))
            }
        }
    }

    private fun reload() {
        reloadTrigger.update { it + 1 }
    }

    private fun loadDetail(orderId: String) {
        _uiState.update { it.copy(detail = OrderDetailUiState(orderId = orderId, isLoading = true)) }
        viewModelScope.launch {
            getOrderDetailUseCase(orderId)
                .onSuccess { detail ->
                    _uiState.update { state ->
                        // Ignore a response for a dialog the merchant has already dismissed or
                        // replaced.
                        if (state.detail?.orderId != orderId) return@update state
                        state.copy(
                            detail = state.detail.copy(order = detail, isLoading = false),
                        )
                    }
                }
                .onFailure {
                    _uiState.update { state ->
                        if (state.detail?.orderId != orderId) return@update state
                        state.copy(
                            detail = state.detail.copy(isLoading = false, isError = true),
                        )
                    }
                }
        }
    }

    private fun confirmOrder(orderId: String) {
        if (!startMutation(orderId)) return
        viewModelScope.launch {
            confirmOrderUseCase(orderId)
                .onSuccess {
                    finishMutation(closeDialog = true)
                    _uiEffect.emit(OrderRequestsUiEffect.ShowConfirmedSnackbar)
                    reload()
                }
                .onFailure { throwable ->
                    when (val error = (throwable as? ConfirmOrderException)?.error) {
                        // Retryable: the order is still pending, so the dialog stays open.
                        ConfirmOrderError.Network, null -> {
                            finishMutation(closeDialog = false)
                            _uiEffect.emit(
                                OrderRequestsUiEffect.ShowError(R.string.order_error_network),
                            )
                        }
                        // Settled elsewhere: close the dialog, explain, and re-query.
                        else -> {
                            finishMutation(closeDialog = true)
                            _uiEffect.emit(error.toEffect())
                            reload()
                        }
                    }
                }
        }
    }

    /** Rejects the order the form is open for, with the motive the merchant picked. */
    private fun rejectOrder(reason: String) {
        val orderId = _uiState.value.detail?.takeIf { it.rejection != null }?.orderId ?: return
        if (!startMutation(orderId)) return
        viewModelScope.launch {
            rejectOrderUseCase(orderId, reason)
                .onSuccess {
                    finishMutation(closeDialog = true)
                    _uiEffect.emit(OrderRequestsUiEffect.ShowRejectedSnackbar)
                    reload()
                }
                .onFailure { throwable ->
                    when (val error = (throwable as? RejectOrderException)?.error) {
                        RejectOrderError.Network, null -> {
                            finishMutation(closeDialog = false)
                            _uiEffect.emit(
                                OrderRequestsUiEffect.ShowError(R.string.order_error_network),
                            )
                        }
                        else -> {
                            finishMutation(closeDialog = true)
                            _uiEffect.emit(error.toEffect())
                            reload()
                        }
                    }
                }
        }
    }

    /** Returns false when a mutation is already in flight, so a second tap is a no-op. */
    private fun startMutation(orderId: String): Boolean {
        if (mutatingOrderId != null) return false
        mutatingOrderId = orderId
        _uiState.update { state ->
            val detail = state.detail ?: return@update state
            state.copy(detail = detail.copy(isSubmitting = true))
        }
        return true
    }

    private fun finishMutation(closeDialog: Boolean) {
        mutatingOrderId = null
        _uiState.update { state ->
            if (closeDialog) {
                state.copy(detail = null)
            } else {
                state.copy(detail = state.detail?.copy(isSubmitting = false))
            }
        }
    }

    private fun ConfirmOrderError.toEffect(): OrderRequestsUiEffect.ShowError = when (this) {
        is ConfirmOrderError.StockInsufficient ->
            if (productName != null) {
                OrderRequestsUiEffect.ShowError(R.string.order_error_stock, productName)
            } else {
                OrderRequestsUiEffect.ShowError(R.string.order_error_stock_generic)
            }
        ConfirmOrderError.AlreadyHandled ->
            OrderRequestsUiEffect.ShowError(R.string.order_error_already_handled)
        ConfirmOrderError.NotFound ->
            OrderRequestsUiEffect.ShowError(R.string.order_error_not_found)
        ConfirmOrderError.Network ->
            OrderRequestsUiEffect.ShowError(R.string.order_error_network)
        is ConfirmOrderError.Unknown ->
            OrderRequestsUiEffect.ShowError(R.string.order_error_unknown)
    }

    private fun RejectOrderError.toEffect(): OrderRequestsUiEffect.ShowError = when (this) {
        RejectOrderError.AlreadyHandled ->
            OrderRequestsUiEffect.ShowError(R.string.order_error_already_handled)
        RejectOrderError.NotFound ->
            OrderRequestsUiEffect.ShowError(R.string.order_error_not_found)
        RejectOrderError.Network ->
            OrderRequestsUiEffect.ShowError(R.string.order_error_network)
        is RejectOrderError.Unknown ->
            OrderRequestsUiEffect.ShowError(R.string.order_error_unknown)
    }

    private data class LoadRequest(val query: String, val filter: OrderRequestsFilter)

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 350L
        const val FIRST_PAGE = 0

        /** Generous enough to cover a realistic day without pagination UI. */
        const val PAGE_SIZE = 50
    }
}
