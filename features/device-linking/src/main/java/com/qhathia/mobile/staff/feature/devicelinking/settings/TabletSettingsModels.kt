package com.qhathia.mobile.staff.feature.devicelinking.settings

import androidx.annotation.StringRes
import com.qhathia.mobile.staff.core.domain.model.Device
import com.qhathia.mobile.staff.core.domain.model.DeviceSettings
import com.qhathia.mobile.staff.core.domain.model.DeviceSettingsChange
import com.qhathia.mobile.staff.core.domain.model.isValidExitPin

/** How far loading the screen got. */
enum class TabletSettingsLoad { LOADING, LOADED, ERROR, NOT_FOUND }

/** The controls that save on change; each can have one request in flight. */
enum class SettingKey { IDLE_SHOWCASE, VOICE_ASSISTANT, BRIGHTNESS, KIOSK_MODE, AUTO_SYNC }

/**
 * Immutable snapshot of Ajustes de la tablet. [settings] is what the screen shows — a change in
 * flight is already applied — and [saving] the controls waiting for the server. [device] feeds the
 * Información group and is null when the device list could not be read. [pinDialog] is non-null
 * while the exit-PIN dialog is open. [isUnlinkConfirmVisible] is the "¿Desvincular?" dialog and
 * [isUnlinking] the unlink request in flight. [renameDialog] is non-null while "Nombre" is being
 * edited.
 */
data class TabletSettingsUiState(
    val load: TabletSettingsLoad = TabletSettingsLoad.LOADING,
    val settings: DeviceSettings? = null,
    val device: Device? = null,
    val saving: Set<SettingKey> = emptySet(),
    val pinDialog: ExitPinDialogState? = null,
    val isUnlinkConfirmVisible: Boolean = false,
    val isUnlinking: Boolean = false,
    val renameDialog: RenameDialogState? = null,
) {
    fun isSaving(key: SettingKey): Boolean = key in saving

    /** Kiosk mode with no PIN: anyone at the stand can leave it, which the kiosk row warns about. */
    val isKioskWithoutPin: Boolean
        get() = settings?.let { it.kioskMode && !it.exitPinSet } == true
}

/**
 * The exit-PIN dialog. The PIN is typed twice and never shown back; [isConfirmingRemoval] is the
 * "¿Quitar el PIN?" step.
 */
data class ExitPinDialogState(
    val pin: String = "",
    val confirmation: String = "",
    val isSaving: Boolean = false,
    @param:StringRes val errorRes: Int? = null,
    val isConfirmingRemoval: Boolean = false,
) {
    /** Both fields are full but different — shown as an error under the second one. */
    val isMismatch: Boolean
        get() = confirmation.length >= pin.length && confirmation.isNotEmpty() && pin != confirmation

    val canSave: Boolean
        get() = !isSaving && isValidExitPin(pin) && pin == confirmation
}

/**
 * The rename dialog: [value] is what is typed, [current] the name it started from. Saving needs a
 * non-blank name that differs from the current one.
 */
data class RenameDialogState(
    val value: String,
    val current: String,
    val isSaving: Boolean = false,
    @param:StringRes val errorRes: Int? = null,
) {
    val canSave: Boolean
        get() = !isSaving && value.isNotBlank() && value.trim() != current.trim()
}

sealed interface TabletSettingsUiEvent {
    data object Retry : TabletSettingsUiEvent

    /** A switch or the brightness changed; saved straight away. */
    data class SettingChanged(val change: DeviceSettingsChange) : TabletSettingsUiEvent

    data object OpenPinDialog : TabletSettingsUiEvent
    data object DismissPinDialog : TabletSettingsUiEvent
    data class PinChanged(val value: String) : TabletSettingsUiEvent
    data class PinConfirmationChanged(val value: String) : TabletSettingsUiEvent
    data object SavePin : TabletSettingsUiEvent
    data object RemovePinRequested : TabletSettingsUiEvent
    data object RemovePinConfirmed : TabletSettingsUiEvent
    data object RemovePinCancelled : TabletSettingsUiEvent

    data object UnlinkRequested : TabletSettingsUiEvent
    data object UnlinkDismissed : TabletSettingsUiEvent
    data object UnlinkConfirmed : TabletSettingsUiEvent

    /** "Nombre" in Información; opens the rename dialog. */
    data object RenameClicked : TabletSettingsUiEvent
    data class RenameChanged(val value: String) : TabletSettingsUiEvent
    data object RenameConfirmed : TabletSettingsUiEvent
    data object RenameDismissed : TabletSettingsUiEvent
}

sealed interface TabletSettingsUiEffect {
    data class ShowMessage(@param:StringRes val messageRes: Int) : TabletSettingsUiEffect

    /** The tablet left the store; the screen closes and Dispositivos drops its card. */
    data class Unlinked(val deviceId: String) : TabletSettingsUiEffect
}

/** Which control a change belongs to; PIN changes go through the dialog instead. */
internal fun DeviceSettingsChange.key(): SettingKey? = when (this) {
    is DeviceSettingsChange.IdleShowcase -> SettingKey.IDLE_SHOWCASE
    is DeviceSettingsChange.VoiceAssistant -> SettingKey.VOICE_ASSISTANT
    is DeviceSettingsChange.Brightness -> SettingKey.BRIGHTNESS
    is DeviceSettingsChange.KioskMode -> SettingKey.KIOSK_MODE
    is DeviceSettingsChange.AutoSync -> SettingKey.AUTO_SYNC
    is DeviceSettingsChange.SetExitPin, DeviceSettingsChange.RemoveExitPin -> null
}

/** The settings with [change] applied, as the screen shows it while the request is in flight. */
internal fun DeviceSettings.applied(change: DeviceSettingsChange): DeviceSettings = when (change) {
    is DeviceSettingsChange.IdleShowcase -> copy(idleShowcaseEnabled = change.enabled)
    is DeviceSettingsChange.VoiceAssistant -> copy(voiceAssistantEnabled = change.enabled)
    is DeviceSettingsChange.Brightness -> copy(brightnessMode = change.mode)
    is DeviceSettingsChange.KioskMode -> copy(kioskMode = change.enabled)
    is DeviceSettingsChange.AutoSync -> copy(autoSyncEnabled = change.enabled)
    is DeviceSettingsChange.SetExitPin -> copy(exitPinSet = true)
    DeviceSettingsChange.RemoveExitPin -> copy(exitPinSet = false)
}

/** These settings with the value of [key] taken from [source]. */
internal fun DeviceSettings.withValueOf(key: SettingKey, source: DeviceSettings): DeviceSettings = when (key) {
    SettingKey.IDLE_SHOWCASE -> copy(idleShowcaseEnabled = source.idleShowcaseEnabled)
    SettingKey.VOICE_ASSISTANT -> copy(voiceAssistantEnabled = source.voiceAssistantEnabled)
    SettingKey.BRIGHTNESS -> copy(brightnessMode = source.brightnessMode)
    SettingKey.KIOSK_MODE -> copy(kioskMode = source.kioskMode)
    SettingKey.AUTO_SYNC -> copy(autoSyncEnabled = source.autoSyncEnabled)
}
