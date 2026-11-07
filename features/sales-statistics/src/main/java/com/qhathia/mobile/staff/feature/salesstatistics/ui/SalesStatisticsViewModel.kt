package com.qhathia.mobile.staff.feature.salesstatistics.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qhathia.mobile.staff.core.domain.model.SalesPeriod
import com.qhathia.mobile.staff.core.domain.usecase.GetCategoriesUseCase
import com.qhathia.mobile.staff.core.domain.usecase.GetSalesRankingUseCase
import com.qhathia.mobile.staff.core.domain.usecase.GetSalesSummaryUseCase
import com.qhathia.mobile.staff.core.domain.usecase.GetStoreActivityUseCase
import com.qhathia.mobile.staff.feature.salesstatistics.model.SalesStatisticsUiEffect
import com.qhathia.mobile.staff.feature.salesstatistics.model.SalesStatisticsUiEvent
import com.qhathia.mobile.staff.feature.salesstatistics.model.SalesStatisticsUiState
import com.qhathia.mobile.staff.feature.salesstatistics.model.StatisticsTab
import com.qhathia.mobile.staff.feature.salesstatistics.model.TabContent
import com.qhathia.mobile.staff.feature.salesstatistics.navigation.SALES_STATISTICS_ARG_PERIOD
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.ensureActive
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
 * Drives the "Resumen de ventas" screen.
 *
 * Only the tab on screen loads, and only when its data does not already belong to the current
 * filters — its query is `period + category` for Resumen and Ranking, and `period` alone for
 * Actividad, whose endpoint has no category filter. So flicking between loaded tabs issues no
 * request, and changing the category while on Actividad reloads nothing.
 *
 * One load runs at a time. Starting another cancels it, so a slow response for filters the merchant
 * already left never lands; the abandoned tab simply loads again when it is shown.
 *
 * A filter change clears the tab's figures before loading — last week's numbers under "Mensual"
 * would read as the month's. A failed retry for the same filters keeps them next to the error.
 *
 * Coming back to the screen reloads the tab on screen, keeping its figures until the answer: a
 * restock elsewhere changes "Lo que necesita atención". Back is emitted once, so a double tap can
 * never pop the screen below.
 */
