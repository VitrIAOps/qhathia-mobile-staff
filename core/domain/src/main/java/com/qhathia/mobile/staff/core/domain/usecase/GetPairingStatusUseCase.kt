package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.model.PairingSessionStatus
import com.qhathia.mobile.staff.core.domain.repository.DeviceRepository
import javax.inject.Inject

/** Whether the tablet exchanged the code of an approved pairing session. */
class GetPairingStatusUseCase @Inject constructor(
    private val repository: DeviceRepository,
) {
    suspend operator fun invoke(sessionId: String): Result<PairingSessionStatus> =
        repository.getPairingStatus(sessionId)
}
