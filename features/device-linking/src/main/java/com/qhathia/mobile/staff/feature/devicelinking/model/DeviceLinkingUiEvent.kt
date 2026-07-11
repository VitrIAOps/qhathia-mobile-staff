package com.qhathia.mobile.staff.feature.devicelinking.model

/** User intents from the linked-devices screen. */
sealed interface DeviceLinkingUiEvent {
    data object Load : DeviceLinkingUiEvent
    data object Retry : DeviceLinkingUiEvent
    data object OpenPairDialog : DeviceLinkingUiEvent
    data object DismissPairDialog : DeviceLinkingUiEvent
    data class PairCodeChanged(val code: String) : DeviceLinkingUiEvent
    data object SubmitPair : DeviceLinkingUiEvent
}
