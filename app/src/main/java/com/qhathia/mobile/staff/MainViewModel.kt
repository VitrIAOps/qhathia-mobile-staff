package com.qhathia.mobile.staff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qhathia.mobile.staff.core.domain.usecase.ObserveBiometricEnabledUseCase
import com.qhathia.mobile.staff.core.domain.usecase.ObserveSessionUseCase
import com.qhathia.mobile.staff.core.domain.usecase.RegisterPushTokenUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

/**
 * Drives the start destination by observing whether a session exists, and registers this device's
 * push token whenever the session transitions to authenticated (covers login and authenticated app
 * start). Registration is fire-and-forget and fail-open.
 *
 * With fingerprint sign-in on, a stored session starts [AuthState.Locked] until the merchant passes
 * the device biometric, and locks again after [RELOCK_AFTER] in the background. A password login
 * counts as verified, so signing in never lands on the lock.
 *
 * It also holds the section a notification tap asked for until the shell can honour it. The state
 * lives here rather than in the Activity so it survives configuration changes and — more
 * importantly — the `Loading → Authenticated` transition: a push tapped while logged out (or locked)
 * is applied once the merchant is in. While the lock is due, the shell's "consumed" call keeps the
 * destination: a shell composed for a moment just before the lock is dropped with it, and the one
 * after the fingerprint must still find it.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    observeSession: ObserveSessionUseCase,
    private val observeBiometricEnabled: ObserveBiometricEnabledUseCase,
    private val registerPushToken: RegisterPushTokenUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val _pendingDestination = MutableStateFlow<String?>(null)

    /** The section to jump to on the next composition of the shell, or null when there is none. */
    val pendingDestination: StateFlow<String?> = _pendingDestination.asStateFlow()

    /** Whether the biometric gate was passed in this process (or skipped by a password login). */
    private val unlocked = MutableStateFlow(false)
    private var backgroundedAt: Instant? = null

    /**
     * Records a destination requested from outside the app. Takes the already-extracted value rather
     * than an `Intent` so this stays a plain unit-testable ViewModel. A null (an ordinary launcher
     * start) leaves any pending destination alone.
     */
    fun onDeepLink(destination: String?) {
        if (destination == null) return
        // The tap and the return to the foreground arrive in either order: lock now if it is due, so
        // the shell still on screen cannot take the destination with it.
        if (backgroundedLongEnough()) unlocked.value = false
        _pendingDestination.value = destination
    }

    /**
     * Called by the shell once it has navigated, so the jump does not repeat. Ignored while the lock
     * is due, because that shell is about to be replaced by the gate.
     */
    fun onPendingDestinationConsumed() {
        if (isLockDue()) return
        _pendingDestination.value = null
    }

    /** The merchant passed the biometric prompt. */
    fun onUnlocked() {
        unlocked.value = true
    }

    /** The whole app went to the background. */
    fun onAppBackgrounded() {
        backgroundedAt = clock.instant()
    }

    /** The app came back; a long enough absence locks it again. */
    fun onAppForegrounded() {
        if (backgroundedLongEnough()) unlocked.value = false
        backgroundedAt = null
    }

    /**
     * The fingerprint preference as last read, apart from [authState] so that state keeps starting at
     * [AuthState.Loading]. Null until the first read. Declared before [authState] so it subscribes
     * first.
     */
    private val biometricEnabledNow: StateFlow<Boolean?> =
        observeBiometricEnabled().stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Whether the biometric gate is showing or about to show. An unread preference counts as due. */
    private fun isLockDue(): Boolean {
        val biometric = biometricEnabledNow.value ?: return true
        if (!biometric) return false
        return !unlocked.value || backgroundedLongEnough()
    }

    private fun backgroundedLongEnough(): Boolean {
        val since = backgroundedAt ?: return false
        return Duration.between(since, clock.instant()) >= RELOCK_AFTER
    }

    private val isAuthenticated = observeSession()
        .map { session -> session != null }
        .distinctUntilChanged()
        .onEach { authenticated ->
            if (authenticated) {
                viewModelScope.launch { registerPushToken() }
            } else {
                // The next session starts with a password login, which already proves who is there.
                unlocked.value = true
            }
        }

    val authState: StateFlow<AuthState> =
        combine(isAuthenticated, observeBiometricEnabled(), unlocked) { authenticated, biometric, isUnlocked ->
            when {
                !authenticated -> AuthState.Unauthenticated
                biometric && !isUnlocked -> AuthState.Locked
                else -> AuthState.Authenticated
            }
        }
            .distinctUntilChanged()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = AuthState.Loading,
            )

    companion object {
        val RELOCK_AFTER: Duration = Duration.ofMinutes(5)
    }
}

/** Top-level authentication state used to choose what the root renders. */
enum class AuthState { Loading, Locked, Authenticated, Unauthenticated }
