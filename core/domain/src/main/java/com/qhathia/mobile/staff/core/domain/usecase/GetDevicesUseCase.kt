package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.model.Device
import com.qhathia.mobile.staff.core.domain.repository.DeviceRepository
import javax.inject.Inject

/** Lists the tablets paired to the current store. */
class GetDevicesUseCase @Inject constructor(
    private val repository: DeviceRepository,
) {
    suspend operator fun invoke(): Result<List<Device>> = repository.getDevices()
}
