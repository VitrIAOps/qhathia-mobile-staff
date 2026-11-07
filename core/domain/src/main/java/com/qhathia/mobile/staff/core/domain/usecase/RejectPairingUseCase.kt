package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.repository.DeviceRepository
import javax.inject.Inject

/** Declines a pairing [code]; the tablet shows that the store rejected it. */
class RejectPairingUseCase @Inject constructor(
    private val repository: DeviceRepository,
) {
    suspend operator fun invoke(code: String): Result<Unit> = repository.rejectPairing(code)
}
