package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.model.DeviceSettings
import com.qhathia.mobile.staff.core.domain.model.DeviceSettingsError
import com.qhathia.mobile.staff.core.domain.model.DeviceSettingsException
import com.qhathia.mobile.staff.core.domain.repository.DeviceRepository
import javax.inject.Inject

/** Reads a tablet's remote settings; every failure is a [DeviceSettingsException]. */
class GetDeviceSettingsUseCase @Inject constructor(
    private val repository: DeviceRepository,
) {
    suspend operator fun invoke(deviceId: String): Result<DeviceSettings> =
        repository.getSettings(deviceId).recoverCatching { throw it.asSettingsException() }
}

internal fun Throwable.asSettingsException(): DeviceSettingsException =
    this as? DeviceSettingsException ?: DeviceSettingsException(DeviceSettingsError.Unknown(message))
