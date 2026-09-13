package com.qhathia.mobile.staff.feature.home.model

import com.qhathia.mobile.staff.core.domain.model.SalesPeriod

/** Intents the Inicio tab raises. */
sealed interface HomeUiEvent {

    /** Re-requests the summary and the suggestions for the selected period. */
    data object Refresh : HomeUiEvent

    /** Switches the summary card to [period] and loads it. */
    data class SelectPeriod(val period: SalesPeriod) : HomeUiEvent

    /** The header bell — a tab switch to Solicitudes, not a push on top of Inicio. */
    data object OpenNotifications : HomeUiEvent
}
