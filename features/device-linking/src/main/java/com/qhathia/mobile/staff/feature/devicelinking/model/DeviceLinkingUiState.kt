package com.qhathia.mobile.staff.feature.devicelinking.model

import java.util.Locale
import com.qhathia.mobile.staff.core.domain.model.PairedDevice
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
 * of each type; [sendingCommands] are requests not yet answered.
 *
 * The pairing dialog owns its own transient sub-state: [pairCode] (always normalized to the pairing
 * alphabet), [pairName], the request flags, [pairErrorRes] and [pairingPhase] — entering the code,
 * waiting for the tablet to exchange it, or the tablet not answering.
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
    val pairName: String = "",
    val isPairing: Boolean = false,
    val isRejecting: Boolean = false,
    val isUndoing: Boolean = false,
    val pairingPhase: PairingPhase = PairingPhase.EnteringCode,
    @param:StringRes val pairErrorRes: Int? = null,
) {
    /** True once the list has loaded at least one device. */
    val hasDevices: Boolean get() = devices.isNotEmpty()

    /** Vincular and Rechazar need a full code, the code step, and no request in flight. */
    val canSubmitPair: Boolean
        get() = pairCode.length == PAIRING_CODE_LENGTH &&
            pairingPhase == PairingPhase.EnteringCode &&
            !isPairing &&
            !isRejecting

    fun commandFor(deviceId: String, type: DeviceCommandType): DeviceCommand? = commands[deviceId]?.get(type)

    /** A command button is disabled while its request or the tablet's confirmation is pending. */
    fun isCommandBusy(deviceId: String, type: DeviceCommandType): Boolean =
        CommandKey(deviceId, type) in sendingCommands || commandFor(deviceId, type)?.isInFlight == true

    /** Whether any shown command still waits for the tablet — the condition to keep polling. */
    val hasCommandsInFlight: Boolean
        get() = commands.values.any { byType -> byType.values.any { it.isInFlight } }
}

data class CommandKey(val deviceId: String, val type: DeviceCommandType)

/** The step the pairing dialog is on. Only [EnteringCode] can be dismissed. */
sealed interface PairingPhase {
    data object EnteringCode : PairingPhase

    /** Approved; polling until the tablet exchanges the code. */
    data class WaitingForTablet(val paired: PairedDevice) : PairingPhase

    /** The wait ran out; the approval can be undone or waited on again. */
    data class TabletDidNotRespond(val paired: PairedDevice) : PairingPhase
}

/** Length of a pairing code, as the backend generates it. */
const val PAIRING_CODE_LENGTH = 5

/** The backend's pairing alphabet: no O, 0, I, 1 or L, which read alike. */
const val PAIRING_ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ"

/** Longest tablet name the dialogs accept (the backend allows more; cards would cut it). */
const val MAX_TABLET_NAME_LENGTH = 40

/** Uppercases, keeps only the pairing alphabet and stops at the code length. */
fun normalizePairingCode(input: String): String =
    input.uppercase(Locale.ROOT).filter { it in PAIRING_ALPHABET }.take(PAIRING_CODE_LENGTH)

/** The first "Tablet N" that no tablet of the store uses, so the backend never repeats a name. */
fun suggestTabletName(devices: List<Device>): String {
    val used = devices.mapNotNull { it.name?.trim()?.lowercase(Locale.ROOT) }.toSet()
    return generateSequence(1) { it + 1 }.map { "Tablet $it" }.first { it.lowercase(Locale.ROOT) !in used }
}
