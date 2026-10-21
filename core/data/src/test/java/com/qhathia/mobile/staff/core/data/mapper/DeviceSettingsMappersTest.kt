package com.qhathia.mobile.staff.core.data.mapper

import com.qhathia.mobile.staff.core.domain.model.BrightnessMode
import com.qhathia.mobile.staff.core.domain.model.DeviceSettingsChange
import com.qhathia.mobile.staff.core.network.dto.DeviceSettingsDto
import com.qhathia.mobile.staff.core.network.dto.UpdateDeviceSettingsRequestDto
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DeviceSettingsMappersTest {

    @Test
    fun `maps the settings and degrades an unknown brightness`() {
        val dto = DeviceSettingsDto(brightnessMode = "HIGH", kioskMode = false, exitPinSet = true)
        val settings = dto.toDomain()
        assertEquals(BrightnessMode.HIGH, settings.brightnessMode)
        assertEquals(false, settings.kioskMode)
        assertEquals(true, settings.exitPinSet)

        assertEquals(BrightnessMode.UNKNOWN, DeviceSettingsDto(brightnessMode = "ULTRA").toDomain().brightnessMode)
        assertEquals(BrightnessMode.UNKNOWN, DeviceSettingsDto(brightnessMode = "UNKNOWN").toDomain().brightnessMode)
    }

    @Test
    fun `each change encodes only its own field`() {
        val json = Json
        fun body(change: DeviceSettingsChange) =
            json.encodeToString(UpdateDeviceSettingsRequestDto.serializer(), change.toRequestDto())

        assertEquals("""{"kioskMode":false}""", body(DeviceSettingsChange.KioskMode(false)))
        assertEquals("""{"brightnessMode":"HIGH"}""", body(DeviceSettingsChange.Brightness(BrightnessMode.HIGH)))
        assertEquals("""{"exitPin":"1234"}""", body(DeviceSettingsChange.SetExitPin("1234")))
        assertEquals("""{"removeExitPin":true}""", body(DeviceSettingsChange.RemoveExitPin))
        assertEquals("""{"idleShowcaseEnabled":true}""", body(DeviceSettingsChange.IdleShowcase(true)))
        assertEquals("""{"voiceAssistantEnabled":false}""", body(DeviceSettingsChange.VoiceAssistant(false)))
        assertEquals("""{"autoSyncEnabled":false}""", body(DeviceSettingsChange.AutoSync(false)))
    }
}
