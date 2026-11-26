package com.qhathia.mobile.staff.core.testing

import com.qhathia.mobile.staff.core.domain.repository.AuthTokenCache

/** [AuthTokenCache] for tests: counts [clear] calls. */
class FakeAuthTokenCache : AuthTokenCache {

    var clearCallCount: Int = 0
        private set

    override fun clear() {
        clearCallCount++
    }
}
