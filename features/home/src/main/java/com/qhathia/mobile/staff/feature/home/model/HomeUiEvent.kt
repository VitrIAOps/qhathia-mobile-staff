package com.qhathia.mobile.staff.feature.home.model

import com.qhathia.mobile.staff.core.domain.model.SalesPeriod

/** Intents the Inicio tab raises. */
sealed interface HomeUiEvent {

    /** Re-requests the summary and the suggestions for the selected period. */
    data object Refresh : HomeUiEvent

    /**
     * Inicio came back on screen (`ON_START`): from another tab, from a pushed screen, or with the app
     * returning from the background. Reloads like [Refresh], except for the first one, which the
     * initial load already covers.
     */
    data object ScreenStarted : HomeUiEvent

    /** Switches the summary card to [period] and loads it. */
    data class SelectPeriod(val period: SalesPeriod) : HomeUiEvent

    /** The header bell — a tab switch to Solicitudes, not a push on top of Inicio. */
    data object OpenNotifications : HomeUiEvent

    /** `Ver más` on the summary heading — opens the sales statistics on the selected period. */
    data object OpenSalesStatistics : HomeUiEvent
}
