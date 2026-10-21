package com.qhathia.mobile.staff.core.data.repository

import com.qhathia.mobile.staff.core.data.mapper.toDomain
import com.qhathia.mobile.staff.core.domain.model.UpdateProfileError
import com.qhathia.mobile.staff.core.domain.model.UpdateProfileException
import com.qhathia.mobile.staff.core.domain.model.UserProfile
import com.qhathia.mobile.staff.core.domain.repository.UserProfileRepository
import com.qhathia.mobile.staff.core.network.NetworkResult
import com.qhathia.mobile.staff.core.network.service.UserApiService
import javax.inject.Inject

/** [UserProfileRepository] over `GET` and `PUT /users/me`. */
class UserProfileRepositoryImpl @Inject constructor(
    private val apiService: UserApiService,
) : UserProfileRepository {

    override suspend fun getProfile(): Result<UserProfile> =
        apiService.getMe().asDomainResult { it.toDomain() }

    /** Reads the status itself rather than via `asDomainResult`, which would drop the `422`. */
    override suspend fun updateProfile(fullName: String?, phone: String?): Result<UserProfile> =
        when (val result = apiService.updateMe(fullName = fullName, phone = phone)) {
            is NetworkResult.Success -> runCatching { result.data.toDomain() }
            is NetworkResult.HttpError -> Result.failure(
                UpdateProfileException(
                    if (result.code == HTTP_UNPROCESSABLE) {
                        UpdateProfileError.Validation
                    } else {
                        UpdateProfileError.Unknown("HTTP ${result.code}")
                    },
                ),
            )
            is NetworkResult.NetworkError -> Result.failure(UpdateProfileException(UpdateProfileError.Network))
        }

    private companion object {
        const val HTTP_UNPROCESSABLE = 422
    }
}
