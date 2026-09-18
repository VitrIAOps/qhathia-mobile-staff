package com.qhathia.mobile.staff.core.testing

import com.qhathia.mobile.staff.core.domain.model.Device
import com.qhathia.mobile.staff.core.domain.model.DeviceCommand
import com.qhathia.mobile.staff.core.domain.model.DeviceCommandStatus
import com.qhathia.mobile.staff.core.domain.model.DeviceCommandType
import com.qhathia.mobile.staff.core.domain.repository.DeviceRepository

/**
 * Controllable [DeviceRepository] for tests. Set the `*Result` fields to the value the next call
 * should return; the last paired code, the sent commands and call counts are exposed for assertions.
 */
class FakeDeviceRepository : DeviceRepository {

    var devicesResult: Result<List<Device>> = Result.success(listOf(TestFixtures.device()))
    var pairResult: Result<Unit> = Result.success(Unit)
    var catalogCountResult: Result<Int> = Result.success(12)

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
}
