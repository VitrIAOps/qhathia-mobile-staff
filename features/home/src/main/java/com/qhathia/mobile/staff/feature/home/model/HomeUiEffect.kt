package com.qhathia.mobile.staff.feature.home.model

import com.qhathia.mobile.staff.core.domain.model.SalesPeriod

/** One-shot side-effects of the Inicio tab. */
sealed interface HomeUiEffect {

    /**
     * Raised by the bell. The shell fulfils it with the bottom bar's own navigation options, so it
     * behaves as a tab switch rather than pushing a second Solicitudes destination onto Inicio.
     */
    data object NavigateToOrderRequests : HomeUiEffect

    /**
     * Raised by `Ver más`. Unlike the bell it is a push on top of Inicio, so going back returns
     * here with the selection intact. [period] is the period the card was showing.
     */
    data class NavigateToSalesStatistics(val period: SalesPeriod) : HomeUiEffect
}
