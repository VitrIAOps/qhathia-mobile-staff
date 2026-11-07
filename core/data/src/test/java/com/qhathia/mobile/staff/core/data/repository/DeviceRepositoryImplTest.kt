package com.qhathia.mobile.staff.core.data.repository

import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import com.qhathia.mobile.staff.core.network.dto.RenameDeviceRequestDto
import com.qhathia.mobile.staff.core.network.dto.PairingSessionStatusDto
import com.qhathia.mobile.staff.core.network.dto.ProblemDto
import com.qhathia.mobile.staff.core.domain.model.PairedDevice
import com.qhathia.mobile.staff.core.domain.model.DeviceSettingsChange
import com.qhathia.mobile.staff.core.domain.model.DeviceSettingsError
import com.qhathia.mobile.staff.core.domain.model.DeviceSettingsException
import com.qhathia.mobile.staff.core.domain.model.DeviceStatus
import com.qhathia.mobile.staff.core.domain.model.PairDeviceError
import com.qhathia.mobile.staff.core.domain.model.PairDeviceException
import com.qhathia.mobile.staff.core.network.NetworkResult
import com.qhathia.mobile.staff.core.network.dto.DeviceDto
import com.qhathia.mobile.staff.core.network.dto.DeviceSettingsDto
import com.qhathia.mobile.staff.core.network.dto.UpdateDeviceSettingsRequestDto
import com.qhathia.mobile.staff.core.network.dto.PairDeviceRequestDto
import com.qhathia.mobile.staff.core.network.dto.PairDeviceResponseDto
import com.qhathia.mobile.staff.core.network.service.CatalogApiService
import com.qhathia.mobile.staff.core.network.service.DeviceApiService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException

class DeviceRepositoryImplTest {

    private val api = mockk<DeviceApiService>()
    private val catalogApi = mockk<CatalogApiService>()
    private val repository = DeviceRepositoryImpl(api, catalogApi)

    private fun deviceDto() = DeviceDto(
        id = "d1",
        storeId = "s1",
        serialNumber = "SN-001",
        status = "ACTIVE",
        androidVersion = "13",
        appVersion = "1.0.0",
        linkedAt = "2026-04-15T18:30:00Z",
    )

    @Test
    fun `given success when getDevices then maps dtos to domain`() = runTest {
        coEvery { api.getDevices() } returns NetworkResult.Success(listOf(deviceDto()))

        val result = repository.getDevices()

        assertTrue(result.isSuccess)
        val device = result.getOrThrow().single()
        assertEquals("d1", device.id)
        assertEquals("SN-001", device.serialNumber)
        assertEquals(DeviceStatus.ACTIVE, device.status)
    }

