package com.qhathia.mobile.staff.feature.profile.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qhathia.mobile.staff.core.domain.model.AppLanguage
import com.qhathia.mobile.staff.core.domain.model.UserRole
import com.qhathia.mobile.staff.core.domain.usecase.GetStoreSettingsUseCase
import com.qhathia.mobile.staff.core.domain.usecase.ObserveAppLanguageUseCase
import com.qhathia.mobile.staff.core.domain.usecase.ObserveSessionUseCase
import com.qhathia.mobile.staff.core.domain.usecase.SetAppLanguageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
 */
data class AppSettingsUiState(
    val language: AppLanguage? = null,
)

sealed interface AppSettingsUiEvent {
    data class SelectLanguage(val language: AppLanguage) : AppSettingsUiEvent
}

/**
 * Drives Configuración, whose only setting is the app language.
 *
 * The language is owned by this device: the selector only ever reflects the local value, and the
 * store's `language` never overwrites it. Selecting a language persists it (the app locale follows
 * from the shell observing the same value) and mirrors it to the store as best effort. Because that
 * mirror may have failed before, opening the screen reads the store and, for OWNER/ADMIN, re-sends
 * the local value whenever the store disagrees.
 *
 * That read is a background sync the merchant cannot see, so a failure is not surfaced: the device
 * language has already applied, and the next visit tries again.
 */
@HiltViewModel
class AppSettingsViewModel @Inject constructor(
    observeAppLanguageUseCase: ObserveAppLanguageUseCase,
    private val observeSessionUseCase: ObserveSessionUseCase,
    private val setAppLanguageUseCase: SetAppLanguageUseCase,
    private val getStoreSettingsUseCase: GetStoreSettingsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppSettingsUiState())
    val uiState: StateFlow<AppSettingsUiState> = _uiState.asStateFlow()

    init {
        observeAppLanguageUseCase()
            .onEach { language -> _uiState.update { it.copy(language = language) } }
            .launchIn(viewModelScope)
        viewModelScope.launch { reconcileStoreLanguage() }
    }

    fun onEvent(event: AppSettingsUiEvent) {
        when (event) {
            is AppSettingsUiEvent.SelectLanguage -> selectLanguage(event.language)
        }
    }

    /** Re-sends the device language if the store holds another one. OWNER/ADMIN only; silent. */
    private suspend fun reconcileStoreLanguage() {
        val role = observeSessionUseCase().first()?.user?.role
        if (role != UserRole.OWNER && role != UserRole.ADMIN) return
        val store = getStoreSettingsUseCase().getOrNull() ?: return
        val local = _uiState.value.language ?: return
        if (store.language != local) setAppLanguageUseCase(local)
    }

    private fun selectLanguage(language: AppLanguage) {
        if (_uiState.value.language == language) return
        viewModelScope.launch { setAppLanguageUseCase(language) }
    }
}
