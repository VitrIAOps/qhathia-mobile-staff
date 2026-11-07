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

    /**
     * The tablet was unlinked from its settings screen; its card leaves the list. [alreadyGone]
     * means it had been unlinked elsewhere (the unlink answered 404).
     */
    data class DeviceUnlinked(val deviceId: String, val alreadyGone: Boolean = false) : DeviceLinkingUiEvent

    data object OpenPairDialog : DeviceLinkingUiEvent
    data object DismissPairDialog : DeviceLinkingUiEvent
    data class PairCodeChanged(val code: String) : DeviceLinkingUiEvent
    data class PairNameChanged(val name: String) : DeviceLinkingUiEvent
    data object SubmitPair : DeviceLinkingUiEvent

    /** Declines the typed code; the tablet shows that the store rejected it. */
    data object RejectPair : DeviceLinkingUiEvent

    /** The tablet did not answer: wait another round. */
    data object KeepWaiting : DeviceLinkingUiEvent

    /** The tablet did not answer: unlink the device the approval created. */
    data object UndoPair : DeviceLinkingUiEvent
}

/** One-shot outcomes of Dispositivos. */
sealed interface DeviceLinkingUiEffect {
    data class ShowCommandError(val type: DeviceCommandType) : DeviceLinkingUiEffect
    data class ShowUnlinked(val alreadyGone: Boolean = false) : DeviceLinkingUiEffect

    /** The tablet exchanged the code; [name] is what the backend called it. */
    data class ShowPaired(val name: String?) : DeviceLinkingUiEffect
    data object ShowPairRejected : DeviceLinkingUiEffect
    data object ShowPairUndone : DeviceLinkingUiEffect
}

/** Kept for call sites that only need the command type name. */
internal val DeviceCommandType.isRestart: Boolean get() = this == DeviceCommandType.RESTART_APP
