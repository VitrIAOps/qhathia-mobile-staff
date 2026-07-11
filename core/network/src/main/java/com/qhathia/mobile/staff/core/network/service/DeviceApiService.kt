package com.qhathia.mobile.staff.core.network.service

import com.qhathia.mobile.staff.core.network.NetworkResult
import com.qhathia.mobile.staff.core.network.dto.DeviceDto
import com.qhathia.mobile.staff.core.network.dto.PairDeviceRequestDto
import com.qhathia.mobile.staff.core.network.dto.PairDeviceResponseDto
import com.qhathia.mobile.staff.core.network.toNetworkResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody

/**
 * Wraps the devices API. Runs on the authenticated client so the OWNER bearer token (and its
 * refresh) is injected by the Ktor Auth plugin.
 */
class DeviceApiService(private val client: HttpClient) {

    suspend fun getDevices(): NetworkResult<List<DeviceDto>> =
        runCatching {
            client.get("api/v1/devices").body<List<DeviceDto>>()
        }.toNetworkResult()

    suspend fun pairDevice(body: PairDeviceRequestDto): NetworkResult<PairDeviceResponseDto> =
        runCatching {
            client.post("api/v1/devices/pair") { setBody(body) }.body<PairDeviceResponseDto>()
        }.toNetworkResult()
}
