package com.qhathia.mobile.staff.core.domain.model

/**
 * A tablet (terminal) paired to the store, as shown in the linked-devices list. The list endpoint
 * (`GET /api/v1/devices`) does not expose a friendly device name, so [serialNumber] is the closest
 * identifier and the presentation layer falls back to a generic label when it is absent.
 */
data class Device(
    val id: String,
    val serialNumber: String?,
    val status: DeviceStatus,
    val appVersion: String?,
    val linkedAt: String?,
)

/** Lifecycle state of a device. Mirrors the backend `DeviceStatus` enum; [UNKNOWN] is a safe fallback. */
enum class DeviceStatus {
    PENDING_LINK,
    ACTIVE,
    INACTIVE,
    SUSPENDED,
    UNKNOWN,
}
