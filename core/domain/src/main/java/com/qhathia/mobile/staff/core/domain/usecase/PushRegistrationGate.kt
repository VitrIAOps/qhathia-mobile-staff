package com.qhathia.mobile.staff.core.domain.usecase

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Process-wide state shared by [RegisterPushTokenUseCase] and [UnregisterPushTokenUseCase]: one
 * registration or unregistration at a time, and the token last registered for a user, so the same
 * pair is not sent twice. After a new login the app asks to register from the sign-in and from FCM's
 * new-token callback at the same moment; without this they reached the backend together (F-47).
 */
@Singleton
class PushRegistrationGate @Inject constructor() {

    private val mutex = Mutex()
    private var registered: Registered? = null

    /** Runs [block] with exclusive access to the registration state. */
    internal suspend fun <T> withLock(block: suspend Registration.() -> T): T =
        mutex.withLock { Registration().block() }

    /** Access to the state while the lock is held. */
    internal inner class Registration {
        /** Whether [token] was registered successfully for [userId] and not unregistered since. */
        fun isRegistered(token: String, userId: String): Boolean = registered == Registered(token, userId)

        fun markRegistered(token: String, userId: String) {
            registered = Registered(token, userId)
        }

        fun forget() {
            registered = null
        }
    }

    private data class Registered(val token: String, val userId: String)
}
