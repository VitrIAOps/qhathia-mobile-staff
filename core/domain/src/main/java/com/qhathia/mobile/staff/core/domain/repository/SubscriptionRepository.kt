package com.qhathia.mobile.staff.core.domain.repository

import com.qhathia.mobile.staff.core.domain.model.Subscription

/** Port for the store's current subscription. OWNER/ADMIN only. */
interface SubscriptionRepository {

    /** The current subscription, or a successful `null` when the store has none (`404`). */
    suspend fun getCurrent(): Result<Subscription?>
}
