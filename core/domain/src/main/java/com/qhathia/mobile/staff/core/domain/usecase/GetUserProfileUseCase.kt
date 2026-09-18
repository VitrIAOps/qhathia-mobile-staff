package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.model.UserProfile
import com.qhathia.mobile.staff.core.domain.repository.UserProfileRepository
import javax.inject.Inject

/** Loads the authenticated user's profile. */
class GetUserProfileUseCase @Inject constructor(
    private val repository: UserProfileRepository,
) {
    suspend operator fun invoke(): Result<UserProfile> = repository.getProfile()
}
