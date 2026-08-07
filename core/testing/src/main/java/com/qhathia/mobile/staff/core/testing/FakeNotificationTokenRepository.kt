package com.qhathia.mobile.staff.core.testing

import com.qhathia.mobile.staff.core.domain.repository.NotificationTokenRepository

/**
 * Controllable [NotificationTokenRepository] for tests. Set [registerResult] / [unregisterResult] to
 * the value the next call should return; call arguments and counts are captured for assertions.
 */
class FakeNotificationTokenRepository : NotificationTokenRepository {

    var registerResult: Result<Unit> = Result.success(Unit)
    var unregisterResult: Result<Unit> = Result.success(Unit)

    var lastRegisteredToken: String? = null
        private set
    var lastRegisteredLabel: String? = null
        private set
    var registerCallCount: Int = 0
        private set
    var lastUnregisteredToken: String? = null
        private set
    var unregisterCallCount: Int = 0
        private set

    override suspend fun registerToken(token: String, deviceLabel: String?): Result<Unit> {
        registerCallCount++
        lastRegisteredToken = token
        lastRegisteredLabel = deviceLabel
        return registerResult
    }

    override suspend fun unregisterToken(token: String): Result<Unit> {
        unregisterCallCount++
        lastUnregisteredToken = token
        return unregisterResult
    }
}
