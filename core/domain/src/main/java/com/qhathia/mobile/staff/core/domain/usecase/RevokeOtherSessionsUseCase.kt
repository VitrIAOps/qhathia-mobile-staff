package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.repository.AccountSecurityRepository
import javax.inject.Inject

/** Closes every session except this device's. */
class RevokeOtherSessionsUseCase @Inject constructor(
    private val repository: AccountSecurityRepository,
) {
    suspend operator fun invoke(): Result<Unit> = repository.revokeOtherSessions()
}
