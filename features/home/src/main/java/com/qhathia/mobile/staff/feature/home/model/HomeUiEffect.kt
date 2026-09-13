package com.qhathia.mobile.staff.feature.home.model

/** One-shot side-effects of the Inicio tab. */
sealed interface HomeUiEffect {

    /**
     * Raised by the bell. The shell fulfils it with the bottom bar's own navigation options, so it
     * behaves as a tab switch rather than pushing a second Solicitudes destination onto Inicio.
     */
    data object NavigateToOrderRequests : HomeUiEffect
}
