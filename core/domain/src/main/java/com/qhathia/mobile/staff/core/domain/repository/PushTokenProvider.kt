package com.qhathia.mobile.staff.core.domain.repository

/**
 * Port over the device push-messaging SDK (Firebase Cloud Messaging), keeping the SDK out of the
 * domain. Implemented in the notifications layer.
 */
interface PushTokenProvider {

    /** The current FCM registration token, or `null` if it cannot be obtained. */
    suspend fun currentToken(): String?

    /** Delete the local FCM token so this device stops receiving pushes. */
    suspend fun deleteToken()
}
