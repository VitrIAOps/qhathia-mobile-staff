package com.qhathia.mobile.staff.core.testing

import com.qhathia.mobile.staff.core.domain.repository.PushTokenProvider

/**
 * Controllable [PushTokenProvider] for tests. [token] is what [currentToken] returns (set to `null`
 * to simulate an unobtainable token); [deleteToken] is counted for assertions.
 */
class FakePushTokenProvider(
    var token: String? = "fcm-token",
) : PushTokenProvider {

    var currentTokenCallCount: Int = 0
        private set
    var deleteTokenCallCount: Int = 0
        private set

    override suspend fun currentToken(): String? {
        currentTokenCallCount++
        return token
    }

    override suspend fun deleteToken() {
        deleteTokenCallCount++
    }
}
