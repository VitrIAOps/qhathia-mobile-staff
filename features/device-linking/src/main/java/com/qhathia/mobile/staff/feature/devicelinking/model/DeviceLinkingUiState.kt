package com.qhathia.mobile.staff.feature.devicelinking.model

import androidx.annotation.StringRes
import com.qhathia.mobile.staff.core.domain.model.Device
import com.qhathia.mobile.staff.core.domain.model.DeviceCommand
import com.qhathia.mobile.staff.core.domain.model.DeviceCommandType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.time.Instant

/**
 * Immutable snapshot of Dispositivos. [devices] is the store's paired tablets and [catalogCount] the
 * store-wide active catalog (`null` when unknown). [commands] keeps, per device, the latest command
 * of each type; [sendingCommands] are requests not yet answered. The pairing dialog owns its own
 * transient sub-state ([isPairDialogVisible], [pairCode], [isPairing], [pairErrorRes]).
 */
data class DeviceLinkingUiState(
    val devices: ImmutableList<Device> = persistentListOf(),
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val catalogCount: Int? = null,
    val commands: Map<String, Map<DeviceCommandType, DeviceCommand>> = emptyMap(),
    val sendingCommands: Set<CommandKey> = emptySet(),
    val pendingRestartDeviceId: String? = null,
    val now: Instant = Instant.EPOCH,
    val isPairDialogVisible: Boolean = false,
    val pairCode: String = "",
    val isPairing: Boolean = false,
    @param:StringRes val pairErrorRes: Int? = null,
) {
    /** True once the list has loaded at least one device. */
    val hasDevices: Boolean get() = devices.isNotEmpty()

    /** The submit button is enabled only with a non-blank code and no pairing in flight. */
    val canSubmitPair: Boolean get() = pairCode.isNotBlank() && !isPairing

    fun commandFor(deviceId: String, type: DeviceCommandType): DeviceCommand? = commands[deviceId]?.get(type)

    /** A command button is disabled while its request or the tablet's confirmation is pending. */
    fun isCommandBusy(deviceId: String, type: DeviceCommandType): Boolean =
        CommandKey(deviceId, type) in sendingCommands || commandFor(deviceId, type)?.isInFlight == true

    /** Whether any shown command still waits for the tablet — the condition to keep polling. */
    val hasCommandsInFlight: Boolean
        get() = commands.values.any { byType -> byType.values.any { it.isInFlight } }
}

data class CommandKey(val deviceId: String, val type: DeviceCommandType)
