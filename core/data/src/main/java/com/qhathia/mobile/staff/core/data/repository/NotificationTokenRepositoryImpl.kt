package com.qhathia.mobile.staff.core.data.repository

import com.qhathia.mobile.staff.core.domain.repository.NotificationTokenRepository
import com.qhathia.mobile.staff.core.network.NetworkResult
import com.qhathia.mobile.staff.core.network.dto.RegisterTokenRequestDto
import com.qhathia.mobile.staff.core.network.dto.UnregisterTokenRequestDto
import com.qhathia.mobile.staff.core.network.service.NotificationApiService
import javax.inject.Inject

/**
 * Maps transport [NetworkResult]s to domain [Result]s at the data boundary. The store/user are
 * derived server-side from the JWT, so only the token (and an optional label) travel in the body.
 */
class NotificationTokenRepositoryImpl @Inject constructor(
    private val apiService: NotificationApiService,
) : NotificationTokenRepository {

    override suspend fun registerToken(token: String, deviceLabel: String?): Result<Unit> =
        apiService.registerToken(
            RegisterTokenRequestDto(token = token, platform = PLATFORM_ANDROID, deviceLabel = deviceLabel),
        ).toUnitResult()

    override suspend fun unregisterToken(token: String): Result<Unit> =
        apiService.unregisterToken(UnregisterTokenRequestDto(token = token)).toUnitResult()

    private fun NetworkResult<Unit>.toUnitResult(): Result<Unit> = when (this) {
        is NetworkResult.Success -> Result.success(Unit)
        is NetworkResult.HttpError ->
            Result.failure(IllegalStateException("HTTP $code: $message"))
        is NetworkResult.NetworkError -> Result.failure(cause)
    }

    companion object {
        private const val PLATFORM_ANDROID = "ANDROID"
    }
}
