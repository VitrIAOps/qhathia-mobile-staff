package com.qhathia.mobile.staff.feature.devicelinking.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qhathia.mobile.staff.core.domain.model.DeviceCommand
import com.qhathia.mobile.staff.core.domain.model.DeviceCommandType
import com.qhathia.mobile.staff.core.domain.model.PairDeviceError
import com.qhathia.mobile.staff.core.domain.model.PairDeviceException
import com.qhathia.mobile.staff.core.domain.usecase.GetCatalogCountUseCase
import com.qhathia.mobile.staff.core.domain.usecase.GetDeviceCommandsUseCase
import com.qhathia.mobile.staff.core.domain.usecase.GetDevicesUseCase
import com.qhathia.mobile.staff.core.domain.usecase.PairDeviceUseCase
import com.qhathia.mobile.staff.core.domain.usecase.SendDeviceCommandUseCase
import com.qhathia.mobile.staff.feature.devicelinking.R
import com.qhathia.mobile.staff.feature.devicelinking.model.CommandKey
import com.qhathia.mobile.staff.feature.devicelinking.model.DeviceLinkingUiEffect
import com.qhathia.mobile.staff.feature.devicelinking.model.DeviceLinkingUiEvent
import com.qhathia.mobile.staff.feature.devicelinking.model.DeviceLinkingUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import javax.inject.Inject

/**
 * Drives Dispositivos: the store's tablets with their last reported telemetry, the store's catalog
 * size, pairing a new tablet, and the two remote commands.
 *
 * Commands travel on the tablet's heartbeat, so the screen says "Enviado" until the tablet confirms.
 * While a command is in flight and the screen is visible, each device's commands are polled every
 * [COMMAND_POLL_INTERVAL]; polling stops as soon as nothing is in flight or the screen stops. A
 * finished command older than [FINISHED_COMMAND_TTL] is no longer news and is not shown.
 *
 * Pairing errors are surfaced inline in the dialog via a string resource; the only differentiated
 * case is an invalid/expired code, everything else falls back to a generic message.
 */
