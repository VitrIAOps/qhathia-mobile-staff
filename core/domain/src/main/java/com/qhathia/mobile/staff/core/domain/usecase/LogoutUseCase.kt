package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.repository.SessionRepository
import javax.inject.Inject

/** Clears the stored session, returning the app to the unauthenticated state. */
class LogoutUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    suspend operator fun invoke() = sessionRepository.clearSession()
}
