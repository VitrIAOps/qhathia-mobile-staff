package com.qhathia.mobile.staff.core.data.repository

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
    fun `given success when pairDevice then approves the trimmed code with android metadata`() =
        runTest {
            val bodySlot = slot<PairDeviceRequestDto>()
            coEvery { api.pairDevice(capture(bodySlot)) } returns
                NetworkResult.Success(PairDeviceResponseDto(status = "APPROVED"))

            val result = repository.pairDevice("  AB3CD  ")

            assertTrue(result.isSuccess)
            assertEquals("AB3CD", bodySlot.captured.pairingToken)
            assertEquals("APPROVE", bodySlot.captured.decision)
            assertEquals("ANDROID", bodySlot.captured.platform)
        }

    @Test
    fun `given http 400 when pairDevice then fails with InvalidCode`() = runTest {
        coEvery { api.pairDevice(any()) } returns NetworkResult.HttpError(400, "bad")

        val error = (repository.pairDevice("X").exceptionOrNull() as PairDeviceException).error

        assertEquals(PairDeviceError.InvalidCode, error)
    }

    @Test
    fun `given http 409 when pairDevice then fails with CatalogNotReady`() = runTest {
        coEvery { api.pairDevice(any()) } returns NetworkResult.HttpError(409, "threshold")

        val error = (repository.pairDevice("X").exceptionOrNull() as PairDeviceException).error

        assertEquals(PairDeviceError.CatalogNotReady, error)
    }

    @Test
    fun `given network error when pairDevice then fails with Network`() = runTest {
        coEvery { api.pairDevice(any()) } returns NetworkResult.NetworkError(IOException("offline"))

        val error = (repository.pairDevice("X").exceptionOrNull() as PairDeviceException).error

        assertEquals(PairDeviceError.Network, error)
    }

    // ---- Settings ----

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
}
