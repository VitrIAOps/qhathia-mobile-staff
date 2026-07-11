package com.qhathia.mobile.staff.core.network.dto

import kotlinx.serialization.Serializable

/**
 * Row of `GET /api/v1/devices`. All fields but [id] are optional because the list endpoint returns
 * provisioning fields that may be null (e.g. an unlinked device has no `linkedAt`). This endpoint
 * exposes no friendly device name — [serialNumber] is the closest identifier.
 */
@Serializable
data class DeviceDto(
    val id: String,
    val storeId: String? = null,
    val serialNumber: String? = null,
    val status: String? = null,
    val androidVersion: String? = null,
    val appVersion: String? = null,
    val lastPingAt: String? = null,
    val linkedAt: String? = null,
    val createdAt: String? = null,
)

/**
 * Body of `POST /api/v1/devices/pair`. The merchant enters the [pairingToken] shown on the tablet
 * and approves it; the remaining fields are device metadata required by the contract.
 */
@Serializable
data class PairDeviceRequestDto(
    val pairingToken: String,
    val decision: String,
    val deviceName: String,
    val platform: String,
    val appVersion: String,
)

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
