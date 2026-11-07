package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.model.PairedDevice
import com.qhathia.mobile.staff.core.domain.repository.DeviceRepository
import javax.inject.Inject

/** Approves a pairing [code] for a tablet named [name]; the tablet links once it exchanges the code. */
class PairDeviceUseCase @Inject constructor(
    private val repository: DeviceRepository,
) {
    suspend operator fun invoke(code: String, name: String?): Result<PairedDevice> =
        repository.pairDevice(code, name)
}
