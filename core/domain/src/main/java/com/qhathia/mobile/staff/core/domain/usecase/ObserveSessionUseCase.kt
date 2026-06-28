package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.model.Session
import com.qhathia.mobile.staff.core.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Streams the current [Session] (or `null` when unauthenticated). */
class ObserveSessionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
) {
    operator fun invoke(): Flow<Session?> = sessionRepository.observeSession()
}
