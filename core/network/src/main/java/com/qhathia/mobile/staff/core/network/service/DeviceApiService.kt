package com.qhathia.mobile.staff.core.network.service

import io.ktor.client.request.patch
import com.qhathia.mobile.staff.core.network.dto.RenameDeviceRequestDto
import com.qhathia.mobile.staff.core.network.dto.PairingSessionStatusDto
import com.qhathia.mobile.staff.core.network.NetworkResult
import com.qhathia.mobile.staff.core.network.dto.DeviceCommandDto
import com.qhathia.mobile.staff.core.network.dto.DeviceDto
import com.qhathia.mobile.staff.core.network.dto.DeviceSettingsDto
import com.qhathia.mobile.staff.core.network.dto.PairDeviceRequestDto
import com.qhathia.mobile.staff.core.network.dto.PairDeviceResponseDto
import com.qhathia.mobile.staff.core.network.dto.SendDeviceCommandRequestDto
import com.qhathia.mobile.staff.core.network.dto.UpdateDeviceSettingsRequestDto
import com.qhathia.mobile.staff.core.network.toNetworkResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import kotlinx.serialization.json.Json

/**
 * Wraps the devices API. Runs on the authenticated client so the OWNER bearer token (and its
 * refresh) is injected by the Ktor Auth plugin.
 */
class DeviceApiService(
    private val client: HttpClient,
    private val json: Json,
) {

    suspend fun getDevices(): NetworkResult<List<DeviceDto>> =
        runCatching {
            client.get("api/v1/devices").body<List<DeviceDto>>()
        }.toNetworkResult(json)

    suspend fun pairDevice(body: PairDeviceRequestDto): NetworkResult<PairDeviceResponseDto> =
        runCatching {
            client.post("api/v1/devices/pair") { setBody(body) }.body<PairDeviceResponseDto>()
        }.toNetworkResult(json)

    /** Detaches the tablet from the store (OWNER/ADMIN). It answers the device, now `INACTIVE`. */
    suspend fun unlinkDevice(deviceId: String): NetworkResult<DeviceDto> =
        runCatching {
            client.post("api/v1/devices/$deviceId/unlink").body<DeviceDto>()
        }.toNetworkResult(json)

    /** Whether the tablet exchanged the code of an approved pairing session (public, read-only). */
    suspend fun getPairingStatus(sessionId: String): NetworkResult<PairingSessionStatusDto> =
        runCatching {
            client.get("api/v1/devices/pairing-session/$sessionId/status").body<PairingSessionStatusDto>()
        }.toNetworkResult(json)

    /** Renames the tablet (OWNER/ADMIN); answers the device. */
    suspend fun renameDevice(deviceId: String, body: RenameDeviceRequestDto): NetworkResult<DeviceDto> =
        runCatching {
            client.patch("api/v1/devices/$deviceId") { setBody(body) }.body<DeviceDto>()
        }.toNetworkResult(json)

    /** Queues a command (`202`). Idempotent while one of the same type is still in flight. */
    suspend fun sendCommand(
        deviceId: String,
        body: SendDeviceCommandRequestDto,
    ): NetworkResult<DeviceCommandDto> =
        runCatching {
            client.post("api/v1/devices/$deviceId/commands") { setBody(body) }
                .body<DeviceCommandDto>()
        }.toNetworkResult(json)

    /** The device's last 20 commands, newest first. */
    suspend fun getCommands(deviceId: String): NetworkResult<List<DeviceCommandDto>> =
        runCatching {
            client.get("api/v1/devices/$deviceId/commands").body<List<DeviceCommandDto>>()
        }.toNetworkResult(json)

    /** The tablet's remote settings (OWNER/ADMIN). The first read creates the defaults. */
    suspend fun getSettings(deviceId: String): NetworkResult<DeviceSettingsDto> =
        runCatching {
            client.get("api/v1/devices/$deviceId/settings").body<DeviceSettingsDto>()
        }.toNetworkResult(json)

    /** Partial update: only the non-null fields of [body] change. Answers the full applied set. */
    suspend fun updateSettings(
        deviceId: String,
        body: UpdateDeviceSettingsRequestDto,
    ): NetworkResult<DeviceSettingsDto> =
        runCatching {
            client.put("api/v1/devices/$deviceId/settings") { setBody(body) }.body<DeviceSettingsDto>()
        }.toNetworkResult(json)
}
