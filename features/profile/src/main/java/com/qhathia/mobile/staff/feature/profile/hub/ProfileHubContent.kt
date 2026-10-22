package com.qhathia.mobile.staff.feature.profile.hub

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.TabletAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.qhathia.mobile.staff.core.domain.model.UserRole
import com.qhathia.mobile.staff.core.ui.component.settings.QhathiaNavigationRow
import com.qhathia.mobile.staff.core.ui.component.settings.QhathiaSettingsSection
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme
import com.qhathia.mobile.staff.feature.profile.R
import com.qhathia.mobile.staff.feature.profile.common.LoadState

/** Stateless Mi perfil: header with identity, subscription card, menu and sign out. */
@Composable
fun ProfileHubContent(
    uiState: ProfileHubUiState,
    onEvent: (ProfileHubUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = Dimens.SpaceLg),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
    ) {
        item(key = "header") {
            ProfileHeader(uiState = uiState, onEvent = onEvent)
        }
        if (uiState.canManageStore) {
            item(key = "subscription") {
                SubscriptionSummaryCard(
                    state = uiState.subscription,
                    now = uiState.now,
                    onOpen = { onEvent(ProfileHubUiEvent.Open(ProfileDestination.SUBSCRIPTION)) },
                    modifier = Modifier.padding(horizontal = Dimens.SpaceMd),
                )
            }
        }
        item(key = "menu") {
            ProfileMenu(uiState = uiState, onEvent = onEvent, modifier = Modifier.padding(horizontal = Dimens.SpaceMd))
        }
        item(key = "logout") {
            TextButton(
                onClick = { onEvent(ProfileHubUiEvent.LogoutRequested) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.SpaceMd),
            ) {
                Text(
                    text = stringResource(R.string.profile_logout),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }

    if (uiState.isLogoutConfirmVisible) {
        AlertDialog(
            onDismissRequest = { onEvent(ProfileHubUiEvent.LogoutDismissed) },
            title = { Text(stringResource(R.string.profile_logout_confirm_title)) },
            text = { Text(stringResource(R.string.profile_logout_confirm_message)) },
            confirmButton = {
                TextButton(onClick = { onEvent(ProfileHubUiEvent.LogoutConfirmed) }) {
                    Text(stringResource(R.string.profile_logout), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(ProfileHubUiEvent.LogoutDismissed) }) {
                    Text(stringResource(R.string.profile_cancel))
                }
            },
        )
    }
}

@Composable
private fun ProfileMenu(
    uiState: ProfileHubUiState,
    onEvent: (ProfileHubUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val open: (ProfileDestination) -> () -> Unit = { destination ->
        { onEvent(ProfileHubUiEvent.Open(destination)) }
    }
    QhathiaSettingsSection(title = null, modifier = modifier) {
        QhathiaNavigationRow(
            title = stringResource(R.string.menu_personal_title),
            subtitle = stringResource(R.string.menu_personal_subtitle),
            icon = Icons.Outlined.Person,
            onClick = open(ProfileDestination.PERSONAL_DATA),
        )
        QhathiaNavigationRow(
            title = stringResource(R.string.menu_notifications_title),
            subtitle = uiState.alertCounts?.let {
                pluralStringResource(R.plurals.menu_notifications_active, it.total, it.active, it.total)
            } ?: stringResource(R.string.menu_notifications_fallback),
            icon = Icons.Outlined.Notifications,
            onClick = open(ProfileDestination.NOTIFICATIONS),
        )
        QhathiaNavigationRow(
            title = stringResource(R.string.menu_security_title),
            subtitle = stringResource(R.string.menu_security_subtitle),
            icon = Icons.Outlined.Lock,
            onClick = open(ProfileDestination.SECURITY),
        )
        QhathiaNavigationRow(
            title = stringResource(R.string.menu_settings_title),
            subtitle = stringResource(R.string.menu_settings_subtitle),
            icon = Icons.Outlined.Settings,
            onClick = open(ProfileDestination.SETTINGS),
        )
        if (uiState.canManageStore) {
            QhathiaNavigationRow(
                title = stringResource(R.string.menu_devices_title),
                subtitle = uiState.tablets?.takeIf { it.linked > 0 }?.let { counts ->
                    stringResource(
                        R.string.menu_devices_summary,
                        pluralStringResource(R.plurals.menu_devices_linked, counts.linked, counts.linked),
                        pluralStringResource(R.plurals.menu_devices_connected, counts.connected, counts.connected),
                    )
                } ?: stringResource(R.string.menu_devices_fallback),
                icon = Icons.Outlined.TabletAndroid,
                onClick = open(ProfileDestination.DEVICES),
            )
        }
        QhathiaNavigationRow(
            title = stringResource(R.string.menu_help_title),
            subtitle = stringResource(R.string.menu_help_subtitle),
            icon = Icons.Outlined.SupportAgent,
            onClick = open(ProfileDestination.HELP),
            showDivider = false,
        )
    }
}

@Preview(name = "Mi perfil, owner — light", showBackground = true, heightDp = 900)
@Preview(name = "Mi perfil, owner — dark", showBackground = true, heightDp = 900, uiMode = 0x20)
@Composable
private fun ProfileHubOwnerPreview() {
    QhathiaTheme {
        ProfileHubContent(uiState = ProfileHubPreviewData.owner, onEvent = {})
    }
}

@Preview(name = "Mi perfil, seller — light", showBackground = true)
@Preview(name = "Mi perfil, seller — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun ProfileHubSellerPreview() {
    QhathiaTheme {
        ProfileHubContent(
            uiState = ProfileHubPreviewData.owner.copy(role = UserRole.SELLER),
            onEvent = {},
        )
    }
}

@Preview(name = "Mi perfil, loading / no subscription — light", showBackground = true)
@Composable
private fun ProfileHubLoadingPreview() {
    QhathiaTheme {
        ProfileHubContent(
            uiState = ProfileHubPreviewData.owner.copy(
                profile = LoadState.Loading,
                subscription = LoadState.Loaded(null),
                alertCounts = null,
                tablets = null,
            ),
            onEvent = {},
        )
    }
}
