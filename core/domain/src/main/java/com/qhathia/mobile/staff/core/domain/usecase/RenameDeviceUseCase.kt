package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.model.Device
import com.qhathia.mobile.staff.core.domain.repository.DeviceRepository
import javax.inject.Inject

/** Renames a tablet; [name] is trimmed. */
class RenameDeviceUseCase @Inject constructor(
    private val repository: DeviceRepository,
) {
    suspend operator fun invoke(deviceId: String, name: String): Result<Device> =
        repository.renameDevice(deviceId, name.trim())
}
