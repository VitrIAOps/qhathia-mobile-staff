package com.qhathia.mobile.staff.core.testing

import com.qhathia.mobile.staff.core.domain.model.Device
import com.qhathia.mobile.staff.core.domain.model.DeviceCommand
import com.qhathia.mobile.staff.core.domain.model.DeviceCommandStatus
import com.qhathia.mobile.staff.core.domain.model.DeviceCommandType
import com.qhathia.mobile.staff.core.domain.model.DeviceSettings
import com.qhathia.mobile.staff.core.domain.model.DeviceSettingsChange
import kotlinx.coroutines.CompletableDeferred
import com.qhathia.mobile.staff.core.domain.repository.DeviceRepository

/**
 * Controllable [DeviceRepository] for tests. Set the `*Result` fields to the value the next call
 * should return; the last paired code, the sent commands and call counts are exposed for assertions.
 */
class FakeDeviceRepository : DeviceRepository {

    var devicesResult: Result<List<Device>> = Result.success(listOf(TestFixtures.device()))
    var pairResult: Result<Unit> = Result.success(Unit)
    var catalogCountResult: Result<Int> = Result.success(12)
    var unlinkResult: Result<Unit> = Result.success(Unit)

    /** Every device id sent to [unlinkDevice], in order. */
    val unlinkedDevices: MutableList<String> = mutableListOf()
    var settingsResult: Result<DeviceSettings> = Result.success(TestFixtures.deviceSettings())

    /**
     * Answers [updateSettings]. When null, the change is applied to the current settings (from
     * [settingsResult]) and that is returned, as the backend does.
     */
    var updateSettingsResult: Result<DeviceSettings>? = null

    /** Set to a pending deferred to hold [updateSettings] in flight. */
    var updateSettingsGate: CompletableDeferred<Unit>? = null

    /** Every `(deviceId, change)` sent, in order. */
    val settingsChanges: MutableList<Pair<String, DeviceSettingsChange>> = mutableListOf()

    /** When null, a sent command succeeds as `PENDING` of the requested type. */
    var sendCommandResult: Result<DeviceCommand>? = null

    /** Per-device answers of [getCommands]; a device without an entry has no commands. */
    val commandsResults: MutableMap<String, Result<List<DeviceCommand>>> = mutableMapOf()

    var getDevicesCallCount: Int = 0
        private set
    var lastPairedCode: String? = null
        private set
    var pairCallCount: Int = 0
        private set
    var getCommandsCallCount: Int = 0
        private set

    /** Every `(deviceId, type)` sent, in order. */
    val sentCommands: MutableList<Pair<String, DeviceCommandType>> = mutableListOf()

    override suspend fun getDevices(): Result<List<Device>> {
        getDevicesCallCount++
        return devicesResult
    }

    override suspend fun pairDevice(code: String): Result<Unit> {
        pairCallCount++
        lastPairedCode = code
        return pairResult
    }

    override suspend fun unlinkDevice(deviceId: String): Result<Unit> {
        unlinkedDevices += deviceId
        return unlinkResult
    }

    override suspend fun getCatalogCount(): Result<Int> = catalogCountResult

    override suspend fun sendCommand(
        deviceId: String,
        type: DeviceCommandType,
    ): Result<DeviceCommand> {
        sentCommands += deviceId to type
        return sendCommandResult ?: Result.success(
            TestFixtures.deviceCommand(
                id = "cmd-${sentCommands.size}",
                type = type,
                status = DeviceCommandStatus.PENDING,
            ),
        )
    }

    override suspend fun getCommands(deviceId: String): Result<List<DeviceCommand>> {
        getCommandsCallCount++
        return commandsResults[deviceId] ?: Result.success(emptyList())
    }

    override suspend fun getSettings(deviceId: String): Result<DeviceSettings> = settingsResult

    override suspend fun updateSettings(deviceId: String, change: DeviceSettingsChange): Result<DeviceSettings> {
        settingsChanges += deviceId to change
        updateSettingsGate?.await()
        updateSettingsResult?.let { return it }
        val current = settingsResult.getOrNull() ?: TestFixtures.deviceSettings()
        val applied = when (change) {
            is DeviceSettingsChange.IdleShowcase -> current.copy(idleShowcaseEnabled = change.enabled)
            is DeviceSettingsChange.VoiceAssistant -> current.copy(voiceAssistantEnabled = change.enabled)
            is DeviceSettingsChange.Brightness -> current.copy(brightnessMode = change.mode)
            is DeviceSettingsChange.KioskMode -> current.copy(kioskMode = change.enabled)
            is DeviceSettingsChange.AutoSync -> current.copy(autoSyncEnabled = change.enabled)
            is DeviceSettingsChange.SetExitPin -> current.copy(exitPinSet = true)
            DeviceSettingsChange.RemoveExitPin -> current.copy(exitPinSet = false)
        }
        settingsResult = Result.success(applied)
        return Result.success(applied)
    }
}