    @Test
    fun `given empty list when getDevices then returns empty success`() = runTest {
        coEvery { api.getDevices() } returns NetworkResult.Success(emptyList())

        val result = repository.getDevices()

        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().isEmpty())
    }

    @Test
    fun `given unknown status when getDevices then defaults to UNKNOWN`() = runTest {
        coEvery { api.getDevices() } returns
            NetworkResult.Success(listOf(deviceDto().copy(status = "WAT")))

        val device = repository.getDevices().getOrThrow().single()

        assertEquals(DeviceStatus.UNKNOWN, device.status)
    }

    @Test
    fun `given http error when getDevices then fails`() = runTest {
        coEvery { api.getDevices() } returns NetworkResult.HttpError(401, "Unauthorized")

        val result = repository.getDevices()

        assertTrue(result.isFailure)
    }

    @Test
    fun `given network error when getDevices then fails with cause`() = runTest {
        val cause = IOException("offline")
        coEvery { api.getDevices() } returns NetworkResult.NetworkError(cause)

        val result = repository.getDevices()

        assertEquals(cause, result.exceptionOrNull())
    }

    @Test
    fun `given success when pairDevice then approves the uppercased code with the name and no app version`() =
        runTest {
            val bodySlot = slot<PairDeviceRequestDto>()
            coEvery { api.pairDevice(capture(bodySlot)) } returns NetworkResult.Success(
                PairDeviceResponseDto(sessionId = "s1", status = "APPROVED", deviceId = "d9", deviceName = "Mostrador"),
            )

            val result = repository.pairDevice("  ab3cd  ", " Mostrador ")

            assertEquals(PairedDevice(deviceId = "d9", sessionId = "s1", name = "Mostrador"), result.getOrThrow())
            assertEquals("AB3CD", bodySlot.captured.pairingToken)
            assertEquals("APPROVE", bodySlot.captured.decision)
            assertEquals("Mostrador", bodySlot.captured.deviceName)
            assertEquals("ANDROID", bodySlot.captured.platform)
            assertNull(bodySlot.captured.appVersion)
        }

    @Test
    fun `a blank name is not sent`() = runTest {
        val bodySlot = slot<PairDeviceRequestDto>()
        coEvery { api.pairDevice(capture(bodySlot)) } returns
            NetworkResult.Success(PairDeviceResponseDto(sessionId = "s1", deviceId = "d9"))

        repository.pairDevice("AB3CD", "   ")

        assertNull(bodySlot.captured.deviceName)
    }

    @Test
    fun `an approval without device or session fails with Unknown`() = runTest {
        coEvery { api.pairDevice(any()) } returns NetworkResult.Success(PairDeviceResponseDto(status = "APPROVED"))

        val error = (repository.pairDevice("AB3CD", null).exceptionOrNull() as PairDeviceException).error

        assertInstanceOf(PairDeviceError.Unknown::class.java, error)
    }

    @Test
    fun `given http 400 when pairDevice then fails with InvalidCode`() = runTest {
        coEvery { api.pairDevice(any()) } returns NetworkResult.HttpError(400, "bad")

        val error = (repository.pairDevice("X", null).exceptionOrNull() as PairDeviceException).error

        assertEquals(PairDeviceError.InvalidCode, error)
    }

    @Test
    fun `each pairing errorCode maps to its own error`() = runTest {
        val cases = mapOf(
            "INVALID_PAIRING_TOKEN" to PairDeviceError.InvalidCode,
            "PAIRING_TOKEN_EXPIRED" to PairDeviceError.ExpiredCode,
            "PAIRING_TOKEN_ALREADY_USED" to PairDeviceError.AlreadyUsedCode,
            "PAIRING_TOKEN_CANCELLED" to PairDeviceError.CancelledCode,
        )
        cases.forEach { (errorCode, expected) ->
            coEvery { api.pairDevice(any()) } returns
                NetworkResult.HttpError(400, "Bad Request", ProblemDto(errorCode = errorCode))

            val error = (repository.pairDevice("AB3CD", null).exceptionOrNull() as PairDeviceException).error

            assertEquals(expected, error, errorCode)
        }
    }

    @Test
    fun `503 means the catalog could not be checked`() = runTest {
        coEvery { api.pairDevice(any()) } returns NetworkResult.HttpError(503, "Unavailable")

        val error = (repository.pairDevice("AB3CD", null).exceptionOrNull() as PairDeviceException).error

        assertEquals(PairDeviceError.CatalogCheckUnavailable, error)
    }

    @Test
    fun `given http 409 when pairDevice then fails with CatalogNotReady carrying the detail`() = runTest {
        coEvery { api.pairDevice(any()) } returns NetworkResult.HttpError(
            409,
            "Conflict",
            ProblemDto(errorCode = "CATALOG_THRESHOLD_NOT_MET", detail = "Necesitas al menos 5 productos"),
        )

        val error = (repository.pairDevice("X", null).exceptionOrNull() as PairDeviceException).error

        assertEquals(PairDeviceError.CatalogNotReady("Necesitas al menos 5 productos"), error)
    }

    @Test
    fun `given network error when pairDevice then fails with Network`() = runTest {
        coEvery { api.pairDevice(any()) } returns NetworkResult.NetworkError(IOException("offline"))

        val error = (repository.pairDevice("X", null).exceptionOrNull() as PairDeviceException).error

        assertEquals(PairDeviceError.Network, error)
    }

    // ---- Unlink ----

    @Test
    fun `unlinkDevice succeeds when the backend answers the device`() = runTest {
        coEvery { api.unlinkDevice("d1") } returns NetworkResult.Success(deviceDto().copy(status = "INACTIVE"))

        assertTrue(repository.unlinkDevice("d1").isSuccess)
        coVerify(exactly = 1) { api.unlinkDevice("d1") }
    }

    @Test
    fun `unlinkDevice fails on http and network errors`() = runTest {
        coEvery { api.unlinkDevice("d1") } returns NetworkResult.HttpError(403, "Forbidden")
        assertTrue(repository.unlinkDevice("d1").isFailure)

        coEvery { api.unlinkDevice("d1") } returns NetworkResult.NetworkError(IOException("offline"))
        assertTrue(repository.unlinkDevice("d1").isFailure)
    }

    @Test
    fun `unlinking a device that is gone fails with NotFound`() = runTest {
        coEvery { api.unlinkDevice("d1") } returns NetworkResult.HttpError(404, "Not Found")

        val error = (repository.unlinkDevice("d1").exceptionOrNull() as DeviceSettingsException).error

        assertEquals(DeviceSettingsError.NotFound, error)
    }

    @Test
    fun `updateSettings sends the single-field body and maps the answer`() = runTest {
        val body = slot<UpdateDeviceSettingsRequestDto>()
        coEvery { api.updateSettings("d1", capture(body)) } returns
            NetworkResult.Success(DeviceSettingsDto(kioskMode = false, exitPinSet = true))

        val result = repository.updateSettings("d1", DeviceSettingsChange.KioskMode(false))

        assertEquals(UpdateDeviceSettingsRequestDto(kioskMode = false), body.captured)
        assertEquals(false, result.getOrThrow().kioskMode)
    }

    @Test
    fun `settings errors map from the HTTP status`() = runTest {
        val cases = mapOf(
            404 to DeviceSettingsError.NotFound,
            422 to DeviceSettingsError.InvalidInput,
            400 to DeviceSettingsError.InvalidInput,
            503 to DeviceSettingsError.Network,
            500 to DeviceSettingsError.Unknown("HTTP 500"),
        )
        for ((code, expected) in cases) {
            coEvery { api.getSettings("d1") } returns NetworkResult.HttpError(code, "x")
            val error = repository.getSettings("d1").exceptionOrNull() as DeviceSettingsException
            assertEquals(expected, error.error)
        }
        coEvery { api.getSettings("d1") } returns NetworkResult.NetworkError(IOException("off"))
        assertEquals(
            DeviceSettingsError.Network,
            (repository.getSettings("d1").exceptionOrNull() as DeviceSettingsException).error,
        )
    }

    @Test
    fun `rejectPairing sends the code with REJECT and nothing else`() = runTest {
        val bodySlot = slot<PairDeviceRequestDto>()
        coEvery { api.pairDevice(capture(bodySlot)) } returns
            NetworkResult.Success(PairDeviceResponseDto(sessionId = "s1", status = "REJECTED"))

        assertTrue(repository.rejectPairing("ab3cd").isSuccess)
        assertEquals("AB3CD", bodySlot.captured.pairingToken)
        assertEquals("REJECT", bodySlot.captured.decision)
        assertNull(bodySlot.captured.deviceName)
    }

    @Test
    fun `getPairingStatus maps tokenExchanged`() = runTest {
        coEvery { api.getPairingStatus("s1") } returns
            NetworkResult.Success(PairingSessionStatusDto(status = "APPROVED", tokenExchanged = true))

        assertTrue(repository.getPairingStatus("s1").getOrThrow().tokenExchanged)
    }

    @Test
    fun `renameDevice returns the renamed device and maps 404 and 422`() = runTest {
        coEvery { api.renameDevice("d1", RenameDeviceRequestDto("Caja 2")) } returns
            NetworkResult.Success(deviceDto().copy(name = "Caja 2"))
        assertEquals("Caja 2", repository.renameDevice("d1", "Caja 2").getOrThrow().name)

        coEvery { api.renameDevice(any(), any()) } returns NetworkResult.HttpError(404, "Not Found")
        assertEquals(
            DeviceSettingsError.NotFound,
            (repository.renameDevice("d1", "x").exceptionOrNull() as DeviceSettingsException).error,
        )

        coEvery { api.renameDevice(any(), any()) } returns NetworkResult.HttpError(422, "Unprocessable")
        assertEquals(
            DeviceSettingsError.InvalidInput,
            (repository.renameDevice("d1", "x").exceptionOrNull() as DeviceSettingsException).error,
        )
    }
}
