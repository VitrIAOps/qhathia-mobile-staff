package com.qhathia.mobile.staff.core.domain.model

import java.time.Instant

/**
 * One signed-in device of the user. A session is a device, not a credential: it keeps its
 * [sessionId] across token rotations.
 *
 * @property isCurrent true for the device the app is running on.
 */
data class ActiveSession(
    val sessionId: String,
    val deviceLabel: String,
    val lastUsedAt: Instant?,
    val isCurrent: Boolean,
)
