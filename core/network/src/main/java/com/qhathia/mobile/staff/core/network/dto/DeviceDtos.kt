package com.qhathia.mobile.staff.core.network.dto

import kotlinx.serialization.Serializable

/**
 * Row of `GET /api/v1/devices` (§18 shape). All fields but [id] are optional because the list mixes
 * provisioning fields that may be null (an unlinked device has no `linkedAt`). [batteryLevel] and
 * [networkStatus] come from the tablet's last heartbeat, so they must be read with [lastPingAt].
 */
@Serializable
data class DeviceDto(
    val id: String,
    val storeId: String? = null,
    val name: String? = null,
    val storeName: String? = null,
    val serialNumber: String? = null,
    val status: String? = null,
    val batteryLevel: Int? = null,
    val networkStatus: String? = null,
    val androidVersion: String? = null,
    val appVersion: String? = null,
    val lastPingAt: String? = null,
    val lastSyncAt: String? = null,
    val linkedAt: String? = null,
    val createdAt: String? = null,
    /** Commercial model ("Lenovo Tab M11"), reported by the tablet in its heartbeat. */
    val model: String? = null,
)

/**
 * Body of `POST /api/v1/devices/pair`. The merchant enters the [pairingToken] shown on the tablet
 * and approves (`APPROVE`) or rejects (`REJECT`) it. The metadata is optional and a null field is
 * not sent (the shared Json does not encode defaults): without [deviceName] the backend names the
 * tablet "Tablet N", and [appVersion] arrives with the tablet's first heartbeat.
 */
@Serializable
data class PairDeviceRequestDto(
    val pairingToken: String,
    val decision: String,
    val deviceName: String? = null,
    val platform: String? = null,
    val appVersion: String? = null,
)

/**
 * Response of `GET /api/v1/devices/pairing-session/{sessionId}/status` (public, read-only). After
 * approving, [tokenExchanged] turns true once the tablet exchanged the code for its token.
 */
@Serializable
data class PairingSessionStatusDto(
    val sessionId: String? = null,
    val status: String? = null,
    val expiresAt: String? = null,
    val tokenExchanged: Boolean = false,
    val tokenExchangedAt: String? = null,
    val deviceId: String? = null,
)

/** Body of `PATCH /api/v1/devices/{deviceId}`. */
@Serializable
data class RenameDeviceRequestDto(val name: String)

/**
 * Response of `POST /api/v1/devices/pair`. Only the outcome is consumed by the client; extra fields
 * are ignored by Json.
 */
@Serializable
data class PairDeviceResponseDto(
    val sessionId: String? = null,
    val status: String? = null,
    val storeId: String? = null,
    val deviceId: String? = null,
    val deviceName: String? = null,
    val platform: String? = null,
)

/** Body of `POST /api/v1/devices/{deviceId}/commands`: `REFRESH_CATALOG` or `RESTART_APP`. */
@Serializable
data class SendDeviceCommandRequestDto(
    val command: String,
)

/** A remote command as returned by the commands endpoints (§23). */
@Serializable
data class DeviceCommandDto(
    val id: String,
    val deviceId: String? = null,
    val command: String? = null,
    val status: String? = null,
    val requestedAt: String? = null,
    val expiresAt: String? = null,
    val deliveredAt: String? = null,
    val ackedAt: String? = null,
    val resultDetail: String? = null,
)

/**
 * Body of `GET` / `PUT /api/v1/devices/{deviceId}/settings`. The PIN never comes back, only whether
 * one is set ([exitPinSet]).
 */
@Serializable
data class DeviceSettingsDto(
    val deviceId: String? = null,
    val idleShowcaseEnabled: Boolean = true,
    val voiceAssistantEnabled: Boolean = true,
    val brightnessMode: String? = null,
    val kioskMode: Boolean = true,
    val exitPinSet: Boolean = false,
    val autoSyncEnabled: Boolean = true,
    val settingsVersion: Long? = null,
)

/**
 * Body of the partial `PUT /api/v1/devices/{deviceId}/settings`: a null field is not encoded, and an
 * absent field is left untouched by the backend. Removing the PIN needs an explicit
 * [removeExitPin] — an absent [exitPin] means "do not touch it".
 */
@Serializable
data class UpdateDeviceSettingsRequestDto(
    val idleShowcaseEnabled: Boolean? = null,
    val voiceAssistantEnabled: Boolean? = null,
    val brightnessMode: String? = null,
    val kioskMode: Boolean? = null,
    val exitPin: String? = null,
    val removeExitPin: Boolean? = null,
    val autoSyncEnabled: Boolean? = null,
)
