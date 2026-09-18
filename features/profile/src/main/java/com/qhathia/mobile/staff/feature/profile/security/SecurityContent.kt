package com.qhathia.mobile.staff.feature.profile.security

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.staff.core.domain.model.ActiveSession
import com.qhathia.mobile.staff.core.ui.component.settings.QhathiaGradientTopBar
import com.qhathia.mobile.staff.core.ui.component.settings.QhathiaNavigationRow
import com.qhathia.mobile.staff.core.ui.component.settings.QhathiaSettingsSection
import com.qhathia.mobile.staff.core.ui.component.settings.QhathiaSwitchRow
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme
import com.qhathia.mobile.staff.feature.profile.R
import com.qhathia.mobile.staff.feature.profile.common.ErrorPane
import com.qhathia.mobile.staff.feature.profile.common.LoadState
import com.qhathia.mobile.staff.feature.profile.common.LoadingPane
import com.qhathia.mobile.staff.core.ui.component.time.label
import com.qhathia.mobile.staff.core.ui.component.time.relativeTimeBetween
import java.time.Instant

/** Stateless Seguridad: password and fingerprint card, active sessions, and their dialogs. */
@Composable
fun SecurityContent(
    uiState: SecurityUiState,
    onEvent: (SecurityUiEvent) -> Unit,
    onBiometricToggle: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        QhathiaGradientTopBar(title = stringResource(R.string.security_title), onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Dimens.SpaceMd),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceLg),
        ) {
            QhathiaSettingsSection(title = null) {
                QhathiaNavigationRow(
                    title = stringResource(R.string.security_change_password),
                    subtitle = passwordSubtitle(uiState),
                    onClick = { onEvent(SecurityUiEvent.ChangePasswordClicked) },
                )
                QhathiaSwitchRow(
                    title = stringResource(R.string.security_biometric),
                    subtitle = stringResource(R.string.security_biometric_subtitle),
                    checked = uiState.biometricEnabled,
                    onCheckedChange = onBiometricToggle,
                    showDivider = false,
                )
            }
            QhathiaSettingsSection(title = stringResource(R.string.security_sessions_title)) {
                SessionsBody(uiState = uiState, onEvent = onEvent)
            }
            if (uiState.canRevokeOthers) {
                TextButton(
                    onClick = { onEvent(SecurityUiEvent.RevokeOthersRequested) },
                    enabled = !uiState.isRevokingOthers,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.security_revoke_others),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
    SecurityDialogs(uiState = uiState, onEvent = onEvent)
}

@Composable
private fun passwordSubtitle(uiState: SecurityUiState): String? =
    when (val updated = uiState.passwordUpdatedAt) {
        is LoadState.Loaded -> updated.value
            ?.let { stringResource(R.string.security_password_updated, relativeTimeBetween(it, uiState.now).label()) }
            ?: stringResource(R.string.security_password_never)
        LoadState.Loading, LoadState.Error -> null
    }

@Composable
private fun SessionsBody(uiState: SecurityUiState, onEvent: (SecurityUiEvent) -> Unit) {
    when (val sessions = uiState.sessions) {
        LoadState.Loading -> LoadingPane(modifier = Modifier.padding(vertical = Dimens.SpaceSm))
        LoadState.Error -> ErrorPane(
            message = stringResource(R.string.security_sessions_error),
            onRetry = { onEvent(SecurityUiEvent.RetrySessions) },
        )
        is LoadState.Loaded -> sessions.value.forEachIndexed { index, session ->
            SessionRow(
                session = session,
                now = uiState.now,
                isRevoking = session.sessionId in uiState.revokingIds,
                onClose = { onEvent(SecurityUiEvent.RevokeRequested(session)) },
                showDivider = index < sessions.value.lastIndex,
            )
        }
    }
}

@Composable
private fun SessionRow(
    session: ActiveSession,
    now: Instant,
    isRevoking: Boolean,
    onClose: () -> Unit,
    showDivider: Boolean,
) {
    val label = session.deviceLabel.ifBlank { stringResource(R.string.security_session_unknown_device) }
    QhathiaNavigationRow(
        title = if (session.isCurrent) stringResource(R.string.security_session_current, label) else label,
        subtitle = session.lastUsedAt?.let {
            stringResource(R.string.security_session_last_used, relativeTimeBetween(it, now).label())
        },
        icon = Icons.Outlined.Devices,
        onClick = null,
        showDivider = showDivider,
        trailing = {
            when {
                session.isCurrent -> Unit
                isRevoking -> CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                else -> {
                    val closeCd = stringResource(R.string.security_session_close_cd, label)
                    TextButton(onClick = onClose, modifier = Modifier.semantics { contentDescription = closeCd }) {
                        Text(
                            text = stringResource(R.string.security_session_close),
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun SecurityDialogs(uiState: SecurityUiState, onEvent: (SecurityUiEvent) -> Unit) {
    uiState.pendingRevoke?.let { session ->
        val label = session.deviceLabel.ifBlank { stringResource(R.string.security_session_unknown_device) }
        ConfirmDialog(
            title = stringResource(R.string.security_revoke_title),
            message = stringResource(R.string.security_revoke_message, label),
            confirm = stringResource(R.string.security_revoke_confirm),
            onConfirm = { onEvent(SecurityUiEvent.RevokeConfirmed) },
            onDismiss = { onEvent(SecurityUiEvent.RevokeDismissed) },
        )
    }
    if (uiState.isRevokeOthersConfirmVisible) {
        ConfirmDialog(
            title = stringResource(R.string.security_revoke_others),
            message = stringResource(R.string.security_revoke_others_message),
            confirm = stringResource(R.string.security_revoke_confirm),
            onConfirm = { onEvent(SecurityUiEvent.RevokeOthersConfirmed) },
            onDismiss = { onEvent(SecurityUiEvent.RevokeOthersDismissed) },
        )
    }
    when (uiState.biometricHint) {
        BiometricHint.NONE_ENROLLED -> ConfirmDialog(
            title = stringResource(R.string.biometric_not_enrolled_title),
            message = stringResource(R.string.biometric_not_enrolled_message),
            confirm = stringResource(R.string.biometric_open_settings),
            dismiss = stringResource(R.string.biometric_not_now),
            destructive = false,
            onConfirm = { onEvent(SecurityUiEvent.OpenEnrollmentSettings) },
            onDismiss = { onEvent(SecurityUiEvent.BiometricHintDismissed) },
        )
        BiometricHint.NOT_AVAILABLE -> AlertDialog(
            onDismissRequest = { onEvent(SecurityUiEvent.BiometricHintDismissed) },
            text = { Text(stringResource(R.string.biometric_not_available_message)) },
            confirmButton = {
                TextButton(onClick = { onEvent(SecurityUiEvent.BiometricHintDismissed) }) {
                    Text(stringResource(R.string.biometric_ok))
                }
            },
        )
        null -> Unit
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismiss: String = stringResource(R.string.profile_cancel),
    destructive: Boolean = true,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = confirm,
                    color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(dismiss) } },
    )
}

private val PreviewNow: Instant = Instant.parse("2026-03-10T15:00:00Z")

private val PreviewState = SecurityUiState(
    passwordUpdatedAt = LoadState.Loaded(Instant.parse("2025-11-08T15:00:00Z")),
    sessions = LoadState.Loaded(
        listOf(
            ActiveSession("s1", "iPhone 13", Instant.parse("2026-03-10T14:58:00Z"), isCurrent = true),
            ActiveSession("s2", "Samsung 15", Instant.parse("2025-11-10T15:00:00Z"), isCurrent = false),
        ),
    ),
    biometricEnabled = true,
    now = PreviewNow,
)

@Preview(name = "Seguridad — light", showBackground = true, heightDp = 800)
@Preview(name = "Seguridad — dark", showBackground = true, heightDp = 800, uiMode = 0x20)
@Composable
private fun SecurityContentPreview() {
    QhathiaTheme {
        SecurityContent(uiState = PreviewState, onEvent = {}, onBiometricToggle = {}, onBack = {})
    }
}

@Preview(name = "Seguridad, only this device — light", showBackground = true)
@Composable
private fun SecurityAlonePreview() {
    QhathiaTheme {
        SecurityContent(
            uiState = PreviewState.copy(
                passwordUpdatedAt = LoadState.Loaded(null),
                sessions = LoadState.Loaded(listOf(ActiveSession("s1", "iPhone 13", PreviewNow, true))),
                biometricEnabled = false,
            ),
            onEvent = {},
            onBiometricToggle = {},
            onBack = {},
        )
    }
}

@Preview(name = "Seguridad, sessions error — light", showBackground = true)
@Composable
private fun SecurityErrorPreview() {
    QhathiaTheme {
        SecurityContent(
            uiState = PreviewState.copy(sessions = LoadState.Error),
            onEvent = {},
            onBiometricToggle = {},
            onBack = {},
        )
    }
}
