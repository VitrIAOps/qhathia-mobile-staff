package com.qhathia.mobile.staff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qhathia.mobile.staff.core.domain.usecase.ObserveSessionUseCase
import com.qhathia.mobile.staff.core.domain.usecase.RegisterPushTokenUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
 *
 * It also holds the section a notification tap asked for until the shell can honour it. The state
 * lives here rather than in the Activity so it survives configuration changes and — more
 * importantly — the `Loading → Authenticated` transition: a push tapped while logged out is applied
 * once the merchant signs in.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    private val registerPushToken: RegisterPushTokenUseCase,
) : ViewModel() {

    private val _pendingDestination = MutableStateFlow<String?>(null)

    /** The section to jump to on the next composition of the shell, or null when there is none. */
    val pendingDestination: StateFlow<String?> = _pendingDestination.asStateFlow()

    /**
     * Records a destination requested from outside the app. Takes the already-extracted value rather
     * than an `Intent` so this stays a plain unit-testable ViewModel. A null (an ordinary launcher
     * start) leaves any pending destination alone.
     */
    fun onDeepLink(destination: String?) {
        if (destination != null) _pendingDestination.value = destination
    }

    /** Called by the shell once it has navigated, so the jump does not repeat. */
    fun onPendingDestinationConsumed() {
        _pendingDestination.value = null
    }

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
