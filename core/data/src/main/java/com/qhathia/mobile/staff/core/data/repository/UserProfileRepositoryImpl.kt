package com.qhathia.mobile.staff.core.data.repository

import com.qhathia.mobile.staff.core.data.mapper.toDomain
import com.qhathia.mobile.staff.core.domain.model.UserProfile
import com.qhathia.mobile.staff.core.domain.repository.UserProfileRepository
import com.qhathia.mobile.staff.core.network.service.UserApiService
import javax.inject.Inject

/** [UserProfileRepository] over `GET /users/me`. */
class UserProfileRepositoryImpl @Inject constructor(
    private val apiService: UserApiService,
) : UserProfileRepository {

    override suspend fun getProfile(): Result<UserProfile> =
        apiService.getMe().asDomainResult { it.toDomain() }
}
