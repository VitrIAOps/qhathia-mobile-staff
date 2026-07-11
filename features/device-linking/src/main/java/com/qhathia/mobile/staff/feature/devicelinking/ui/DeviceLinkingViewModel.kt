package com.qhathia.mobile.staff.feature.devicelinking.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qhathia.mobile.staff.core.domain.model.PairDeviceError
import com.qhathia.mobile.staff.core.domain.model.PairDeviceException
import com.qhathia.mobile.staff.core.domain.usecase.GetDevicesUseCase
import com.qhathia.mobile.staff.core.domain.usecase.PairDeviceUseCase
import com.qhathia.mobile.staff.feature.devicelinking.R
import com.qhathia.mobile.staff.feature.devicelinking.model.DeviceLinkingUiEvent
import com.qhathia.mobile.staff.feature.devicelinking.model.DeviceLinkingUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Drives the linked-devices screen: loads the store's paired terminals on init and after a
 * successful pairing. Pairing errors are surfaced inline in the dialog via a string resource; the
 * only differentiated case is an invalid/expired code, everything else falls back to a generic
 * message (POC scope).
 */
@HiltViewModel
class DeviceLinkingViewModel @Inject constructor(
    private val getDevicesUseCase: GetDevicesUseCase,
    private val pairDeviceUseCase: PairDeviceUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceLinkingUiState())
    val uiState: StateFlow<DeviceLinkingUiState> = _uiState.asStateFlow()

    init {
        onEvent(DeviceLinkingUiEvent.Load)
    }

    fun onEvent(event: DeviceLinkingUiEvent) {
        when (event) {
            DeviceLinkingUiEvent.Load,
            DeviceLinkingUiEvent.Retry -> loadDevices()
            DeviceLinkingUiEvent.OpenPairDialog ->
                _uiState.update {
                    it.copy(isPairDialogVisible = true, pairCode = "", pairErrorRes = null)
                }
            DeviceLinkingUiEvent.DismissPairDialog ->
                _uiState.update {
                    it.copy(
                        isPairDialogVisible = false,
                        isPairing = false,
                        pairCode = "",
                        pairErrorRes = null,
                    )
                }
            is DeviceLinkingUiEvent.PairCodeChanged ->
                _uiState.update { it.copy(pairCode = event.code, pairErrorRes = null) }
            DeviceLinkingUiEvent.SubmitPair -> submitPair()
        }
    }

    private fun loadDevices() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isError = false) }
            getDevicesUseCase()
                .onSuccess { devices ->
                    _uiState.update {
                        it.copy(
                            devices = devices.toImmutableList(),
                            isLoading = false,
                            isError = false,
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoading = false, isError = true) }
                }
        }
    }

    private fun submitPair() {
        val code = _uiState.value.pairCode
        if (code.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isPairing = true, pairErrorRes = null) }
            pairDeviceUseCase(code)
                .onSuccess {
                    _uiState.update {
                        it.copy(isPairDialogVisible = false, isPairing = false, pairCode = "")
                    }
                    loadDevices()
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(isPairing = false, pairErrorRes = throwable.toPairErrorRes())
                    }
                }
        }
    }

    private fun Throwable.toPairErrorRes(): Int =
        when ((this as? PairDeviceException)?.error) {
            PairDeviceError.InvalidCode -> R.string.device_pair_error_invalid_code
            else -> R.string.device_pair_error_generic
        }
}
