package com.qhathia.mobile.staff.feature.salesstatistics.model

/** One-shot side-effects of the "Resumen de ventas" screen. */
sealed interface SalesStatisticsUiEffect {

    /** Leave the screen; the shell pops it, returning to Inicio. */
    data object NavigateBack : SalesStatisticsUiEffect

    /** Open the system share sheet with a text summary of [tab], built by the screen. */
    data class Share(val tab: StatisticsTab) : SalesStatisticsUiEffect
}
