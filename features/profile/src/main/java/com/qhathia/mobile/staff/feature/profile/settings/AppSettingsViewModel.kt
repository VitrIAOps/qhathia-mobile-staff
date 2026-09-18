package com.qhathia.mobile.staff.feature.profile.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qhathia.mobile.staff.core.domain.model.AppLanguage
import com.qhathia.mobile.staff.core.domain.model.StoreSettings
import com.qhathia.mobile.staff.core.domain.model.UserRole
import com.qhathia.mobile.staff.core.domain.usecase.GetStoreSettingsUseCase
import com.qhathia.mobile.staff.core.domain.usecase.ObserveAppLanguageUseCase
import com.qhathia.mobile.staff.core.domain.usecase.ObserveSessionUseCase
import com.qhathia.mobile.staff.core.domain.usecase.SetAppLanguageUseCase
import com.qhathia.mobile.staff.core.domain.usecase.SetPricesIncludeIgvUseCase
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Configuración.
 *
 * @property language the device-owned language; `null` only before the app has stored its default.
 * @property canEditStore IGV and the store language mirror are OWNER/ADMIN only.
 */
data class AppSettingsUiState(
    val language: AppLanguage? = null,
    val store: LoadState<StoreSettings> = LoadState.Loading,
    val canEditStore: Boolean = false,
    val isIgvSaving: Boolean = false,
)

sealed interface AppSettingsUiEvent {
    data class SelectLanguage(val language: AppLanguage) : AppSettingsUiEvent
    data class ToggleIgv(val pricesIncludeIgv: Boolean) : AppSettingsUiEvent
    data object RetryStore : AppSettingsUiEvent
}

sealed interface AppSettingsUiEffect {
    data object ShowIgvSaveError : AppSettingsUiEffect
}

/**
 * Drives Configuración.
 *
 * The language is owned by this device: the selector only ever reflects the local value, and the
 * store's `language` never overwrites it. Selecting a language persists it (the app locale follows
 * from the shell observing the same value) and mirrors it to the store as best effort. Because that
 * mirror may have failed before, opening the screen re-sends the local value whenever the store
 * disagrees.
 *
 * IGV is an optimistic toggle that springs back if the store update fails; nothing is recalculated.
 */
@HiltViewModel
class AppSettingsViewModel @Inject constructor(
    observeAppLanguageUseCase: ObserveAppLanguageUseCase,
    private val observeSessionUseCase: ObserveSessionUseCase,
    private val setAppLanguageUseCase: SetAppLanguageUseCase,
    private val getStoreSettingsUseCase: GetStoreSettingsUseCase,
    private val setPricesIncludeIgvUseCase: SetPricesIncludeIgvUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppSettingsUiState())
    val uiState: StateFlow<AppSettingsUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<AppSettingsUiEffect>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val uiEffect: SharedFlow<AppSettingsUiEffect> = _uiEffect.asSharedFlow()

    init {
        observeAppLanguageUseCase()
            .onEach { language -> _uiState.update { it.copy(language = language) } }
            .launchIn(viewModelScope)
        viewModelScope.launch {
            val role = observeSessionUseCase().first()?.user?.role
            _uiState.update { it.copy(canEditStore = role == UserRole.OWNER || role == UserRole.ADMIN) }
            loadStore(reconcileLanguage = true)
        }
    }

    fun onEvent(event: AppSettingsUiEvent) {
        when (event) {
            is AppSettingsUiEvent.SelectLanguage -> selectLanguage(event.language)
            is AppSettingsUiEvent.ToggleIgv -> toggleIgv(event.pricesIncludeIgv)
            AppSettingsUiEvent.RetryStore -> viewModelScope.launch { loadStore(reconcileLanguage = true) }
        }
    }

    private suspend fun loadStore(reconcileLanguage: Boolean) {
        _uiState.update { it.copy(store = LoadState.Loading) }
        getStoreSettingsUseCase()
            .onSuccess { store ->
                _uiState.update { it.copy(store = LoadState.Loaded(store)) }
                val local = _uiState.value.language
                if (reconcileLanguage && _uiState.value.canEditStore && local != null && store.language != local) {
                    setAppLanguageUseCase(local)
                }
            }
            .onFailure { _uiState.update { it.copy(store = LoadState.Error) } }
    }

    private fun selectLanguage(language: AppLanguage) {
        if (_uiState.value.language == language) return
        viewModelScope.launch { setAppLanguageUseCase(language) }
    }

    private fun toggleIgv(pricesIncludeIgv: Boolean) {
        val state = _uiState.value
        val store = state.store.valueOrNull ?: return
        if (!state.canEditStore || state.isIgvSaving || store.pricesIncludeIgv == pricesIncludeIgv) return

        _uiState.update {
            it.copy(store = LoadState.Loaded(store.copy(pricesIncludeIgv = pricesIncludeIgv)), isIgvSaving = true)
        }
        viewModelScope.launch {
            setPricesIncludeIgvUseCase(pricesIncludeIgv)
                .onSuccess { applied ->
                    _uiState.update { it.copy(store = LoadState.Loaded(applied), isIgvSaving = false) }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(store = LoadState.Loaded(store), isIgvSaving = false)
                    }
                    _uiEffect.tryEmit(AppSettingsUiEffect.ShowIgvSaveError)
                }
        }
    }
}
