package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.model.DeviceSettings
import com.qhathia.mobile.staff.core.domain.model.DeviceSettingsChange
import com.qhathia.mobile.staff.core.domain.model.DeviceSettingsError
import com.qhathia.mobile.staff.core.domain.model.DeviceSettingsException
import com.qhathia.mobile.staff.core.domain.model.isValidExitPin
import com.qhathia.mobile.staff.core.domain.repository.DeviceRepository
import javax.inject.Inject

/**
 * Applies one change to a tablet's settings. A PIN that is not 4 to 6 digits is rejected here, so it
 * never costs a request; every failure is a [DeviceSettingsException].
 */
class UpdateDeviceSettingsUseCase @Inject constructor(
    private val repository: DeviceRepository,
) {
    suspend operator fun invoke(deviceId: String, change: DeviceSettingsChange): Result<DeviceSettings> {
        if (change is DeviceSettingsChange.SetExitPin && !isValidExitPin(change.pin)) {
            return Result.failure(DeviceSettingsException(DeviceSettingsError.InvalidInput))
        }
        return repository.updateSettings(deviceId, change).recoverCatching { throw it.asSettingsException() }
    }
}
