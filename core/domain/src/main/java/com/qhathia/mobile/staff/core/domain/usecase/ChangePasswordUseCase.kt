package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.repository.AccountSecurityRepository
import javax.inject.Inject

/** Changes the password keeping this device signed in. */
class ChangePasswordUseCase @Inject constructor(
    private val repository: AccountSecurityRepository,
) {
    suspend operator fun invoke(currentPassword: String, newPassword: String): Result<Unit> =
        repository.changePassword(currentPassword, newPassword)
}
