package com.qhathia.mobile.staff.feature.auth.model

/**
 * Snapshot of the biometric unlock screen.
 *
 * @property userFirstName greets the merchant whose session is locked; `null` before it is known.
 * @property isBiometricGone the device no longer has an enrolled biometric, so the only way in is
 *   the password — the screen explains that and offers "Continuar".
 * @property lastError the system's message after a prompt ended with an error (e.g. lockout).
 */
data class BiometricUnlockUiState(
    val userFirstName: String? = null,
    val isBiometricGone: Boolean = false,
    val lastError: String? = null,
    val isSigningOut: Boolean = false,
)

sealed interface BiometricUnlockUiEvent {
    data object AuthenticationSucceeded : BiometricUnlockUiEvent

    /** The prompt was dismissed; the screen stays so the merchant can retry. */
    data object AuthenticationCancelled : BiometricUnlockUiEvent

    data class AuthenticationFailed(val message: String) : BiometricUnlockUiEvent

    /** No usable enrolled biometric remains on the device. */
    data object BiometricGone : BiometricUnlockUiEvent

    /** "Ingresar con contraseña" / "Continuar": sign out and go through login. */
    data object UsePassword : BiometricUnlockUiEvent
}

sealed interface BiometricUnlockUiEffect {
    data object Unlocked : BiometricUnlockUiEffect
}
