package com.qhathia.mobile.staff.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.qhathia.mobile.staff.R
import com.qhathia.mobile.staff.core.ui.component.QhathiaButton
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme

/**
 * Profile section (placeholder for this change) exposing device linking and sign out. Signing out
 * clears the session, which returns the app to login automatically.
 */
@Composable
fun ProfileScreen(
    onNavigateToLinkDevices: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    ProfileContent(
        onNavigateToLinkDevices = onNavigateToLinkDevices,
        onLogoutClick = { showLogoutDialog = true },
        modifier = modifier,
    )

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(stringResource(R.string.profile_logout_confirm_title)) },
            text = { Text(stringResource(R.string.profile_logout_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                    },
                ) {
                    Text(stringResource(R.string.profile_logout))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun ProfileContent(
    onNavigateToLinkDevices: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.nav_profile),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(R.string.placeholder_coming_soon),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp),
            )
            QhathiaButton(
                text = stringResource(R.string.profile_link_devices),
                onClick = onNavigateToLinkDevices,
            )
            TextButton(
                onClick = onLogoutClick,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text(
                    text = stringResource(R.string.profile_logout),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Preview(name = "Profile — light", showBackground = true)
@Preview(name = "Profile — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun ProfileScreenPreview() {
    QhathiaTheme {
        ProfileContent(onNavigateToLinkDevices = {}, onLogoutClick = {})
    }
}
