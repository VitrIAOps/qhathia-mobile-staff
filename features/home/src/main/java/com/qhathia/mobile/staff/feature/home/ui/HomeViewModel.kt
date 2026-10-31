package com.qhathia.mobile.staff.feature.home.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qhathia.mobile.staff.core.domain.model.SalesPeriod
import com.qhathia.mobile.staff.core.domain.model.SalesSummary
import com.qhathia.mobile.staff.core.domain.model.StoreSuggestions
import com.qhathia.mobile.staff.core.domain.usecase.GetSalesSuggestionsUseCase
import com.qhathia.mobile.staff.core.domain.usecase.GetSalesSummaryUseCase
import com.qhathia.mobile.staff.core.domain.usecase.ObservePendingOrderCountUseCase
import com.qhathia.mobile.staff.core.domain.usecase.ObserveSessionUseCase
import com.qhathia.mobile.staff.core.domain.usecase.RefreshPendingOrderCountUseCase
import com.qhathia.mobile.staff.feature.home.model.HomeUiEffect
import com.qhathia.mobile.staff.feature.home.model.HomeUiEvent
import com.qhathia.mobile.staff.feature.home.model.HomeUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the Inicio tab.
 *
 * Every load goes through one `flatMapLatest` pipeline keyed on the selected period, so a slow
 * response for a period the merchant has already switched away from can never land on screen. The
 * summary, the suggestions and the pending-count refresh are issued concurrently — none of them
 * depends on another.
 *
 * Inicio is a bottom-nav tab, so this ViewModel outlives every visit to it. Each return to the
 * screen ([HomeUiEvent.ScreenStarted]) reloads, so a product restocked from Inventario does not stay
 * under "Productos para reponer" until the merchant thinks to refresh. A reload keeps what is on
 * screen until the new data arrives.
 *
 * The greeting and the badge are the only two values that are not part of a load: the first is
 * taken from the real session, the second from the app-wide pending count, and both are collected
 * for the lifetime of the ViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getSalesSummaryUseCase: GetSalesSummaryUseCase,
    private val getSalesSuggestionsUseCase: GetSalesSuggestionsUseCase,
    private val refreshPendingOrderCountUseCase: RefreshPendingOrderCountUseCase,
    observeSessionUseCase: ObserveSessionUseCase,
    observePendingOrderCountUseCase: ObservePendingOrderCountUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<HomeUiEffect>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val uiEffect: SharedFlow<HomeUiEffect> = _uiEffect.asSharedFlow()

    private val selectedPeriod = MutableStateFlow(SalesPeriod.WEEKLY)

    /** Bumped to re-run the pipeline without changing the period. */
    private val reloadTrigger = MutableStateFlow(0)

    /** The first `ON_START` arrives while the load started in `init` is already running. */
    private var hasStarted = false

    init {
        observeSessionUseCase()
            .onEach { session ->
                _uiState.update { it.copy(merchantFirstName = session?.user?.fullName.firstName()) }
            }
            .launchIn(viewModelScope)

        observePendingOrderCountUseCase()
            .onEach { count -> _uiState.update { it.copy(pendingOrderCount = count) } }
            .launchIn(viewModelScope)

        observeLoads()
    }

    fun onEvent(event: HomeUiEvent) {
        when (event) {
            HomeUiEvent.Refresh -> reloadTrigger.update { it + 1 }

            HomeUiEvent.ScreenStarted -> {
                if (hasStarted) reloadTrigger.update { it + 1 }
                hasStarted = true
            }

            is HomeUiEvent.SelectPeriod -> {
                _uiState.update { it.copy(selectedPeriod = event.period) }
                selectedPeriod.value = event.period
            }

            HomeUiEvent.OpenNotifications -> viewModelScope.launch {
                _uiEffect.emit(HomeUiEffect.NavigateToOrderRequests)
            }

            HomeUiEvent.OpenSalesStatistics -> viewModelScope.launch {
                _uiEffect.emit(HomeUiEffect.NavigateToSalesStatistics(selectedPeriod.value))
            }
        }
    }

    private fun observeLoads() {
        combine(selectedPeriod, reloadTrigger) { period, _ -> period }
            .onEach { _uiState.update { state -> state.copy(isLoading = true, isError = false) } }
            .flatMapLatest { period -> flow { emit(load(period)) } }
            .onEach { outcome -> applyOutcome(outcome) }
            .launchIn(viewModelScope)
    }

    /**
     * The pending count rides along with every load, so opening Inicio is enough to make the bell
     * and the Solicitudes tab badge current. Its outcome does not feed [LoadOutcome] — a badge that
     * failed to refresh is not a reason to put the whole screen in the error state.
     */
    private suspend fun load(period: SalesPeriod): LoadOutcome = coroutineScope {
        val summary = async { getSalesSummaryUseCase(period) }
        val suggestions = async { getSalesSuggestionsUseCase() }
        val pendingCount = async { refreshPendingOrderCountUseCase() }
        pendingCount.await()
        LoadOutcome(summary = summary.await(), suggestions = suggestions.await())
    }

    private fun applyOutcome(outcome: LoadOutcome) {
        _uiState.update { state ->
            val summary: SalesSummary? = outcome.summary.getOrNull()
            val suggestions: StoreSuggestions? = outcome.suggestions.getOrNull()
            state.copy(
                // A failed load keeps whatever was already on screen, timestamp included.
                summary = summary ?: state.summary,
                missingProducts = suggestions?.missingProducts?.toImmutableList()
                    ?: state.missingProducts,
                restockProducts = suggestions?.restockProducts?.toImmutableList()
                    ?: state.restockProducts,
                otherRequests = suggestions?.otherRequests?.toImmutableList()
                    ?: state.otherRequests,
                lastUpdatedAt = summary?.generatedAt ?: state.lastUpdatedAt,
                isLoading = false,
                isError = summary == null || suggestions == null,
            )
        }
    }

    /** The first whitespace-separated token of the merchant's full name, or null when there is none. */
    private fun String?.firstName(): String? =
        this?.trim()?.split(' ', '\t', '\n')?.firstOrNull()?.takeIf { it.isNotBlank() }

    private data class LoadOutcome(
        val summary: Result<SalesSummary>,
        val suggestions: Result<StoreSuggestions>,
    )
}
