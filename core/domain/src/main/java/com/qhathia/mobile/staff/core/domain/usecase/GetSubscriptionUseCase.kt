package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.model.Subscription
import com.qhathia.mobile.staff.core.domain.repository.SubscriptionRepository
import javax.inject.Inject

/** Loads the store's current subscription; a successful `null` means the store has none. */
class GetSubscriptionUseCase @Inject constructor(
    private val repository: SubscriptionRepository,
) {
    suspend operator fun invoke(): Result<Subscription?> = repository.getCurrent()
}
