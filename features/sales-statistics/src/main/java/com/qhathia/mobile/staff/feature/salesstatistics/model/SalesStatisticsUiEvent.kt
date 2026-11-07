package com.qhathia.mobile.staff.feature.salesstatistics.model

import com.qhathia.mobile.staff.core.domain.model.SalesPeriod

/** Intents the "Resumen de ventas" screen raises. */
sealed interface SalesStatisticsUiEvent {

    /** Shows [tab], loading it only if it has nothing for the current filters. */
    data class SelectTab(val tab: StatisticsTab) : SalesStatisticsUiEvent

    /** Changes the period for every tab and reloads the one on screen. */
    data class SelectPeriod(val period: SalesPeriod) : SalesStatisticsUiEvent

    /** Changes the category (null = "Todos") and reloads the tab on screen if it uses it. */
    data class SelectCategory(val categoryId: String?) : SalesStatisticsUiEvent

    /** Expands or collapses the "Filtros" panel. */
    data object ToggleFilters : SalesStatisticsUiEvent

    /** Re-requests the tab on screen for the current filters. */
    data object Retry : SalesStatisticsUiEvent

    /** The top-bar back control. */
    data object Back : SalesStatisticsUiEvent

    /** The top-bar share control: shares the tab on screen, once it has loaded. */
    data object Share : SalesStatisticsUiEvent

    /**
     * The screen came back (`ON_START`): from the background or a pushed screen. Reloads the tab on
     * screen, except the first time, which the initial load covers.
     */
    data object ScreenStarted : SalesStatisticsUiEvent
}
