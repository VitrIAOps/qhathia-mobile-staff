package com.qhathia.mobile.staff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qhathia.mobile.staff.core.domain.usecase.ObserveSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Drives the start destination by observing whether a session exists. */
@HiltViewModel
class MainViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
) : ViewModel() {

    val authState: StateFlow<AuthState> = observeSession()
        .map { session -> if (session != null) AuthState.Authenticated else AuthState.Unauthenticated }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AuthState.Loading,
        )
}

/** Top-level authentication state used to choose the navigation start destination. */
enum class AuthState { Loading, Authenticated, Unauthenticated }
