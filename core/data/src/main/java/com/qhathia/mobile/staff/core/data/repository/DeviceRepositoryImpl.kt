package com.qhathia.mobile.staff.core.data.repository

import com.qhathia.mobile.staff.core.data.mapper.toDomain
import com.qhathia.mobile.staff.core.data.mapper.toRequestDto
import com.qhathia.mobile.staff.core.domain.model.Device
import com.qhathia.mobile.staff.core.domain.model.DeviceCommand
import com.qhathia.mobile.staff.core.domain.model.DeviceCommandType
import com.qhathia.mobile.staff.core.domain.model.DeviceSettings
import com.qhathia.mobile.staff.core.domain.model.DeviceSettingsChange
import com.qhathia.mobile.staff.core.domain.model.DeviceSettingsError
import com.qhathia.mobile.staff.core.domain.model.DeviceSettingsException
import com.qhathia.mobile.staff.core.domain.model.PairDeviceError
import com.qhathia.mobile.staff.core.domain.model.PairDeviceException
import com.qhathia.mobile.staff.core.domain.repository.DeviceRepository
import com.qhathia.mobile.staff.core.network.NetworkResult
import com.qhathia.mobile.staff.core.network.dto.DeviceSettingsDto
import com.qhathia.mobile.staff.core.network.dto.PairDeviceRequestDto
import com.qhathia.mobile.staff.core.network.dto.SendDeviceCommandRequestDto
import com.qhathia.mobile.staff.core.network.service.CatalogApiService
import com.qhathia.mobile.staff.core.network.service.DeviceApiService
import javax.inject.Inject

/**
 * Maps transport [NetworkResult]s to domain [Result]s at the data boundary, so the domain and
 * presentation layers never see Ktor or DTO types. Pairing approves the entered code with default
 * device metadata (POC); a successful approve maps to `Result.success(Unit)` and any failure carries
 * a [PairDeviceException] whose [PairDeviceError] is derived from the HTTP status (400 → invalid
 * code, 409/503 → catalog not ready).
 */
class DeviceRepositoryImpl @Inject constructor(
    private val apiService: DeviceApiService,
    private val catalogApiService: CatalogApiService,
) : DeviceRepository {

    override suspend fun getDevices(): Result<List<Device>> =
        apiService.getDevices().asDomainResult { devices -> devices.map { it.toDomain() } }

    override suspend fun pairDevice(code: String): Result<Unit> {
        val body = PairDeviceRequestDto(
            pairingToken = code.trim(),
            decision = DECISION_APPROVE,
            deviceName = DEFAULT_DEVICE_NAME,
            platform = PLATFORM_ANDROID,
            appVersion = DEFAULT_APP_VERSION,
        )
        return when (val result = apiService.pairDevice(body)) {
            is NetworkResult.Success ->
                Result.success(Unit)
            is NetworkResult.HttpError ->
                Result.failure(PairDeviceException(result.code.toPairError()))
            is NetworkResult.NetworkError ->
                Result.failure(PairDeviceException(PairDeviceError.Network))
        }
    }

    override suspend fun unlinkDevice(deviceId: String): Result<Unit> =
        apiService.unlinkDevice(deviceId).asDomainResult { }

    override suspend fun getCatalogCount(): Result<Int> =
        catalogApiService.getCount().asDomainResult { it.activeProducts.coerceAtLeast(0) }

    override suspend fun sendCommand(
        deviceId: String,
        type: DeviceCommandType,
    ): Result<DeviceCommand> =
        apiService.sendCommand(deviceId, SendDeviceCommandRequestDto(type.name))
            .asDomainResult { dto ->
                requireNotNull(dto.toDomain()) { "Unexpected command type ${dto.command}" }
            }

    override suspend fun getCommands(deviceId: String): Result<List<DeviceCommand>> =
        apiService.getCommands(deviceId).asDomainResult { commands ->
            commands.mapNotNull { it.toDomain() }
        }

    override suspend fun getSettings(deviceId: String): Result<DeviceSettings> =
        apiService.getSettings(deviceId).toSettingsResult()

    override suspend fun updateSettings(deviceId: String, change: DeviceSettingsChange): Result<DeviceSettings> =
        apiService.updateSettings(deviceId, change.toRequestDto()).toSettingsResult()

    private fun NetworkResult<DeviceSettingsDto>.toSettingsResult():
        Result<DeviceSettings> = when (this) {
        is NetworkResult.Success -> Result.success(data.toDomain())
        is NetworkResult.HttpError -> Result.failure(DeviceSettingsException(code.toSettingsError()))
        is NetworkResult.NetworkError -> Result.failure(DeviceSettingsException(DeviceSettingsError.Network))
    }

    private fun Int.toSettingsError(): DeviceSettingsError = when (this) {
        404 -> DeviceSettingsError.NotFound
        400, 422 -> DeviceSettingsError.InvalidInput
        503 -> DeviceSettingsError.Network
        else -> DeviceSettingsError.Unknown("HTTP $this")
    }

    private fun Int.toPairError(): PairDeviceError = when (this) {
        400 -> PairDeviceError.InvalidCode
        409, 503 -> PairDeviceError.CatalogNotReady
        else -> PairDeviceError.Unknown("HTTP $this")
    }

    companion object {
        private const val DECISION_APPROVE = "APPROVE"
        private const val PLATFORM_ANDROID = "ANDROID"
        private const val DEFAULT_DEVICE_NAME = "Tablet"
        private const val DEFAULT_APP_VERSION = "1.0.0"
    }
}
