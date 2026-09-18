package com.qhathia.mobile.staff.feature.profile.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qhathia.mobile.staff.core.domain.model.Subscription
import com.qhathia.mobile.staff.core.domain.usecase.GetSubscriptionUseCase
import com.qhathia.mobile.staff.feature.profile.common.LoadState
import com.qhathia.mobile.staff.feature.profile.common.afterFailure
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/** Mi suscripción. `Loaded(null)` means the store has no subscription. */
data class SubscriptionUiState(
    val subscription: LoadState<Subscription?> = LoadState.Loading,
    val now: Instant = Instant.EPOCH,
)

sealed interface SubscriptionUiEvent {
    data object Retry : SubscriptionUiEvent
}

/** Loads the plan shown on Mi suscripción. Payment sections have no data source. */
@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val getSubscriptionUseCase: GetSubscriptionUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubscriptionUiState(now = clock.instant()))
    val uiState: StateFlow<SubscriptionUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun onEvent(event: SubscriptionUiEvent) {
        when (event) {
            SubscriptionUiEvent.Retry -> load()
        }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    subscription = if (it.subscription is LoadState.Loaded) it.subscription else LoadState.Loading,
                    now = clock.instant(),
                )
            }
            getSubscriptionUseCase()
                .onSuccess { subscription ->
                    _uiState.update { it.copy(subscription = LoadState.Loaded(subscription)) }
                }
                .onFailure { _uiState.update { it.copy(subscription = it.subscription.afterFailure()) } }
        }
    }
}
