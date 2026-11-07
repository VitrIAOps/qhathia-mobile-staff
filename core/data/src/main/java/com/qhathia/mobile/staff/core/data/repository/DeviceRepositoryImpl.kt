package com.qhathia.mobile.staff.core.data.repository

import java.util.Locale
import com.qhathia.mobile.staff.core.network.dto.RenameDeviceRequestDto
import com.qhathia.mobile.staff.core.domain.model.PairingSessionStatus
import com.qhathia.mobile.staff.core.domain.model.PairedDevice
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
 * presentation layers never see Ktor or DTO types. Approving a pairing returns the device and
 * session it created, so the screen can wait for the tablet to exchange the code. Pairing failures
 * carry a [PairDeviceException] chosen by the backend's `errorCode`; unlink, rename and settings
 * failures carry a [DeviceSettingsException].
 */
class DeviceRepositoryImpl @Inject constructor(
    private val apiService: DeviceApiService,
    private val catalogApiService: CatalogApiService,
) : DeviceRepository {

    override suspend fun getDevices(): Result<List<Device>> =
        apiService.getDevices().asDomainResult { devices -> devices.map { it.toDomain() } }

    override suspend fun pairDevice(code: String, name: String?): Result<PairedDevice> {
        val body = PairDeviceRequestDto(
            pairingToken = code.normalizedCode(),
            decision = DECISION_APPROVE,
            deviceName = name?.trim()?.takeIf { it.isNotEmpty() },
            platform = PLATFORM_ANDROID,
        )
        return when (val result = apiService.pairDevice(body)) {
            is NetworkResult.Success -> {
                val deviceId = result.data.deviceId
                val sessionId = result.data.sessionId
                if (deviceId == null || sessionId == null) {
                    Result.failure(PairDeviceException(PairDeviceError.Unknown("Approval without device or session")))
                } else {
                    Result.success(PairedDevice(deviceId, sessionId, result.data.deviceName))
                }
            }
            is NetworkResult.HttpError -> Result.failure(PairDeviceException(result.toPairError()))
            is NetworkResult.NetworkError -> Result.failure(PairDeviceException(PairDeviceError.Network))
        }
    }

    override suspend fun rejectPairing(code: String): Result<Unit> {
        val body = PairDeviceRequestDto(pairingToken = code.normalizedCode(), decision = DECISION_REJECT)
        return when (val result = apiService.pairDevice(body)) {
            is NetworkResult.Success -> Result.success(Unit)
            is NetworkResult.HttpError -> Result.failure(PairDeviceException(result.toPairError()))
            is NetworkResult.NetworkError -> Result.failure(PairDeviceException(PairDeviceError.Network))
        }
    }

    override suspend fun getPairingStatus(sessionId: String): Result<PairingSessionStatus> =
        apiService.getPairingStatus(sessionId).asDomainResult {
            PairingSessionStatus(status = it.status, tokenExchanged = it.tokenExchanged)
        }

    override suspend fun unlinkDevice(deviceId: String): Result<Unit> =
        when (val result = apiService.unlinkDevice(deviceId)) {
            is NetworkResult.Success -> Result.success(Unit)
            is NetworkResult.HttpError -> Result.failure(DeviceSettingsException(result.code.toSettingsError()))
            is NetworkResult.NetworkError -> Result.failure(DeviceSettingsException(DeviceSettingsError.Network))
        }

    override suspend fun renameDevice(deviceId: String, name: String): Result<Device> =
        when (val result = apiService.renameDevice(deviceId, RenameDeviceRequestDto(name))) {
            is NetworkResult.Success -> Result.success(result.data.toDomain())
            is NetworkResult.HttpError -> Result.failure(DeviceSettingsException(result.code.toSettingsError()))
            is NetworkResult.NetworkError -> Result.failure(DeviceSettingsException(DeviceSettingsError.Network))
        }

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

    /** The backend normalizes too; this keeps what is sent equal to what the field shows. */
    private fun String.normalizedCode(): String = trim().uppercase(Locale.ROOT)

    /** By the ProblemDetail `errorCode` first: the four code errors are all HTTP 400. */
    private fun NetworkResult.HttpError.toPairError(): PairDeviceError {
        val errorCode = problem?.errorCode
        return when {
            errorCode == ERROR_EXPIRED -> PairDeviceError.ExpiredCode
            errorCode == ERROR_ALREADY_USED -> PairDeviceError.AlreadyUsedCode
            errorCode == ERROR_CANCELLED -> PairDeviceError.CancelledCode
            errorCode == ERROR_CATALOG_THRESHOLD || code == 409 -> PairDeviceError.CatalogNotReady(problem?.detail)
            code == 503 -> PairDeviceError.CatalogCheckUnavailable
            code == 400 -> PairDeviceError.InvalidCode
            else -> PairDeviceError.Unknown("HTTP $code")
        }
    }

    companion object {
        private const val DECISION_APPROVE = "APPROVE"
        private const val DECISION_REJECT = "REJECT"
        private const val PLATFORM_ANDROID = "ANDROID"
        private const val ERROR_EXPIRED = "PAIRING_TOKEN_EXPIRED"
        private const val ERROR_ALREADY_USED = "PAIRING_TOKEN_ALREADY_USED"
        private const val ERROR_CANCELLED = "PAIRING_TOKEN_CANCELLED"
        private const val ERROR_CATALOG_THRESHOLD = "CATALOG_THRESHOLD_NOT_MET"
    }
}
