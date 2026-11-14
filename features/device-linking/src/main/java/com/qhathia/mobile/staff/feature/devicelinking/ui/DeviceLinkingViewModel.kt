package com.qhathia.mobile.staff.feature.devicelinking.ui

import com.qhathia.mobile.staff.core.domain.model.DeviceCommandStatus
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.coroutineScope
import java.time.Instant
import com.qhathia.mobile.staff.feature.devicelinking.model.suggestTabletName
import com.qhathia.mobile.staff.feature.devicelinking.model.normalizePairingCode
import com.qhathia.mobile.staff.feature.devicelinking.model.PairingPhase
import com.qhathia.mobile.staff.feature.devicelinking.model.MAX_TABLET_NAME_LENGTH
import com.qhathia.mobile.staff.core.domain.usecase.UnlinkDeviceUseCase
import com.qhathia.mobile.staff.core.domain.usecase.RejectPairingUseCase
import com.qhathia.mobile.staff.core.domain.usecase.GetPairingStatusUseCase
import com.qhathia.mobile.staff.core.domain.model.PairedDevice
import com.qhathia.mobile.staff.core.domain.model.DeviceSettingsException
import com.qhathia.mobile.staff.core.domain.model.DeviceSettingsError
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
    private val rejectPairingUseCase: RejectPairingUseCase,
    private val getPairingStatusUseCase: GetPairingStatusUseCase,
    private val unlinkDeviceUseCase: UnlinkDeviceUseCase,
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

    /** Polls the pairing session while the dialog waits for the tablet. */
    private var pairWaitJob: Job? = null

    /** The first start coincides with the initial load; later ones (back from Ajustes) refresh. */
    private var hasStarted = false

    /** When the device list was last read, so a start right after a load does not repeat it. */
    private var lastDevicesLoadAt: Instant? = null

    init {
        onEvent(DeviceLinkingUiEvent.Load)
    }

    fun onEvent(event: DeviceLinkingUiEvent) {
        when (event) {
            DeviceLinkingUiEvent.Load,
            DeviceLinkingUiEvent.Retry -> loadDevices()
            DeviceLinkingUiEvent.ScreenStarted -> {
                isScreenStarted = true
                // Back from Ajustes or from the background: what is shown may be stale.
                val lastLoad = lastDevicesLoadAt
                if (hasStarted && (lastLoad == null || !lastLoad.plus(START_REFRESH_MIN_GAP).isAfter(clock.instant()))) {
                    // A product created or edited meanwhile changes the catalog count too.
                    viewModelScope.launch { refreshSilently(includeCatalog = true) }
                }
                hasStarted = true
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
                _uiEffect.tryEmit(DeviceLinkingUiEffect.ShowUnlinked(event.alreadyGone))
                viewModelScope.launch { refreshSilently() }
            }
            DeviceLinkingUiEvent.OpenPairDialog ->
                _uiState.update {
                    it.copy(
                        isPairDialogVisible = true,
                        pairCode = "",
                        pairName = suggestTabletName(it.devices),
                        pairErrorRes = null,
                        pairingPhase = PairingPhase.EnteringCode,
                    )
                }
            // Once approved, the dialog only leaves through the tablet's answer, undo or a wait. Back while
            // waiting stops the wait and shows the options, never a stuck screen (F-60).
            DeviceLinkingUiEvent.DismissPairDialog -> when (val phase = _uiState.value.pairingPhase) {
                PairingPhase.EnteringCode -> closePairDialog()
                is PairingPhase.WaitingForTablet -> {
                    pairWaitJob?.cancel()
                    _uiState.update { it.copy(pairingPhase = PairingPhase.TabletDidNotRespond(phase.paired)) }
                }
                is PairingPhase.TabletDidNotRespond -> Unit
            }
            is DeviceLinkingUiEvent.PairCodeChanged ->
                _uiState.update { it.copy(pairCode = normalizePairingCode(event.code), pairErrorRes = null) }
            is DeviceLinkingUiEvent.PairNameChanged ->
                _uiState.update { it.copy(pairName = event.name.take(MAX_TABLET_NAME_LENGTH)) }
            DeviceLinkingUiEvent.SubmitPair -> submitPair()
            DeviceLinkingUiEvent.RejectPair -> rejectPair()
            DeviceLinkingUiEvent.KeepWaiting ->
                (_uiState.value.pairingPhase as? PairingPhase.TabletDidNotRespond)?.let { waitForTablet(it.paired) }
            DeviceLinkingUiEvent.UndoPair -> undoPair()
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
                    lastDevicesLoadAt = clock.instant()
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

    /**
     * One refresh loop while the screen is visible. It ticks every [COMMAND_POLL_INTERVAL] and
     * refreshes on every tick while something waits for a tablet, otherwise every other tick
     * ([DEVICES_REFRESH_INTERVAL]). Deciding on each tick means a command sent mid-wait is followed
     * at the faster pace at once. Stopped with the screen.
     */
    private fun ensurePolling() {
        if (!isScreenStarted || pollJob?.isActive == true) return
        pollJob = viewModelScope.launch {
            var ticks = 0
            while (isActive) {
                delay(COMMAND_POLL_INTERVAL.toMillis())
                ticks++
                if (_uiState.value.needsFastPolling || ticks % TICKS_PER_DEVICES_REFRESH == 0) refreshSilently()
            }
        }
    }

    /**
     * Re-reads the devices, then the commands of the tablets that need it, without a spinner; a
     * failed read keeps what is shown. Each tablet's commands are replaced, never merged, so a
     * command the server finished cannot linger as "Enviado".
     *
     * The catalog count is re-read with [includeCatalog] (coming back to the screen) and once when
     * an "Actualizar catálogo" turns ACKED; a plain poll tick leaves it, since only product changes
     * move it. A failed count read keeps the number shown.
     */
    private suspend fun refreshSilently(includeCatalog: Boolean = false) {
        coroutineScope {
            val catalog = if (includeCatalog) async { getCatalogCountUseCase() } else null
            getDevicesUseCase().onSuccess { devices ->
                lastDevicesLoadAt = clock.instant()
                _uiState.update { it.copy(devices = devices.toImmutableList(), now = clock.instant()) }
            }
            catalog?.await()?.onSuccess { count -> _uiState.update { it.copy(catalogCount = count) } }
        }
        val ids = _uiState.value.deviceIdsNeedingCommandPoll
        val results = coroutineScope { ids.map { id -> async { id to getDeviceCommandsUseCase(id) } }.awaitAll() }
        val now = clock.instant()
        val before = _uiState.value.commands
        _uiState.update { state ->
            val replaced = state.commands.toMutableMap()
            results.forEach { (deviceId, result) ->
                result.onSuccess { commands -> replaced[deviceId] = commands.latestPerType(now) }
            }
            state.copy(commands = replaced, now = now)
        }
        if (!includeCatalog && catalogRefreshJustAcked(before, _uiState.value.commands)) {
            getCatalogCountUseCase().onSuccess { count -> _uiState.update { it.copy(catalogCount = count) } }
        }
    }

    /** Whether some tablet's "Actualizar catálogo" was not ACKED in [before] and is in [after]. */
    private fun catalogRefreshJustAcked(
        before: Map<String, Map<DeviceCommandType, DeviceCommand>>,
        after: Map<String, Map<DeviceCommandType, DeviceCommand>>,
    ): Boolean = after.any { (deviceId, byType) ->
        byType[DeviceCommandType.REFRESH_CATALOG]?.status == DeviceCommandStatus.ACKED &&
            before[deviceId]?.get(DeviceCommandType.REFRESH_CATALOG)?.status != DeviceCommandStatus.ACKED
    }

    private fun submitPair() {
        val state = _uiState.value
        if (!state.canSubmitPair) return
        viewModelScope.launch {
            _uiState.update { it.copy(isPairing = true, pairErrorRes = null) }
            pairDeviceUseCase(state.pairCode, state.pairName.trim().ifBlank { null })
                .onSuccess { paired -> waitForTablet(paired) }
                .onFailure { throwable ->
                    _uiState.update { it.copy(isPairing = false, pairErrorRes = throwable.toPairErrorRes()) }
                }
        }
    }

    /**
     * The approval only means "approved": the tablet still has to exchange the code. Polls the
     * session until it does — then the pairing is done — or until the wait runs out.
     */
    private fun waitForTablet(paired: PairedDevice) {
        pairWaitJob?.cancel()
        _uiState.update {
            it.copy(isPairing = false, pairErrorRes = null, pairingPhase = PairingPhase.WaitingForTablet(paired))
        }
        pairWaitJob = viewModelScope.launch {
            // A wall-clock bound (F-60): each read takes time too (~0.5 s on prod, up to the HTTP timeout
            // on a bad network), so counting reads overshoots the wait the backend suggests. The timeout
            // also cancels a read in flight; runCatching in the service swallows that, but the next delay
            // rethrows it, so nothing between the read and the delay may assume the coroutine is alive.
            val exchanged = withTimeoutOrNull(PAIR_EXCHANGE_TIMEOUT.toMillis()) {
                while (getPairingStatusUseCase(paired.sessionId).getOrNull()?.tokenExchanged != true) {
                    delay(PAIR_STATUS_POLL.toMillis())
                }
            } != null
            if (!exchanged) {
                _uiState.update { it.copy(pairingPhase = PairingPhase.TabletDidNotRespond(paired)) }
                return@launch
            }
            // This very job runs the follow-up reads, so it must not cancel itself.
            closePairDialog(cancelWait = false)
            _uiEffect.tryEmit(DeviceLinkingUiEffect.ShowPaired(paired.name))
            loadDevices()
            // The tablet's first heartbeat goes out with the exchange; read again so the new card
            // already shows it.
            delay(FIRST_HEARTBEAT_GRACE.toMillis())
            loadDevices()
        }
    }

    /** Unlinks the device an unanswered approval created; one already gone counts as undone. */
    private fun undoPair() {
        val state = _uiState.value
        val paired = (state.pairingPhase as? PairingPhase.TabletDidNotRespond)?.paired ?: return
        if (state.isUndoing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isUndoing = true, pairErrorRes = null) }
            val result = unlinkDeviceUseCase(paired.deviceId)
            val gone = (result.exceptionOrNull() as? DeviceSettingsException)?.error == DeviceSettingsError.NotFound
            if (result.isSuccess || gone) {
                closePairDialog()
                _uiEffect.tryEmit(DeviceLinkingUiEffect.ShowPairUndone)
                loadDevices()
            } else {
                _uiState.update { it.copy(isUndoing = false, pairErrorRes = R.string.device_pair_error_network) }
            }
        }
    }

    private fun rejectPair() {
        val state = _uiState.value
        if (!state.canSubmitPair) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRejecting = true, pairErrorRes = null) }
            rejectPairingUseCase(state.pairCode)
                .onSuccess {
                    closePairDialog()
                    _uiEffect.tryEmit(DeviceLinkingUiEffect.ShowPairRejected)
                }
                .onFailure { throwable ->
                    _uiState.update { it.copy(isRejecting = false, pairErrorRes = throwable.toPairErrorRes()) }
                }
        }
    }

    private fun closePairDialog(cancelWait: Boolean = true) {
        if (cancelWait) pairWaitJob?.cancel()
        _uiState.update {
            it.copy(
                isPairDialogVisible = false,
                isPairing = false,
                isRejecting = false,
                isUndoing = false,
                pairCode = "",
                pairName = "",
                pairErrorRes = null,
                pairingPhase = PairingPhase.EnteringCode,
            )
        }
    }

    private fun Throwable.toPairErrorRes(): Int =
        when ((this as? PairDeviceException)?.error) {
            PairDeviceError.InvalidCode -> R.string.device_pair_error_invalid_code
            PairDeviceError.ExpiredCode -> R.string.device_pair_error_expired
            PairDeviceError.AlreadyUsedCode -> R.string.device_pair_error_used
            PairDeviceError.CancelledCode -> R.string.device_pair_error_cancelled
            is PairDeviceError.CatalogNotReady -> R.string.device_pair_error_catalog
            PairDeviceError.CatalogCheckUnavailable -> R.string.device_pair_error_catalog_unavailable
            PairDeviceError.Network -> R.string.device_pair_error_network
            is PairDeviceError.Unknown, null -> R.string.device_pair_error_generic
        }

    companion object {
        val COMMAND_POLL_INTERVAL: Duration = Duration.ofSeconds(15)

        /** The tablet beats every 60 s; reading at 30 s shows a beat at most ~30 s late. */
        val DEVICES_REFRESH_INTERVAL: Duration = Duration.ofSeconds(30)

        /** [DEVICES_REFRESH_INTERVAL] in loop ticks of [COMMAND_POLL_INTERVAL]. */
        private val TICKS_PER_DEVICES_REFRESH = (DEVICES_REFRESH_INTERVAL.seconds / COMMAND_POLL_INTERVAL.seconds).toInt()

        /** A start this soon after a load does not read again. */
        val START_REFRESH_MIN_GAP: Duration = Duration.ofSeconds(5)

        /** How often the pairing session is read while waiting for the tablet. */
        val PAIR_STATUS_POLL: Duration = Duration.ofSeconds(2)

        /**
         * Total wait for the exchange, reads included. Leaves room for the POST /pair before it, so the
         * "did not respond" notice lands at most 30 s after "Vincular" (api-contract §37, F-60).
         */
        val PAIR_EXCHANGE_TIMEOUT: Duration = Duration.ofSeconds(25)

        /** The tablet's first heartbeat goes out with the exchange; it is read again after this. */
        val FIRST_HEARTBEAT_GRACE: Duration = Duration.ofSeconds(3)
        val FINISHED_COMMAND_TTL: Duration = Duration.ofMinutes(10)
    }
}

/**
 * The newest command of each type (the backend lists newest first). A finished command stays for
 * [DeviceLinkingViewModel.FINISHED_COMMAND_TTL] counted from when it finished — an expired command
 * finishes 10 min after it was requested, so counting from the request would hide it at once. An
 * in-flight one is always kept.
 */
private fun List<DeviceCommand>.latestPerType(now: Instant): Map<DeviceCommandType, DeviceCommand> =
    sortedByDescending { it.requestedAt }
        .distinctBy { it.type }
        .filter { command ->
            command.isInFlight ||
                (command.finishedAt ?: command.requestedAt)
                    ?.isAfter(now.minus(DeviceLinkingViewModel.FINISHED_COMMAND_TTL)) == true
        }
        .associateBy { it.type }
