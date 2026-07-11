package com.qhathia.mobile.staff.core.testing

import com.qhathia.mobile.staff.core.domain.model.Device
import com.qhathia.mobile.staff.core.domain.repository.DeviceRepository

/**
 * Controllable [DeviceRepository] for tests. Set the `*Result` fields to the value the next call
 * should return; the last paired code and call counts are exposed for assertions.
 */
class FakeDeviceRepository : DeviceRepository {

    var devicesResult: Result<List<Device>> = Result.success(listOf(TestFixtures.device()))
    var pairResult: Result<Unit> = Result.success(Unit)

    var getDevicesCallCount: Int = 0
        private set
    var lastPairedCode: String? = null
        private set
    var pairCallCount: Int = 0
        private set

    override suspend fun getDevices(): Result<List<Device>> {
        getDevicesCallCount++
        return devicesResult
    }

    override suspend fun pairDevice(code: String): Result<Unit> {
        pairCallCount++
        lastPairedCode = code
        return pairResult
    }
}
