package com.qhathia.mobile.staff.core.domain.model

import java.time.Duration
import java.time.Instant

/**
 * A tablet (terminal) paired to the store. Telemetry ([batteryLevel], [networkStatus]) reflects the
 * tablet's last heartbeat, not the current instant — read it together with [lastPingAt].
 *
 * @property name the friendly name set on pairing or rename; `null` when the backend has none.
 * @property batteryLevel 0–100, or `null` when the tablet never reported it.
 */
data class Device(
    val id: String,
    val serialNumber: String?,
    val status: DeviceStatus,
    val appVersion: String?,
    val linkedAt: String?,
    val name: String? = null,
    val batteryLevel: Int? = null,
    val networkStatus: String? = null,
    val storeName: String? = null,
    val lastPingAt: Instant? = null,
    val lastSyncAt: Instant? = null,
    val model: String? = null,
) {
    /**
     * A tablet counts as connected while it is ACTIVE and has sent a heartbeat within
     * [CONNECTED_PING_WINDOW]. The backend does not document its heartbeat interval, so the window
     * is a single conservative constant.
     */
    fun isConnected(now: Instant): Boolean {
        val lastPing = lastPingAt ?: return false
        return status == DeviceStatus.ACTIVE && !lastPing.isBefore(now.minus(CONNECTED_PING_WINDOW))
    }

    companion object {
        val CONNECTED_PING_WINDOW: Duration = Duration.ofMinutes(10)
    }
}

/** Lifecycle state of a device. Mirrors the backend `DeviceStatus` enum; [UNKNOWN] is a safe fallback. */
enum class DeviceStatus {
    PENDING_LINK,
    ACTIVE,
    INACTIVE,
    SUSPENDED,
    UNKNOWN,
}
