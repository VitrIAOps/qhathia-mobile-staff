package com.qhathia.mobile.staff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qhathia.mobile.staff.core.domain.usecase.ObserveSessionUseCase
import com.qhathia.mobile.staff.core.domain.usecase.RegisterPushTokenUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the start destination by observing whether a session exists, and registers this device's
 * push token whenever the session transitions to authenticated (covers login and authenticated app
 * start). Registration is fire-and-forget and fail-open.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    private val registerPushToken: RegisterPushTokenUseCase,
) : ViewModel() {

    val authState: StateFlow<AuthState> = observeSession()
        .map { session -> session != null }
        .distinctUntilChanged()
        .onEach { authenticated ->
            if (authenticated) {
                viewModelScope.launch { registerPushToken() }
            }
        }
        .map { authenticated -> if (authenticated) AuthState.Authenticated else AuthState.Unauthenticated }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AuthState.Loading,
        )
}

/** Top-level authentication state used to choose the navigation start destination. */
enum class AuthState { Loading, Authenticated, Unauthenticated }
