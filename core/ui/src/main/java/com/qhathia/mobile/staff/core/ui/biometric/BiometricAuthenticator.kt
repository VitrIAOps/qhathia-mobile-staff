package com.qhathia.mobile.staff.core.ui.biometric

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** What the device can do for fingerprint sign-in right now. */
enum class BiometricAvailability {
    /** A biometric is enrolled and usable. */
    Available,

    /** The device has a sensor but nothing is enrolled in its settings. */
    NoneEnrolled,

    /** The device has no biometric sensor. */
    NoHardware,

    /** A sensor exists but cannot be used now (busy, needs a security update, unsupported). */
    Unavailable,
}

/** Outcome of one biometric prompt. */
sealed interface BiometricResult {
    data object Success : BiometricResult

    /** The user dismissed the prompt or chose the negative button. */
    data object Cancelled : BiometricResult

    /** The prompt ended without success for a system reason (lockout, sensor error). */
    data class Error(val message: String) : BiometricResult
}

private const val ALLOWED_AUTHENTICATORS = BIOMETRIC_STRONG or BIOMETRIC_WEAK

/**
 * Uses only the biometrics already enrolled in the device settings — the app never stores or sends
 * a credential. The prompt itself is a presence check; the session stays protected by the Keystore
 * cipher it was already encrypted with.
 */
@Stable
class BiometricAuthenticator internal constructor(private val context: Context) {

    fun availability(): BiometricAvailability =
        when (BiometricManager.from(context).canAuthenticate(ALLOWED_AUTHENTICATORS)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.Available
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NoneEnrolled
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricAvailability.NoHardware
            else -> BiometricAvailability.Unavailable
        }

    /**
     * Shows the system prompt and suspends until it ends. Failed attempts (an unrecognised finger)
     * keep the prompt open; only success, dismissal or a terminal error resume.
     */
    suspend fun authenticate(
        title: String,
        subtitle: String?,
        negativeText: String,
    ): BiometricResult {
        val activity = context.findFragmentActivity()
            ?: return BiometricResult.Error("No FragmentActivity to host the prompt")
        return suspendCancellableCoroutine { continuation ->
            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    if (continuation.isActive) continuation.resume(BiometricResult.Success)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (!continuation.isActive) return
                    val outcome = when (errorCode) {
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                        BiometricPrompt.ERROR_CANCELED,
                        -> BiometricResult.Cancelled
                        else -> BiometricResult.Error(errString.toString())
                    }
                    continuation.resume(outcome)
                }
            }
            val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback)
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .apply { if (subtitle != null) setSubtitle(subtitle) }
                .setNegativeButtonText(negativeText)
                .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
                .build()
            prompt.authenticate(info)
            continuation.invokeOnCancellation { prompt.cancelAuthentication() }
        }
    }

    /**
     * Opens the system screen where the user enrolls a fingerprint (API 30+), falling back to the
     * security settings and then to the settings root on devices that lack those screens.
     */
    fun openEnrollmentSettings() {
        val candidates = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                add(
                    Intent(Settings.ACTION_BIOMETRIC_ENROLL)
                        .putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, ALLOWED_AUTHENTICATORS),
                )
            }
            add(Intent(Settings.ACTION_SECURITY_SETTINGS))
            add(Intent(Settings.ACTION_SETTINGS))
        }
        for (intent in candidates) {
            val started = runCatching {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            if (started.isSuccess) return
            if (started.exceptionOrNull() !is ActivityNotFoundException) return
        }
    }
}

/** Remembers a [BiometricAuthenticator] bound to the hosting activity. */
@Composable
fun rememberBiometricAuthenticator(): BiometricAuthenticator {
    val context = LocalContext.current
    return remember(context) { BiometricAuthenticator(context) }
}

private tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}
