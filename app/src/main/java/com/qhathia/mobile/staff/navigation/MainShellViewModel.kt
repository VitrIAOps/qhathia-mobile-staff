package com.qhathia.mobile.staff.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qhathia.mobile.staff.core.domain.usecase.ObservePendingOrderCountUseCase
import com.qhathia.mobile.staff.core.domain.usecase.RefreshPendingOrderCountUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Feeds the shell's Solicitudes tab badge.
 *
 * The count is the app-wide one, so the badge, the Inicio bell and the `Pendientes (N)` filter
 * label are the same number by construction. It is refreshed once on entering the shell — a
 * logged-out shell is never composed, so this is also what stops a count from an earlier session
 * being shown to the next one.
 */
@HiltViewModel
class MainShellViewModel @Inject constructor(
    observePendingOrderCount: ObservePendingOrderCountUseCase,
    private val refreshPendingOrderCount: RefreshPendingOrderCountUseCase,
) : ViewModel() {

    val pendingOrderCount: StateFlow<Int> = observePendingOrderCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0,
        )

    init {
        viewModelScope.launch { refreshPendingOrderCount() }
    }
}
