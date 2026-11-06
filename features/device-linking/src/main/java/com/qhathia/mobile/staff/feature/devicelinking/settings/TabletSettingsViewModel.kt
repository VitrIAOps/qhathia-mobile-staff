package com.qhathia.mobile.staff.feature.devicelinking.settings

import com.qhathia.mobile.staff.feature.devicelinking.model.MAX_TABLET_NAME_LENGTH
import com.qhathia.mobile.staff.core.domain.usecase.RenameDeviceUseCase
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qhathia.mobile.staff.core.domain.model.DeviceSettingsChange
import com.qhathia.mobile.staff.core.domain.model.DeviceSettingsError
import com.qhathia.mobile.staff.core.domain.model.DeviceSettingsException
import com.qhathia.mobile.staff.core.domain.usecase.GetDeviceSettingsUseCase
import com.qhathia.mobile.staff.core.domain.usecase.GetDevicesUseCase
import com.qhathia.mobile.staff.core.domain.usecase.UnlinkDeviceUseCase
import com.qhathia.mobile.staff.core.domain.usecase.UpdateDeviceSettingsUseCase
import com.qhathia.mobile.staff.feature.devicelinking.R
import com.qhathia.mobile.staff.feature.devicelinking.navigation.TABLET_SETTINGS_ARG_ID
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
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
 * Drives Ajustes de la tablet. The settings and the tablet itself load in parallel; only a settings
 * failure blocks the screen (a 404 means the tablet is no longer linked). Switches and brightness
 * save on change, like Notificaciones: the control moves at once, one request per control at a
 * time, and a failure puts back only that control. The exit PIN goes through its own dialog.
 * Unlinking the tablet, at the end of the screen, asks for confirmation first.
 */
