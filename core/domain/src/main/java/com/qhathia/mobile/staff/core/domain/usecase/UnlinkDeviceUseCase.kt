package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.repository.DeviceRepository
import javax.inject.Inject

/** Detaches a tablet from the current store. */
class UnlinkDeviceUseCase @Inject constructor(
    private val repository: DeviceRepository,
) {
    suspend operator fun invoke(deviceId: String): Result<Unit> = repository.unlinkDevice(deviceId)
}
