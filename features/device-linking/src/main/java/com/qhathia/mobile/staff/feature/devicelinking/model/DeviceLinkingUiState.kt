package com.qhathia.mobile.staff.feature.devicelinking.model

import androidx.annotation.StringRes
import com.qhathia.mobile.staff.core.domain.model.Device
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Immutable snapshot of the linked-devices screen. [devices] is the store's paired terminals; the
 * pairing dialog owns its own transient sub-state ([isPairDialogVisible], [pairCode], [isPairing],
 * [pairErrorRes]).
 */
data class DeviceLinkingUiState(
    val devices: ImmutableList<Device> = persistentListOf(),
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val isPairDialogVisible: Boolean = false,
    val pairCode: String = "",
    val isPairing: Boolean = false,
    @param:StringRes val pairErrorRes: Int? = null,
) {
    /** True once the list has loaded at least one device. */
    val hasDevices: Boolean get() = devices.isNotEmpty()

    /** The submit button is enabled only with a non-blank code and no pairing in flight. */
    val canSubmitPair: Boolean get() = pairCode.isNotBlank() && !isPairing
}