@HiltViewModel
class DeviceLinkingViewModel @Inject constructor(
    private val getDevicesUseCase: GetDevicesUseCase,
    private val pairDeviceUseCase: PairDeviceUseCase,
    private val getCatalogCountUseCase: GetCatalogCountUseCase,
    private val sendDeviceCommandUseCase: SendDeviceCommandUseCase,
    private val getDeviceCommandsUseCase: GetDeviceCommandsUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceLinkingUiState(now = clock.instant()))
    val uiState: StateFlow<DeviceLinkingUiState> = _uiState.asStateFlow()

    private val _uiEffect = MutableSharedFlow<DeviceLinkingUiEffect>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val uiEffect: SharedFlow<DeviceLinkingUiEffect> = _uiEffect.asSharedFlow()

    private var isScreenStarted = false
    private var pollJob: Job? = null

    init {
        onEvent(DeviceLinkingUiEvent.Load)
    }

    fun onEvent(event: DeviceLinkingUiEvent) {
        when (event) {
            DeviceLinkingUiEvent.Load,
            DeviceLinkingUiEvent.Retry -> loadDevices()
            DeviceLinkingUiEvent.ScreenStarted -> {
                isScreenStarted = true
                ensurePolling()
            }
            DeviceLinkingUiEvent.ScreenStopped -> {
                isScreenStarted = false
                pollJob?.cancel()
            }
            is DeviceLinkingUiEvent.RefreshCatalog -> sendCommand(event.deviceId, DeviceCommandType.REFRESH_CATALOG)
            is DeviceLinkingUiEvent.RestartRequested ->
                if (!_uiState.value.isCommandBusy(event.deviceId, DeviceCommandType.RESTART_APP)) {
                    _uiState.update { it.copy(pendingRestartDeviceId = event.deviceId) }
                }
            DeviceLinkingUiEvent.RestartDismissed -> _uiState.update { it.copy(pendingRestartDeviceId = null) }
            DeviceLinkingUiEvent.RestartConfirmed -> {
                val deviceId = _uiState.value.pendingRestartDeviceId ?: return
                _uiState.update { it.copy(pendingRestartDeviceId = null) }
                sendCommand(deviceId, DeviceCommandType.RESTART_APP)
            }
            is DeviceLinkingUiEvent.DeviceUnlinked -> {
                _uiState.update { state ->
                    state.copy(
                        devices = state.devices.filterNot { it.id == event.deviceId }.toImmutableList(),
                        commands = state.commands - event.deviceId,
                        sendingCommands = state.sendingCommands.filterNot { it.deviceId == event.deviceId }.toSet(),
                    )
                }
                _uiEffect.tryEmit(DeviceLinkingUiEffect.ShowUnlinked)
            }
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
            _uiState.update { it.copy(isLoading = true, isError = false, now = clock.instant()) }
            val count = async { getCatalogCountUseCase() }
            val devicesResult = getDevicesUseCase()
            _uiState.update { it.copy(catalogCount = count.await().getOrNull() ?: it.catalogCount) }
            devicesResult
                .onSuccess { devices ->
                    _uiState.update {
                        it.copy(
                            devices = devices.toImmutableList(),
                            isLoading = false,
                            isError = false,
                        )
                    }
                    loadCommands(devices.map { it.id })
                }
                .onFailure {
                    _uiState.update { it.copy(isLoading = false, isError = true) }
                }
        }
    }

    private suspend fun loadCommands(deviceIds: List<String>) {
        val results = viewModelScope.run {
            deviceIds.map { id -> async { id to getDeviceCommandsUseCase(id) } }.awaitAll()
        }
        val now = clock.instant()
        _uiState.update { state ->
            val merged = state.commands.toMutableMap()
            results.forEach { (deviceId, result) ->
                result.onSuccess { commands -> merged[deviceId] = commands.latestPerType(now) }
            }
            state.copy(commands = merged, now = now)
        }
        ensurePolling()
    }

    private fun sendCommand(deviceId: String, type: DeviceCommandType) {
        if (_uiState.value.isCommandBusy(deviceId, type)) return
        val key = CommandKey(deviceId, type)
        _uiState.update { it.copy(sendingCommands = it.sendingCommands + key) }
        viewModelScope.launch {
            sendDeviceCommandUseCase(deviceId, type)
                .onSuccess { command ->
                    _uiState.update { state ->
                        val byType = state.commands[deviceId].orEmpty() + (type to command)
                        state.copy(
                            commands = state.commands + (deviceId to byType),
                            sendingCommands = state.sendingCommands - key,
                            now = clock.instant(),
                        )
                    }
                    ensurePolling()
                }
                .onFailure {
                    _uiState.update { it.copy(sendingCommands = it.sendingCommands - key) }
                    _uiEffect.tryEmit(DeviceLinkingUiEffect.ShowCommandError(type))
                }
        }
    }

    private fun ensurePolling() {
        if (!isScreenStarted || pollJob?.isActive == true || !_uiState.value.hasCommandsInFlight) return
        pollJob = viewModelScope.launch {
            while (isActive && _uiState.value.hasCommandsInFlight) {
                delay(COMMAND_POLL_INTERVAL.toMillis())
                val inFlightDevices = _uiState.value.commands
                    .filterValues { byType -> byType.values.any { it.isInFlight } }
                    .keys
                val results = inFlightDevices.map { id -> async { id to getDeviceCommandsUseCase(id) } }.awaitAll()
                val now = clock.instant()
                _uiState.update { state ->
                    val merged = state.commands.toMutableMap()
                    results.forEach { (deviceId, result) ->
                        result.onSuccess { commands ->
                            merged[deviceId] = merged[deviceId].orEmpty() + commands.latestPerType(now)
                        }
                    }
                    state.copy(commands = merged, now = now)
                }
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

    companion object {
        val COMMAND_POLL_INTERVAL: Duration = Duration.ofSeconds(15)
        val FINISHED_COMMAND_TTL: Duration = Duration.ofMinutes(10)
    }
}

/**
 * The newest command of each type (the backend lists newest first). A finished command older than
 * [DeviceLinkingViewModel.FINISHED_COMMAND_TTL] is dropped; an in-flight one is always kept.
 */
private fun List<DeviceCommand>.latestPerType(now: java.time.Instant): Map<DeviceCommandType, DeviceCommand> =
    sortedByDescending { it.requestedAt }
        .distinctBy { it.type }
        .filter { command ->
            command.isInFlight ||
                command.requestedAt?.isAfter(now.minus(DeviceLinkingViewModel.FINISHED_COMMAND_TTL)) == true
        }
        .associateBy { it.type }
