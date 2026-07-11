package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.repository.DeviceRepository
import javax.inject.Inject

/** Pairs (approves) a device by its pairing [code]. */
class PairDeviceUseCase @Inject constructor(
    private val repository: DeviceRepository,
) {
    suspend operator fun invoke(code: String): Result<Unit> = repository.pairDevice(code)
}
