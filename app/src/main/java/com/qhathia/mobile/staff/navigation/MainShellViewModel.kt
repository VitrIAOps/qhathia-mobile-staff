package com.qhathia.mobile.staff.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qhathia.mobile.staff.core.domain.usecase.ObservePendingOrderCountUseCase
import com.qhathia.mobile.staff.core.domain.usecase.ReconcileOrderNotificationsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Feeds the shell's Solicitudes tab badge and keeps the tray's order notifications honest.
 *
 * The count is the app-wide one, so the badge, the Inicio bell and the `Pendientes (N)` filter
 * label are the same number by construction. Each time the shell comes to the foreground
 * [onForegrounded] reconciles the tray with the server's pending orders, which also refreshes that
 * count. A logged-out or locked shell is never composed, so this also stops a count from an earlier
 * session being shown to the next one.
 */
@HiltViewModel
class MainShellViewModel @Inject constructor(
    observePendingOrderCount: ObservePendingOrderCountUseCase,
    private val reconcileOrderNotifications: ReconcileOrderNotificationsUseCase,
) : ViewModel() {

    val pendingOrderCount: StateFlow<Int> = observePendingOrderCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0,
        )

    /** The shell reached `ON_START`: on first entry, after unlocking, or back from the background. */
    fun onForegrounded() {
        viewModelScope.launch { reconcileOrderNotifications() }
    }
}
