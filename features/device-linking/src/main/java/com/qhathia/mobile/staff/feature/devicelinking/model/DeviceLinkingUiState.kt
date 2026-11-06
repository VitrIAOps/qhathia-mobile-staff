package com.qhathia.mobile.staff.feature.devicelinking.model

import java.time.Duration
import com.qhathia.mobile.staff.core.domain.model.DeviceCommandStatus
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

    /**
     * Where a restart of [device] stands. An ack only says the tablet got the order: it is back once
     * it reports again after the ack, and "not back" if [RESTART_BACK_TIMEOUT] passes without that.
     * Null when there is no restart to follow (none, failed, expired or overdue).
     */
    fun restartPhase(device: Device): RestartPhase? {
        val command = commandFor(device.id, DeviceCommandType.RESTART_APP) ?: return null
        if (command.isOverdue(now)) return null
        return when (command.status) {
            DeviceCommandStatus.PENDING -> RestartPhase.SENT
            DeviceCommandStatus.DELIVERED -> RestartPhase.RESTARTING
            DeviceCommandStatus.ACKED -> {
                // An older backend without ackedAt: trust the ack, as before.
                val acked = command.ackedAt ?: return RestartPhase.BACK
                val ping = device.lastPingAt
                when {
                    ping != null && ping.isAfter(acked) -> RestartPhase.BACK
                    !now.isBefore(acked.plus(RESTART_BACK_TIMEOUT)) -> RestartPhase.NOT_BACK
                    else -> RestartPhase.RESTARTING
                }
            }
            else -> null
        }
    }

    /** A command still waiting for the tablet; one long past its deadline counts as expired. */
    private fun DeviceCommand.isPending(): Boolean = isInFlight && !isOverdue(now)

    private fun isWaitingForRestart(deviceId: String): Boolean =
        devices.firstOrNull { it.id == deviceId }?.let { restartPhase(it) == RestartPhase.RESTARTING } == true

    /**
     * A command button is disabled while its request or the tablet's confirmation is pending, and
     * Reiniciar also while the restart waits for the tablet to report again.
     */
    fun isCommandBusy(deviceId: String, type: DeviceCommandType): Boolean =
        CommandKey(deviceId, type) in sendingCommands ||
            commandFor(deviceId, type)?.isPending() == true ||
            (type == DeviceCommandType.RESTART_APP && isWaitingForRestart(deviceId))

    /** Whether any shown command still waits for the tablet. */
    val hasCommandsInFlight: Boolean
        get() = commands.values.any { byType -> byType.values.any { it.isPending() } }

    /** Tablets whose commands are re-read on each refresh: something in flight, or a restart waiting. */
    val deviceIdsNeedingCommandPoll: Set<String>
        get() = devices
            .filter { device ->
                commands[device.id]?.values?.any { it.isPending() } == true ||
                    restartPhase(device) == RestartPhase.RESTARTING
            }
            .map { it.id }
            .toSet()

    /** While anything waits for a tablet, the screen refreshes at the faster pace. */
    val needsFastPolling: Boolean
        get() = deviceIdsNeedingCommandPoll.isNotEmpty()
}

/** The restart line's steps. */
enum class RestartPhase { SENT, RESTARTING, BACK, NOT_BACK }

/** Three missed heartbeats after the ack: the tablet is reported as not back. */
val RESTART_BACK_TIMEOUT: Duration = Duration.ofMinutes(3)

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
