package com.qhathia.mobile.staff.core.testing

import com.qhathia.mobile.staff.core.domain.model.AuthTokens
import com.qhathia.mobile.staff.core.domain.model.Session
import com.qhathia.mobile.staff.core.domain.repository.AuthRepository
import kotlinx.coroutines.CompletableDeferred

/**
 * Controllable [AuthRepository] for tests. Set [loginResult] / [refreshResult] to the value the
 * next call should return; the most recent call arguments are captured for assertions. Set
 * [loginGate] to a pending deferred to hold [login] in-flight (to observe loading state).
 */
class FakeAuthRepository : AuthRepository {

    var loginResult: Result<Session> = Result.success(TestFixtures.session())
    var refreshResult: Result<AuthTokens> = Result.success(TestFixtures.tokens())

    /** When non-null, [login] suspends until this completes — lets tests observe in-flight state. */
    var loginGate: CompletableDeferred<Unit>? = null

    var lastLoginEmail: String? = null
        private set
    var lastLoginPassword: String? = null
        private set
    var loginCallCount: Int = 0
        private set

    override suspend fun login(email: String, password: String): Result<Session> {
        loginCallCount++
        lastLoginEmail = email
        lastLoginPassword = password
        loginGate?.await()
        return loginResult
    }

    override suspend fun refresh(refreshToken: String): Result<AuthTokens> = refreshResult
}
