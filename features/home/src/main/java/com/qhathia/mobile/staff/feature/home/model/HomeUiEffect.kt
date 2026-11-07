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

    /** Raised by `Agregar`: Registro with `Nombre` set to [prefillName]. The shell opens it over Inventario. */
    data class NavigateToProductRegister(val prefillName: String) : HomeUiEffect

    /** Raised by `Reabastecer`: that product's Detalle. The shell opens it over Inventario. */
    data class NavigateToProductDetail(val productId: String) : HomeUiEffect
}
