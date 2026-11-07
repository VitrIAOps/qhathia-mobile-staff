package com.qhathia.mobile.staff.feature.profile.personal

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qhathia.mobile.staff.core.domain.model.UpdateProfileError
import com.qhathia.mobile.staff.core.domain.model.UpdateProfileException
import com.qhathia.mobile.staff.core.domain.model.UserProfile
import com.qhathia.mobile.staff.core.domain.usecase.GetUserProfileUseCase
import com.qhathia.mobile.staff.core.domain.usecase.UpdateUserProfileUseCase
import com.qhathia.mobile.staff.feature.profile.R
import com.qhathia.mobile.staff.feature.profile.common.LoadState
import com.qhathia.mobile.staff.feature.profile.common.afterFailure
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
 * Datos personales. [profile] holds the last saved values; [fullName] and [phone] are what the
 * merchant is typing. Field errors are derived from the text, and shown only once a field differs
 * from its saved value, so a legacy value that breaks today's rules does not open the screen in red.
 * [bannerErrorRes] carries save failures that belong to no field. [isDiscardDialogVisible] is the
 * "¿Descartar cambios?" asked when leaving with unsaved changes.
 */
data class PersonalDataUiState(
    val profile: LoadState<UserProfile> = LoadState.Loading,
    val fullName: String = "",
    val phone: String = "",
    val isSaving: Boolean = false,
    @param:StringRes val bannerErrorRes: Int? = null,
    val isDiscardDialogVisible: Boolean = false,
) {
    private val saved: UserProfile? get() = profile.valueOrNull

    private val nameChanged: Boolean
        get() = saved != null && fullName.trim() != saved?.fullName

    private val phoneChanged: Boolean
        get() = saved != null && phone != saved?.phone.orEmpty()

    @get:StringRes
    val nameErrorRes: Int?
        get() = if (nameChanged && !PersonalDataValidation.isValidName(fullName)) {
            R.string.personal_error_name
        } else {
            null
        }

    @get:StringRes
    val phoneErrorRes: Int?
        get() = if (phoneChanged && !PersonalDataValidation.isValidPhone(phone)) {
            R.string.personal_error_phone
        } else {
            null
        }

    val isDirty: Boolean get() = nameChanged || phoneChanged

    /** A saved phone was emptied: saving removes it, and the field says so. */
    val isRemovingPhone: Boolean
        get() = phone.isEmpty() && !saved?.phone.isNullOrEmpty()

    val canSave: Boolean
        get() = isDirty && nameErrorRes == null && phoneErrorRes == null && !isSaving
}

sealed interface PersonalDataUiEvent {
    data class NameChanged(val value: String) : PersonalDataUiEvent
    data class PhoneChanged(val value: String) : PersonalDataUiEvent
    data object Save : PersonalDataUiEvent
    data object Retry : PersonalDataUiEvent

    /** System back or the top bar's back. */
    data object BackRequested : PersonalDataUiEvent
    data object DiscardConfirmed : PersonalDataUiEvent
    data object DiscardDismissed : PersonalDataUiEvent
}

sealed interface PersonalDataUiEffect {
    data object Saved : PersonalDataUiEffect
    data object NavigateBack : PersonalDataUiEffect
}

/**
 * Loads the profile and saves name and phone changes through `PUT /users/me`. Only the fields that
 * changed are sent; the email is never part of it.
 */
@HiltViewModel
class PersonalDataViewModel @Inject constructor(
    private val getUserProfileUseCase: GetUserProfileUseCase,
    private val updateUserProfileUseCase: UpdateUserProfileUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PersonalDataUiState())
    val uiState: StateFlow<PersonalDataUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<PersonalDataUiEffect>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val uiEffect: SharedFlow<PersonalDataUiEffect> = _uiEffect.asSharedFlow()

    init {
        load()
    }

    fun onEvent(event: PersonalDataUiEvent) {
        when (event) {
            is PersonalDataUiEvent.NameChanged -> _uiState.update {
                it.copy(fullName = event.value, bannerErrorRes = null)
            }
            is PersonalDataUiEvent.PhoneChanged -> _uiState.update {
                it.copy(phone = PersonalDataValidation.sanitizePhone(event.value), bannerErrorRes = null)
            }
            PersonalDataUiEvent.Save -> save()
            PersonalDataUiEvent.Retry -> load()
            PersonalDataUiEvent.BackRequested -> onBackRequested()
            PersonalDataUiEvent.DiscardConfirmed -> {
                _uiState.update { it.copy(isDiscardDialogVisible = false) }
                _uiEffect.tryEmit(PersonalDataUiEffect.NavigateBack)
            }
            PersonalDataUiEvent.DiscardDismissed -> _uiState.update { it.copy(isDiscardDialogVisible = false) }
        }
    }

    /** Leaving mid-save would drop its result, so back waits; unsaved changes ask first. */
    private fun onBackRequested() {
        val state = _uiState.value
        when {
            state.isSaving -> Unit
            state.isDirty -> _uiState.update { it.copy(isDiscardDialogVisible = true) }
            else -> _uiEffect.tryEmit(PersonalDataUiEffect.NavigateBack)
        }
    }

    private fun load() {
        viewModelScope.launch {
            if (_uiState.value.profile !is LoadState.Loaded) {
                _uiState.update { it.copy(profile = LoadState.Loading) }
            }
            getUserProfileUseCase()
                .onSuccess { profile -> _uiState.update { it.withSaved(profile) } }
                .onFailure { _uiState.update { it.copy(profile = it.profile.afterFailure()) } }
        }
    }

    private fun save() {
        val state = _uiState.value
        val saved = state.profile.valueOrNull ?: return
        if (!state.canSave) return

        val name = state.fullName.trim()
        val fullName = name.takeIf { it != saved.fullName }
        val phone = state.phone.takeIf { it != saved.phone.orEmpty() }
        _uiState.update { it.copy(isSaving = true, bannerErrorRes = null) }
        viewModelScope.launch {
            updateUserProfileUseCase(fullName = fullName, phone = phone)
                .onSuccess { profile ->
                    _uiState.update { it.withSaved(profile).copy(isSaving = false) }
                    _uiEffect.tryEmit(PersonalDataUiEffect.Saved)
                }
                .onFailure { throwable ->
                    val error = (throwable as? UpdateProfileException)?.error
                    _uiState.update { it.copy(isSaving = false, bannerErrorRes = error.bannerRes()) }
                }
        }
    }

    /** Takes [profile] as the saved values and resets the fields to them. */
    private fun PersonalDataUiState.withSaved(profile: UserProfile) = copy(
        profile = LoadState.Loaded(profile),
        fullName = profile.fullName,
        phone = profile.phone.orEmpty(),
    )

    @StringRes
    private fun UpdateProfileError?.bannerRes(): Int = when (this) {
        UpdateProfileError.Validation -> R.string.personal_error_save_validation
        UpdateProfileError.Network -> R.string.personal_error_save_network
        is UpdateProfileError.Unknown, null -> R.string.personal_error_save_unknown
    }
}
