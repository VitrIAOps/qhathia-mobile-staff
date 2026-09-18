package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.repository.AppPreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Streams whether fingerprint sign-in is enabled on this device. */
class ObserveBiometricEnabledUseCase @Inject constructor(
    private val repository: AppPreferencesRepository,
) {
    operator fun invoke(): Flow<Boolean> = repository.observeBiometricEnabled()
}
