package com.qhathia.mobile.staff.core.domain.model

import java.time.Instant

/**
 * A remote command queued for a tablet. It travels on the tablet's heartbeat, so it is never
 * instantaneous: [DeviceCommandStatus.PENDING] and [DeviceCommandStatus.DELIVERED] both mean "sent,
 * not yet confirmed", and only the tablet's ack moves it to [DeviceCommandStatus.ACKED] or
 * [DeviceCommandStatus.FAILED]. Commands expire server-side after 10 minutes and are never retried;
 * [expiresAt] is that deadline as the backend states it, the moment the owner can retry at the latest.
 */
data class DeviceCommand(
    val id: String,
    val type: DeviceCommandType,
    val status: DeviceCommandStatus,
    val requestedAt: Instant?,
    val expiresAt: Instant?,
    val resultDetail: String?,
) {
    /** True while the tablet has not confirmed the outcome yet. */
    val isInFlight: Boolean
        get() = status == DeviceCommandStatus.PENDING || status == DeviceCommandStatus.DELIVERED
}

enum class DeviceCommandType {
    /** Forces an out-of-cycle catalog sync on the tablet. */
    REFRESH_CATALOG,

    /** Restarts the kiosk app — not the operating system. */
    RESTART_APP,
}

/** Mirrors the backend command lifecycle; [UNKNOWN] guards against unrecognized values. */
enum class DeviceCommandStatus {
    PENDING,
    DELIVERED,
    ACKED,
    FAILED,
    EXPIRED,
    UNKNOWN,
}
