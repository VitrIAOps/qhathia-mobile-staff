package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.model.DeviceSettingsChange
import com.qhathia.mobile.staff.core.domain.model.DeviceSettingsError
import com.qhathia.mobile.staff.core.domain.model.DeviceSettingsException
import com.qhathia.mobile.staff.core.domain.model.isValidExitPin
import com.qhathia.mobile.staff.core.testing.FakeDeviceRepository
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DeviceSettingsUseCasesTest {

    private val repository = FakeDeviceRepository()
    private val update = UpdateDeviceSettingsUseCase(repository)
    private val get = GetDeviceSettingsUseCase(repository)

    @Test
    fun `a valid PIN is sent`() = runTest {
        val result = update("d1", DeviceSettingsChange.SetExitPin("1234"))

        assertTrue(result.getOrThrow().exitPinSet)
        assertEquals(listOf("d1" to DeviceSettingsChange.SetExitPin("1234")), repository.settingsChanges)
    }

    @Test
    fun `a malformed PIN is rejected without a request`() = runTest {
        for (pin in listOf("123", "1234567", "12a4", "")) {
            val error = update("d1", DeviceSettingsChange.SetExitPin(pin)).exceptionOrNull()
            assertEquals(DeviceSettingsError.InvalidInput, (error as DeviceSettingsException).error)
        }
        assertTrue(repository.settingsChanges.isEmpty())
    }

    @Test
    fun `an unclassified failure becomes Unknown`() = runTest {
        repository.settingsResult = Result.failure(IllegalStateException("boom"))

        val error = get("d1").exceptionOrNull()

        assertEquals(DeviceSettingsError.Unknown("boom"), (error as DeviceSettingsException).error)
    }

    @Test
    fun `exit PIN rule is 4 to 6 digits`() {
        assertTrue(isValidExitPin("1234"))
        assertTrue(isValidExitPin("123456"))
        assertFalse(isValidExitPin("123"))
        assertFalse(isValidExitPin("1234567"))
        assertFalse(isValidExitPin("12 4"))
    }
}
