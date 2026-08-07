package com.qhathia.mobile.staff.core.data.repository

import com.qhathia.mobile.staff.core.data.mapper.toSession
import com.qhathia.mobile.staff.core.data.mapper.toTokens
import com.qhathia.mobile.staff.core.domain.model.AuthTokens
import com.qhathia.mobile.staff.core.domain.model.LoginError
import com.qhathia.mobile.staff.core.domain.model.LoginException
import com.qhathia.mobile.staff.core.domain.model.Session
import com.qhathia.mobile.staff.core.domain.repository.AuthRepository
import com.qhathia.mobile.staff.core.network.NetworkResult
import com.qhathia.mobile.staff.core.network.dto.LoginRequestDto
import com.qhathia.mobile.staff.core.network.dto.LogoutRequestDto
import com.qhathia.mobile.staff.core.network.dto.RefreshRequestDto
import com.qhathia.mobile.staff.core.network.service.AuthApiService
import javax.inject.Inject

/**
 * Maps transport [NetworkResult]s to domain [Result]s. Failures carry a [LoginException] whose
 * [LoginError] is derived from the HTTP status (401 → invalid credentials, 400 → validation).
 */
class AuthRepositoryImpl @Inject constructor(
    private val authApiService: AuthApiService,
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<Session> =
        when (val result = authApiService.login(LoginRequestDto(email, password))) {
            is NetworkResult.Success ->
                runCatching { result.data.toSession() }
                    .recoverCatching { throw LoginException(LoginError.Unknown(it.message)) }
            is NetworkResult.HttpError ->
                Result.failure(LoginException(result.code.toLoginError()))
            is NetworkResult.NetworkError ->
                Result.failure(LoginException(LoginError.Network))
        }

    override suspend fun refresh(refreshToken: String): Result<AuthTokens> =
        when (val result = authApiService.refresh(RefreshRequestDto(refreshToken))) {
            is NetworkResult.Success -> Result.success(result.data.toTokens())
            is NetworkResult.HttpError ->
                Result.failure(LoginException(result.code.toLoginError()))
            is NetworkResult.NetworkError ->
                Result.failure(LoginException(LoginError.Network))
        }

    override suspend fun logout(refreshToken: String): Result<Unit> =
        when (val result = authApiService.logout(LogoutRequestDto(refreshToken))) {
            is NetworkResult.Success -> Result.success(Unit)
            is NetworkResult.HttpError ->
                Result.failure(LoginException(result.code.toLoginError()))
            is NetworkResult.NetworkError ->
                Result.failure(LoginException(LoginError.Network))
        }

    private fun Int.toLoginError(): LoginError = when (this) {
        401 -> LoginError.InvalidCredentials
        400, 422 -> LoginError.Validation
        else -> LoginError.Unknown("HTTP $this")
    }
}
