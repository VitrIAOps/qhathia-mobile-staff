package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.repository.AccountSecurityRepository
import javax.inject.Inject

/** Closes another device's session. */
class RevokeSessionUseCase @Inject constructor(
    private val repository: AccountSecurityRepository,
) {
    suspend operator fun invoke(sessionId: String): Result<Unit> =
        repository.revokeSession(sessionId)
}
