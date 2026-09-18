package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.repository.AppPreferencesRepository
import javax.inject.Inject

/**
 * Turns fingerprint sign-in on or off. The caller must have authenticated with the device biometric
 * before enabling it.
 */
class SetBiometricEnabledUseCase @Inject constructor(
    private val repository: AppPreferencesRepository,
) {
    suspend operator fun invoke(enabled: Boolean) = repository.setBiometricEnabled(enabled)
}