@HiltViewModel
class SalesStatisticsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getSalesSummaryUseCase: GetSalesSummaryUseCase,
    private val getSalesRankingUseCase: GetSalesRankingUseCase,
    private val getStoreActivityUseCase: GetStoreActivityUseCase,
    getCategoriesUseCase: GetCategoriesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SalesStatisticsUiState(
            period = savedStateHandle.get<String>(SALES_STATISTICS_ARG_PERIOD).toSalesPeriod(),
        ),
    )
    val uiState: StateFlow<SalesStatisticsUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<SalesStatisticsUiEffect>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val uiEffect: SharedFlow<SalesStatisticsUiEffect> = _uiEffect.asSharedFlow()

    /** The query each tab's data was loaded for. A tab whose entry differs from its query is stale. */
    private val loadedQueries = mutableMapOf<StatisticsTab, TabQuery>()

    private var inFlight: TabQuery? = null
    private var loadJob: Job? = null

    /** The first `ON_START` arrives while the load started in `init` is already running. */
    private var hasStarted = false

    /** Set once back was emitted; later taps would pop the screen below this one. */
    private var isLeaving = false

    init {
        viewModelScope.launch {
            // A failure leaves only "Todos": the filter is optional, the figures are not.
            getCategoriesUseCase().onSuccess { categories ->
                _uiState.update { it.copy(categories = categories.toImmutableList()) }
            }
        }
        loadActiveTab()
    }

    fun onEvent(event: SalesStatisticsUiEvent) {
        when (event) {
            is SalesStatisticsUiEvent.SelectTab -> {
                _uiState.update { it.copy(selectedTab = event.tab) }
                loadActiveTab()
            }

            is SalesStatisticsUiEvent.SelectPeriod -> {
                _uiState.update { it.copy(period = event.period) }
                loadActiveTab()
            }

            is SalesStatisticsUiEvent.SelectCategory -> {
                _uiState.update { it.copy(selectedCategoryId = event.categoryId) }
                loadActiveTab()
            }

            SalesStatisticsUiEvent.ToggleFilters ->
                _uiState.update { it.copy(isFiltersExpanded = !it.isFiltersExpanded) }

            SalesStatisticsUiEvent.Retry -> loadActiveTab(force = true)

            SalesStatisticsUiEvent.Back -> {
                if (isLeaving) return
                isLeaving = true
                viewModelScope.launch { _uiEffect.emit(SalesStatisticsUiEffect.NavigateBack) }
            }

            SalesStatisticsUiEvent.Share -> {
                val state = _uiState.value
                if (state.isShareEnabled) {
                    viewModelScope.launch { _uiEffect.emit(SalesStatisticsUiEffect.Share(state.selectedTab)) }
                }
            }

            SalesStatisticsUiEvent.ScreenStarted -> {
                if (hasStarted) {
                    // Every tab is stale now; the one on screen reloads keeping its figures, the
                    // others when they are shown.
                    val onScreen = _uiState.value.activeQuery()
                    val keep = loadedQueries[onScreen.tab]
                    loadedQueries.clear()
                    if (keep == onScreen) loadedQueries[onScreen.tab] = onScreen
                    loadActiveTab(force = true)
                }
                hasStarted = true
            }
        }
    }

    private fun loadActiveTab(force: Boolean = false) {
        val query = _uiState.value.activeQuery()
        when (query.tab) {
            StatisticsTab.SUMMARY -> load(
                query = query,
                force = force,
                read = { it.summary },
                write = { state, content -> state.copy(summary = content) },
            ) { getSalesSummaryUseCase(query.period, query.categoryId) }

            StatisticsTab.RANKING -> load(
                query = query,
                force = force,
                read = { it.ranking },
                write = { state, content -> state.copy(ranking = content) },
            ) { getSalesRankingUseCase(query.period, query.categoryId) }

            StatisticsTab.ACTIVITY -> load(
                query = query,
                force = force,
                read = { it.activity },
                write = { state, content -> state.copy(activity = content) },
            ) { getStoreActivityUseCase(query.period) }
        }
    }

    private fun <T> load(
        query: TabQuery,
        force: Boolean,
        read: (SalesStatisticsUiState) -> TabContent<T>,
        write: (SalesStatisticsUiState, TabContent<T>) -> SalesStatisticsUiState,
        fetch: suspend () -> Result<T>,
    ) {
        val loadedFor = loadedQueries[query.tab]
        val alreadyShown = loadedFor == query && read(_uiState.value).data != null
        if (!force && (inFlight == query || alreadyShown)) return

        abandonInFlight()
        val keepData = loadedFor == query
        inFlight = query
        _uiState.update { state ->
            write(state, TabContent(data = if (keepData) read(state).data else null, isLoading = true))
        }

        loadJob = viewModelScope.launch {
            val result = fetch()
            // The network layer turns cancellation into a failed result; never publish one.
            ensureActive()
            result.fold(
                onSuccess = { data ->
                    loadedQueries[query.tab] = query
                    _uiState.update { write(it, TabContent(data = data)) }
                },
                onFailure = {
                    _uiState.update { state ->
                        write(state, read(state).copy(isLoading = false, isError = true))
                    }
                },
            )
            if (inFlight == query) inFlight = null
        }
    }

    /** Cancels the running load and stops its tab's spinner; that tab reloads when shown again. */
    private fun abandonInFlight() {
        val abandoned = inFlight ?: return
        loadJob?.cancel()
        inFlight = null
        _uiState.update { state ->
            when (abandoned.tab) {
                StatisticsTab.SUMMARY -> state.copy(summary = state.summary.copy(isLoading = false))
                StatisticsTab.RANKING -> state.copy(ranking = state.ranking.copy(isLoading = false))
                StatisticsTab.ACTIVITY -> state.copy(activity = state.activity.copy(isLoading = false))
            }
        }
    }

    /** What the tab on screen must be loaded for. Actividad has no category filter. */
    private fun SalesStatisticsUiState.activeQuery(): TabQuery = TabQuery(
        tab = selectedTab,
        period = period,
        categoryId = selectedCategoryId.takeIf { selectedTab != StatisticsTab.ACTIVITY },
    )

    /** A missing or unknown route argument falls back to the weekly view Inicio opens on. */
    private fun String?.toSalesPeriod(): SalesPeriod =
        SalesPeriod.entries.firstOrNull { it.name == this } ?: SalesPeriod.WEEKLY

    private data class TabQuery(
        val tab: StatisticsTab,
        val period: SalesPeriod,
        val categoryId: String?,
    )
}
