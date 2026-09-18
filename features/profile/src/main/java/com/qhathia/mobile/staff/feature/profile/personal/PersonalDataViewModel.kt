package com.qhathia.mobile.staff.feature.profile.personal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qhathia.mobile.staff.core.domain.model.UserProfile
import com.qhathia.mobile.staff.core.domain.usecase.GetUserProfileUseCase
import com.qhathia.mobile.staff.feature.profile.common.LoadState
import com.qhathia.mobile.staff.feature.profile.common.afterFailure
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Read-only Datos personales: there is nothing to edit or save. */
data class PersonalDataUiState(
    val profile: LoadState<UserProfile> = LoadState.Loading,
)

sealed interface PersonalDataUiEvent {
    data object Retry : PersonalDataUiEvent
}

/** Loads the profile shown on Datos personales. */
@HiltViewModel
class PersonalDataViewModel @Inject constructor(
    private val getUserProfileUseCase: GetUserProfileUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PersonalDataUiState())
    val uiState: StateFlow<PersonalDataUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun onEvent(event: PersonalDataUiEvent) {
        when (event) {
            PersonalDataUiEvent.Retry -> load()
        }
    }

    private fun load() {
        viewModelScope.launch {
            if (_uiState.value.profile !is LoadState.Loaded) {
                _uiState.update { it.copy(profile = LoadState.Loading) }
            }
            getUserProfileUseCase()
                .onSuccess { profile -> _uiState.update { it.copy(profile = LoadState.Loaded(profile)) } }
                .onFailure { _uiState.update { it.copy(profile = it.profile.afterFailure()) } }
        }
    }
}
