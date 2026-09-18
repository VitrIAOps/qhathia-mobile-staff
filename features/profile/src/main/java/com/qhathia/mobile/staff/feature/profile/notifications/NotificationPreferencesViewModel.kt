package com.qhathia.mobile.staff.feature.profile.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qhathia.mobile.staff.core.domain.model.NotificationPreference
import com.qhathia.mobile.staff.core.domain.model.NotificationPreferences
import com.qhathia.mobile.staff.core.domain.usecase.GetNotificationPreferencesUseCase
import com.qhathia.mobile.staff.core.domain.usecase.SetNotificationPreferenceUseCase
import com.qhathia.mobile.staff.feature.profile.common.LoadState
import com.qhathia.mobile.staff.feature.profile.common.valueOrNull
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Notificaciones. [saving] holds the toggles whose save is in flight; each of those is locked until
 * its request ends, while the rest stay usable.
 */
data class NotificationPreferencesUiState(
    val preferences: LoadState<NotificationPreferences> = LoadState.Loading,
    val saving: Set<NotificationPreference> = emptySet(),
)

sealed interface NotificationPreferencesUiEvent {
    data object Retry : NotificationPreferencesUiEvent
    data class Toggle(val preference: NotificationPreference, val enabled: Boolean) :
        NotificationPreferencesUiEvent
}

sealed interface NotificationPreferencesUiEffect {
    data object ShowSaveError : NotificationPreferencesUiEffect
}

/**
 * Drives Notificaciones with optimistic, per-toggle saves: the switch moves at once, only that field
 * is sent, and it springs back if the save fails. The backend answers with the full set, which is
 * adopted — except for toggles whose own save is still in flight, which keep the value the merchant
 * just chose so they never flicker.
 */
@HiltViewModel
class NotificationPreferencesViewModel @Inject constructor(
    private val getNotificationPreferencesUseCase: GetNotificationPreferencesUseCase,
    private val setNotificationPreferenceUseCase: SetNotificationPreferenceUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationPreferencesUiState())
    val uiState: StateFlow<NotificationPreferencesUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<NotificationPreferencesUiEffect>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val uiEffect: SharedFlow<NotificationPreferencesUiEffect> = _uiEffect.asSharedFlow()

    init {
        load()
    }

    fun onEvent(event: NotificationPreferencesUiEvent) {
        when (event) {
            NotificationPreferencesUiEvent.Retry -> load()
            is NotificationPreferencesUiEvent.Toggle -> toggle(event.preference, event.enabled)
        }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(preferences = LoadState.Loading) }
            getNotificationPreferencesUseCase()
                .onSuccess { prefs -> _uiState.update { it.copy(preferences = LoadState.Loaded(prefs)) } }
                .onFailure { _uiState.update { it.copy(preferences = LoadState.Error) } }
        }
    }

    private fun toggle(preference: NotificationPreference, enabled: Boolean) {
        val state = _uiState.value
        val current = state.preferences.valueOrNull ?: return
        if (preference in state.saving || current.isEnabled(preference) == enabled) return

        _uiState.update {
            it.copy(
                preferences = LoadState.Loaded(current.with(preference, enabled)),
                saving = it.saving + preference,
            )
        }
        viewModelScope.launch {
            setNotificationPreferenceUseCase(preference, enabled)
                .onSuccess { applied ->
                    _uiState.update { latest ->
                        val stillSaving = latest.saving - preference
                        val shown = latest.preferences.valueOrNull
                        val merged = stillSaving.fold(applied) { acc, pending ->
                            shown?.let { acc.with(pending, it.isEnabled(pending)) } ?: acc
                        }
                        latest.copy(preferences = LoadState.Loaded(merged), saving = stillSaving)
                    }
                }
                .onFailure {
                    _uiState.update { latest ->
                        val shown = latest.preferences.valueOrNull
                        latest.copy(
                            preferences = shown?.let { LoadState.Loaded(it.with(preference, !enabled)) }
                                ?: latest.preferences,
                            saving = latest.saving - preference,
                        )
                    }
                    _uiEffect.tryEmit(NotificationPreferencesUiEffect.ShowSaveError)
                }
        }
    }
}

/** A copy with [preference] set to [enabled] and the active count kept consistent. */
private fun NotificationPreferences.with(
    preference: NotificationPreference,
    enabled: Boolean,
): NotificationPreferences {
    if (isEnabled(preference) == enabled) return this
    return copy(
        values = values + (preference to enabled),
        activeCount = (activeCount + if (enabled) 1 else -1).coerceIn(0, totalCount),
    )
}
