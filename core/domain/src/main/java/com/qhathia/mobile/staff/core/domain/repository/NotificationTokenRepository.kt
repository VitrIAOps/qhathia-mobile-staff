package com.qhathia.mobile.staff.core.domain.repository

/**
 * Port for registering and unregistering this device's push token with the backend so the store's
 * devices receive order pushes. Implemented in the data layer over the authenticated network client;
 * the `storeId`/`userId` are derived server-side from the JWT.
 */
interface NotificationTokenRepository {

    /** Register (upsert) the FCM [token] for the authenticated store/user. */
    suspend fun registerToken(token: String, deviceLabel: String?): Result<Unit>

    /** Unregister the FCM [token] (logout / opt-out). Idempotent server-side. */
    suspend fun unregisterToken(token: String): Result<Unit>
}
