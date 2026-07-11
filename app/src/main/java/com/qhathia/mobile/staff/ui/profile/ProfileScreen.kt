package com.qhathia.mobile.staff.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.staff.R
import com.qhathia.mobile.staff.core.ui.component.QhathiaButton
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme

/**
 * Profile section (placeholder for this change) that exposes the entry point to device linking. The
 * "Vincular terminal" button navigates to the full-screen linked-devices destination.
 */
@Composable
fun ProfileScreen(
    onNavigateToLinkDevices: () -> Unit,
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
        }
    }
}

@Preview(name = "Profile — light", showBackground = true)
@Preview(name = "Profile — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun ProfileScreenPreview() {
    QhathiaTheme {
        ProfileScreen(onNavigateToLinkDevices = {})
    }
}
