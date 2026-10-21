package com.qhathia.mobile.staff.core.domain.model

/**
 * A tablet's remote settings, as the owner manages them from the phone. The exit PIN is write-only:
 * only whether one is set ([exitPinSet]) is ever known here.
 */
data class DeviceSettings(
    val idleShowcaseEnabled: Boolean,
    val voiceAssistantEnabled: Boolean,
    val brightnessMode: BrightnessMode,
    val kioskMode: Boolean,
    val exitPinSet: Boolean,
    val autoSyncEnabled: Boolean,
)

/**
 * The owner declares an intention and the tablet resolves the actual level with its light sensor.
 * [UNKNOWN] keeps an unexpected server value from breaking the screen.
 */
enum class BrightnessMode { AUTO, LOW, MEDIUM, HIGH, UNKNOWN }

/**
 * One change to a tablet's settings. Each maps to a partial update carrying only its own field, so
 * two quick changes never overwrite each other.
 */
sealed interface DeviceSettingsChange {
    data class IdleShowcase(val enabled: Boolean) : DeviceSettingsChange
    data class VoiceAssistant(val enabled: Boolean) : DeviceSettingsChange
    data class Brightness(val mode: BrightnessMode) : DeviceSettingsChange
    data class KioskMode(val enabled: Boolean) : DeviceSettingsChange
    data class AutoSync(val enabled: Boolean) : DeviceSettingsChange

    /** Sets or replaces the exit PIN: 4 to 6 digits. */
    data class SetExitPin(val pin: String) : DeviceSettingsChange

    /** Removes the exit PIN; anyone can then leave kiosk mode on that tablet. */
    data object RemoveExitPin : DeviceSettingsChange
}

/** Why reading or changing a tablet's settings failed. */
sealed interface DeviceSettingsError {
    /** The tablet is not linked to this store (HTTP 404). */
    data object NotFound : DeviceSettingsError

    /** The server rejected the value (HTTP 400/422), e.g. a malformed PIN. */
    data object InvalidInput : DeviceSettingsError

    data object Network : DeviceSettingsError

    data class Unknown(val message: String?) : DeviceSettingsError
}

class DeviceSettingsException(val error: DeviceSettingsError) : Exception()

/** An exit PIN is 4 to 6 digits. */
fun isValidExitPin(pin: String): Boolean = pin.length in 4..6 && pin.all { it in '0'..'9' }