@HiltViewModel
class TabletSettingsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getDeviceSettingsUseCase: GetDeviceSettingsUseCase,
    private val getDevicesUseCase: GetDevicesUseCase,
    private val updateDeviceSettingsUseCase: UpdateDeviceSettingsUseCase,
    private val unlinkDeviceUseCase: UnlinkDeviceUseCase,
    private val renameDeviceUseCase: RenameDeviceUseCase,
) : ViewModel() {

    private val deviceId: String = checkNotNull(savedStateHandle[TABLET_SETTINGS_ARG_ID]) {
        "TabletSettingsViewModel requires a '$TABLET_SETTINGS_ARG_ID' route argument"
    }

    private val _uiState = MutableStateFlow(TabletSettingsUiState())
    val uiState: StateFlow<TabletSettingsUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<TabletSettingsUiEffect>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val uiEffect: SharedFlow<TabletSettingsUiEffect> = _uiEffect.asSharedFlow()

    init {
        load()
    }

    fun onEvent(event: TabletSettingsUiEvent) {
        when (event) {
            TabletSettingsUiEvent.Retry -> load()
            is TabletSettingsUiEvent.SettingChanged -> change(event.change)
            TabletSettingsUiEvent.OpenPinDialog ->
                _uiState.update { if (it.settings == null) it else it.copy(pinDialog = ExitPinDialogState()) }
            TabletSettingsUiEvent.DismissPinDialog ->
                _uiState.update { if (it.pinDialog?.isSaving == true) it else it.copy(pinDialog = null) }
            is TabletSettingsUiEvent.PinChanged ->
                updateDialog { it.copy(pin = event.value.asPinInput(), errorRes = null) }
            is TabletSettingsUiEvent.PinConfirmationChanged ->
                updateDialog { it.copy(confirmation = event.value.asPinInput(), errorRes = null) }
            TabletSettingsUiEvent.SavePin -> savePin()
            TabletSettingsUiEvent.RemovePinRequested ->
                updateDialog { if (it.isSaving) it else it.copy(isConfirmingRemoval = true, errorRes = null) }
            TabletSettingsUiEvent.RemovePinCancelled ->
                updateDialog { if (it.isSaving) it else it.copy(isConfirmingRemoval = false) }
            TabletSettingsUiEvent.RemovePinConfirmed -> removePin()
            TabletSettingsUiEvent.UnlinkRequested ->
                _uiState.update { if (it.isUnlinking) it else it.copy(isUnlinkConfirmVisible = true) }
            TabletSettingsUiEvent.UnlinkDismissed -> _uiState.update { it.copy(isUnlinkConfirmVisible = false) }
            TabletSettingsUiEvent.UnlinkConfirmed -> unlink()
            TabletSettingsUiEvent.RenameClicked ->
                _uiState.update { state ->
                    val device = state.device ?: return@update state
                    val current = device.name.orEmpty()
                    state.copy(renameDialog = RenameDialogState(value = current, current = current))
                }
            is TabletSettingsUiEvent.RenameChanged ->
                updateRename { it.copy(value = event.value.take(MAX_TABLET_NAME_LENGTH), errorRes = null) }
            TabletSettingsUiEvent.RenameConfirmed -> rename()
            TabletSettingsUiEvent.RenameDismissed ->
                _uiState.update { if (it.renameDialog?.isSaving == true) it else it.copy(renameDialog = null) }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(load = TabletSettingsLoad.LOADING) }
            val devices = async { getDevicesUseCase() }
            val settings = getDeviceSettingsUseCase(deviceId)
            val device = devices.await().getOrNull()?.firstOrNull { it.id == deviceId }
            _uiState.update { state ->
                settings.fold(
                    onSuccess = { state.copy(load = TabletSettingsLoad.LOADED, settings = it, device = device) },
                    onFailure = { error ->
                        val notFound = (error as? DeviceSettingsException)?.error == DeviceSettingsError.NotFound
                        state.copy(load = if (notFound) TabletSettingsLoad.NOT_FOUND else TabletSettingsLoad.ERROR)
                    },
                )
            }
        }
    }

    private fun change(change: DeviceSettingsChange) {
        val key = change.key() ?: return
        val current = _uiState.value
        val previous = current.settings ?: return
        if (key in current.saving) return
        if (previous.applied(change) == previous) return

        _uiState.update { it.copy(settings = it.settings?.applied(change), saving = it.saving + key) }
        viewModelScope.launch {
            updateDeviceSettingsUseCase(deviceId, change)
                .onSuccess { server ->
                    // The server's answer is the truth, except for controls whose own change is
                    // still in flight: those keep what the merchant just chose.
                    _uiState.update { latest ->
                        val stillSaving = latest.saving - key
                        val shown = latest.settings ?: server
                        val merged = stillSaving.fold(server) { acc, pending -> acc.withValueOf(pending, shown) }
                        latest.copy(settings = merged, saving = stillSaving)
                    }
                }
                .onFailure {
                    _uiState.update { latest ->
                        latest.copy(
                            settings = latest.settings?.withValueOf(key, previous),
                            saving = latest.saving - key,
                        )
                    }
                    _uiEffect.emit(TabletSettingsUiEffect.ShowMessage(R.string.tablet_settings_save_error))
                }
        }
    }

    private fun savePin() {
        val dialog = _uiState.value.pinDialog ?: return
        if (!dialog.canSave) return
        submitPin(DeviceSettingsChange.SetExitPin(dialog.pin), R.string.tablet_pin_saved)
    }

    private fun removePin() {
        val dialog = _uiState.value.pinDialog ?: return
        if (dialog.isSaving || !dialog.isConfirmingRemoval) return
        submitPin(DeviceSettingsChange.RemoveExitPin, R.string.tablet_pin_removed)
    }

    /** Sends a PIN change; the dialog closes on success and stays open, digits kept, on failure. */
    private fun submitPin(change: DeviceSettingsChange, successRes: Int) {
        updateDialog { it.copy(isSaving = true, errorRes = null) }
        viewModelScope.launch {
            updateDeviceSettingsUseCase(deviceId, change)
                .onSuccess { server ->
                    _uiState.update { latest ->
                        // Only the PIN flag is taken from the answer; toggles in flight keep theirs.
                        latest.copy(
                            settings = latest.settings?.copy(exitPinSet = server.exitPinSet) ?: server,
                            pinDialog = null,
                        )
                    }
                    _uiEffect.emit(TabletSettingsUiEffect.ShowMessage(successRes))
                }
                .onFailure {
                    updateDialog { it.copy(isSaving = false, isConfirmingRemoval = false, errorRes = R.string.tablet_pin_error) }
                }
        }
    }

    private fun unlink() {
        val state = _uiState.value
        if (!state.isUnlinkConfirmVisible || state.isUnlinking) return
        _uiState.update { it.copy(isUnlinkConfirmVisible = false, isUnlinking = true) }
        viewModelScope.launch {
            unlinkDeviceUseCase(deviceId)
                .onSuccess { _uiEffect.emit(TabletSettingsUiEffect.Unlinked(deviceId)) }
                .onFailure {
                    _uiState.update { it.copy(isUnlinking = false) }
                    _uiEffect.emit(TabletSettingsUiEffect.ShowMessage(R.string.device_unlink_error))
                }
        }
    }

    /** Renames the tablet; a 404 means it left the store, which the whole screen then says. */
    private fun rename() {
        val dialog = _uiState.value.renameDialog ?: return
        if (!dialog.canSave) return
        updateRename { it.copy(isSaving = true, errorRes = null) }
        viewModelScope.launch {
            renameDeviceUseCase(deviceId, dialog.value)
                .onSuccess { updated ->
                    _uiState.update { it.copy(device = updated, renameDialog = null) }
                    _uiEffect.emit(TabletSettingsUiEffect.ShowMessage(R.string.tablet_settings_renamed))
                }
                .onFailure { throwable ->
                    when ((throwable as? DeviceSettingsException)?.error) {
                        DeviceSettingsError.NotFound ->
                            _uiState.update { it.copy(renameDialog = null, load = TabletSettingsLoad.NOT_FOUND) }
                        DeviceSettingsError.InvalidInput ->
                            updateRename { it.copy(isSaving = false, errorRes = R.string.tablet_settings_rename_invalid) }
                        else ->
                            updateRename { it.copy(isSaving = false, errorRes = R.string.tablet_settings_rename_network) }
                    }
                }
        }
    }

    private fun updateRename(transform: (RenameDialogState) -> RenameDialogState) {
        _uiState.update { state -> state.renameDialog?.let { state.copy(renameDialog = transform(it)) } ?: state }
    }

    private fun updateDialog(transform: (ExitPinDialogState) -> ExitPinDialogState) {
        _uiState.update { state -> state.pinDialog?.let { state.copy(pinDialog = transform(it)) } ?: state }
    }

    /** Keeps only digits, at most six — the longest PIN the backend accepts. */
    private fun String.asPinInput(): String = filter { it in '0'..'9' }.take(MAX_PIN_LENGTH)

    private companion object {
        const val MAX_PIN_LENGTH = 6
    }
}
