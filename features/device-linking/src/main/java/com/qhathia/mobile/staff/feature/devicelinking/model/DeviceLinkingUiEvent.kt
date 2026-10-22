package com.qhathia.mobile.staff.feature.devicelinking.model

import com.qhathia.mobile.staff.core.domain.model.DeviceCommandType

/** User intents from Dispositivos. */
sealed interface DeviceLinkingUiEvent {
    data object Load : DeviceLinkingUiEvent
    data object Retry : DeviceLinkingUiEvent

    /** The screen became visible / stopped being visible; command polling follows it. */
    data object ScreenStarted : DeviceLinkingUiEvent
    data object ScreenStopped : DeviceLinkingUiEvent

    data class RefreshCatalog(val deviceId: String) : DeviceLinkingUiEvent
    data class RestartRequested(val deviceId: String) : DeviceLinkingUiEvent
    data object RestartDismissed : DeviceLinkingUiEvent
    data object RestartConfirmed : DeviceLinkingUiEvent

    /** The tablet was unlinked from its settings screen; its card leaves the list. */
    data class DeviceUnlinked(val deviceId: String) : DeviceLinkingUiEvent

    data object OpenPairDialog : DeviceLinkingUiEvent
    data object DismissPairDialog : DeviceLinkingUiEvent
    data class PairCodeChanged(val code: String) : DeviceLinkingUiEvent
    data object SubmitPair : DeviceLinkingUiEvent
}

/** One-shot outcomes of Dispositivos. */
sealed interface DeviceLinkingUiEffect {
    data class ShowCommandError(val type: DeviceCommandType) : DeviceLinkingUiEffect
    data object ShowUnlinked : DeviceLinkingUiEffect
}

/** Kept for call sites that only need the command type name. */
internal val DeviceCommandType.isRestart: Boolean get() = this == DeviceCommandType.RESTART_APP
