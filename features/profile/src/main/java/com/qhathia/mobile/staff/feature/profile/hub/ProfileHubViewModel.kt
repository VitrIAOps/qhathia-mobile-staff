package com.qhathia.mobile.staff.feature.profile.hub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qhathia.mobile.staff.core.domain.model.UserRole
import com.qhathia.mobile.staff.core.domain.usecase.GetDevicesUseCase
import com.qhathia.mobile.staff.core.domain.usecase.GetNotificationPreferencesUseCase
import com.qhathia.mobile.staff.core.domain.usecase.GetSubscriptionUseCase
import com.qhathia.mobile.staff.core.domain.usecase.GetUserProfileUseCase
import com.qhathia.mobile.staff.core.domain.usecase.LogoutUseCase
import com.qhathia.mobile.staff.core.domain.usecase.ObserveSessionUseCase
import com.qhathia.mobile.staff.feature.profile.common.LoadState
import com.qhathia.mobile.staff.feature.profile.common.afterFailure
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

/**
 * Drives Mi perfil. A refresh reloads the profile, the notification counts and — for OWNER/ADMIN
 * only — the subscription and the tablets, all in parallel and each failing on its own. The role
 * comes from the session, so a SELLER never issues a request the backend would refuse.
 *
 * The screen refreshes on every resume, which is how a toggle changed in Notificaciones shows up in
 * the menu on return. A refresh already in flight absorbs the next request.
 */
@HiltViewModel
class ProfileHubViewModel @Inject constructor(
    private val observeSessionUseCase: ObserveSessionUseCase,
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val getSubscriptionUseCase: GetSubscriptionUseCase,
    private val getNotificationPreferencesUseCase: GetNotificationPreferencesUseCase,
    private val getDevicesUseCase: GetDevicesUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileHubUiState(now = clock.instant()))
    val uiState: StateFlow<ProfileHubUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<ProfileHubUiEffect>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val uiEffect: SharedFlow<ProfileHubUiEffect> = _uiEffect.asSharedFlow()

    private var refreshJob: Job? = null

    init {
        refresh()
    }

    fun onEvent(event: ProfileHubUiEvent) {
        when (event) {
            ProfileHubUiEvent.Refresh -> refresh()
            is ProfileHubUiEvent.Open -> _uiEffect.tryEmit(ProfileHubUiEffect.Navigate(event.destination))
            ProfileHubUiEvent.LogoutRequested ->
                _uiState.update { it.copy(isLogoutConfirmVisible = true) }
            ProfileHubUiEvent.LogoutDismissed ->
                _uiState.update { it.copy(isLogoutConfirmVisible = false) }
            ProfileHubUiEvent.LogoutConfirmed -> {
                _uiState.update { it.copy(isLogoutConfirmVisible = false) }
                viewModelScope.launch { logoutUseCase() }
            }
        }
    }

    private fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            val session = observeSessionUseCase().first()
            val role = session?.user?.role ?: UserRole.UNKNOWN
            _uiState.update {
                it.copy(
                    sessionName = session?.user?.fullName.orEmpty(),
                    sessionEmail = session?.user?.email.orEmpty(),
                    role = role,
                    now = clock.instant(),
                )
            }
            val jobs = buildList {
                add(launch { loadProfile() })
                add(launch { loadAlertCounts() })
                if (_uiState.value.canManageStore) {
                    add(launch { loadSubscription() })
                    add(launch { loadConnectedTablets() })
                }
            }
            jobs.forEach { it.join() }
        }
    }

    private suspend fun loadProfile() {
        getUserProfileUseCase()
            .onSuccess { profile -> _uiState.update { it.copy(profile = LoadState.Loaded(profile)) } }
            .onFailure { _uiState.update { it.copy(profile = it.profile.afterFailure()) } }
    }

    private suspend fun loadAlertCounts() {
        getNotificationPreferencesUseCase()
            .onSuccess { prefs ->
                _uiState.update { it.copy(alertCounts = AlertCounts(prefs.activeCount, prefs.totalCount)) }
            }
    }

    private suspend fun loadSubscription() {
        getSubscriptionUseCase()
            .onSuccess { subscription ->
                _uiState.update { it.copy(subscription = LoadState.Loaded(subscription)) }
            }
            .onFailure { _uiState.update { it.copy(subscription = it.subscription.afterFailure()) } }
    }

    private suspend fun loadConnectedTablets() {
        getDevicesUseCase()
            .onSuccess { devices ->
                val now = clock.instant()
                _uiState.update { state ->
                    state.copy(connectedTablets = devices.count { it.isConnected(now) })
                }
            }
    }
}
