package com.qhathia.mobile.staff.feature.profile.security

import androidx.annotation.StringRes
import com.qhathia.mobile.staff.core.domain.model.ActiveSession
import com.qhathia.mobile.staff.feature.profile.common.LoadState
import com.qhathia.mobile.staff.feature.profile.common.valueOrNull
import java.time.Instant

/**
 * What the device offers for fingerprint sign-in, as read by the screen. Kept in the feature so the
 * ViewModel does not depend on the biometric helper's types.
 */
enum class DeviceBiometricStatus { AVAILABLE, NONE_ENROLLED, NO_HARDWARE, UNAVAILABLE }

/** The dialog explaining why fingerprint sign-in could not be enabled. */
enum class BiometricHint { NONE_ENROLLED, NOT_AVAILABLE }

/**
 * Snapshot of Seguridad.
 *
 * @property passwordUpdatedAt `Loaded(null)` means the password was never changed.
 * @property pendingRevoke the session awaiting confirmation to close.
 * @property revokingIds sessions whose close request is in flight.
 */
data class SecurityUiState(
    val passwordUpdatedAt: LoadState<Instant?> = LoadState.Loading,
    val sessions: LoadState<List<ActiveSession>> = LoadState.Loading,
    val biometricEnabled: Boolean = false,
    val biometricHint: BiometricHint? = null,
    val pendingRevoke: ActiveSession? = null,
    val isRevokeOthersConfirmVisible: Boolean = false,
    val revokingIds: Set<String> = emptySet(),
    val isRevokingOthers: Boolean = false,
    val now: Instant = Instant.EPOCH,
) {
    /** "Cerrar sesión en otros equipos" only makes sense when another session is listed. */
    val canRevokeOthers: Boolean
        get() = sessions.valueOrNull?.any { !it.isCurrent } == true
}

sealed interface SecurityUiEvent {
    /** The screen resumed; [biometric] is the device's current capability. */
    data class Resumed(val biometric: DeviceBiometricStatus) : SecurityUiEvent

    data object RetrySessions : SecurityUiEvent
    data object ChangePasswordClicked : SecurityUiEvent

    /** Cambiar contraseña finished successfully and returned here. */
    data object PasswordChanged : SecurityUiEvent

    data class BiometricToggled(val enabled: Boolean, val biometric: DeviceBiometricStatus) : SecurityUiEvent
    data class BiometricPromptFinished(val succeeded: Boolean) : SecurityUiEvent
    data object BiometricHintDismissed : SecurityUiEvent
    data object OpenEnrollmentSettings : SecurityUiEvent

    data class RevokeRequested(val session: ActiveSession) : SecurityUiEvent
    data object RevokeDismissed : SecurityUiEvent
    data object RevokeConfirmed : SecurityUiEvent
    data object RevokeOthersRequested : SecurityUiEvent
    data object RevokeOthersDismissed : SecurityUiEvent
    data object RevokeOthersConfirmed : SecurityUiEvent
}

sealed interface SecurityUiEffect {
    data object PromptBiometric : SecurityUiEffect
    data object OpenEnrollmentSettings : SecurityUiEffect
    data object NavigateToChangePassword : SecurityUiEffect
    data class ShowMessage(@param:StringRes val messageRes: Int) : SecurityUiEffect
}
