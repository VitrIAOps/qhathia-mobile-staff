package com.qhathia.mobile.staff.feature.devicelinking.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.staff.core.domain.model.Device
import com.qhathia.mobile.staff.core.domain.model.DeviceStatus
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme
import com.qhathia.mobile.staff.feature.devicelinking.R
import com.qhathia.mobile.staff.feature.devicelinking.model.DeviceLinkingUiEvent
import com.qhathia.mobile.staff.feature.devicelinking.model.DeviceLinkingUiState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * Stateless linked-devices UI — a pure function of [uiState]. A header with a functional back
 * chevron and the bold "Dispositivos vinculados" title sits above the device list (loading / empty /
 * error states handled), with the pairing FAB floating over it and the pairing dialog shown on
 * demand.
 */
@Composable
fun DeviceLinkingContent(
    uiState: DeviceLinkingUiState,
    onEvent: (DeviceLinkingUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = Dimens.SpaceSm)
                .padding(horizontal = Dimens.SpaceMd),
        ) {
            DeviceLinkingHeader(onBack = onBack)
            Spacer(Modifier.height(Dimens.SpaceSm))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = Dimens.SpaceSm),
            ) {
                when {
                    uiState.isLoading && !uiState.hasDevices ->
                        LoadingState()
                    uiState.isError && !uiState.hasDevices ->
                        ErrorState(onRetry = { onEvent(DeviceLinkingUiEvent.Retry) })
                    !uiState.hasDevices ->
                        EmptyState()
                    else ->
                        DeviceListCard(devices = uiState.devices)
                }
            }
        }

        FloatingActionButton(
            onClick = { onEvent(DeviceLinkingUiEvent.OpenPairDialog) },
            shape = RoundedCornerShape(Dimens.ButtonRadius),
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(18.dp),
        ) {
            Icon(
                modifier = Modifier.size(32.dp),
                imageVector = Icons.Rounded.Add,
                contentDescription = stringResource(R.string.device_linking_add_cd),
            )
        }
    }

    if (uiState.isPairDialogVisible) {
        PairDeviceDialog(
            code = uiState.pairCode,
            canSubmit = uiState.canSubmitPair,
            isPairing = uiState.isPairing,
            errorText = uiState.pairErrorRes?.let { stringResource(it) },
            onCodeChange = { onEvent(DeviceLinkingUiEvent.PairCodeChanged(it)) },
            onSubmit = { onEvent(DeviceLinkingUiEvent.SubmitPair) },
            onDismiss = { onEvent(DeviceLinkingUiEvent.DismissPairDialog) },
        )
    }
}

@Composable
private fun DeviceLinkingHeader(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.SpaceSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.device_linking_back_cd),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = stringResource(R.string.device_linking_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = Dimens.SpaceXs),
        )
    }
}

@Composable
private fun DeviceListCard(
    devices: ImmutableList<Device>,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(Dimens.SpaceMd),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(Dimens.SpaceMd),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
        ) {
            items(devices, key = { it.id }) { device ->
                DeviceRow(device = device)
            }
        }
    }
}

@Composable
private fun DeviceRow(device: Device, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = device.serialNumber?.takeIf { it.isNotBlank() }
                ?: stringResource(R.string.device_unnamed),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(device.status.labelRes()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        device.appVersion?.let { version ->
            Text(
                text = stringResource(R.string.device_app_version_format, version),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@StringRes
private fun DeviceStatus.labelRes(): Int = when (this) {
    DeviceStatus.ACTIVE -> R.string.device_status_active
    DeviceStatus.PENDING_LINK -> R.string.device_status_pending
    DeviceStatus.INACTIVE -> R.string.device_status_inactive
    DeviceStatus.SUSPENDED -> R.string.device_status_suspended
    DeviceStatus.UNKNOWN -> R.string.device_status_unknown
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.device_linking_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(Dimens.SpaceLg),
        )
    }
}

@Composable
private fun ErrorState(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
            modifier = Modifier.padding(Dimens.SpaceLg),
        ) {
            Text(
                text = stringResource(R.string.device_linking_error),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.device_linking_retry))
            }
        }
    }
}

// ---- Previews ----

private fun sampleDevices(): ImmutableList<Device> = listOf(
    Device("d1", "Tablet Mostrador 1", DeviceStatus.ACTIVE, "1.0.0", "2026-04-15T18:30:00Z"),
    Device("d2", "SN-00042", DeviceStatus.PENDING_LINK, null, null),
).toImmutableList()

@Preview(name = "Devices — light", showBackground = true)
@Preview(name = "Devices — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun DeviceLinkingContentPreview() {
    QhathiaTheme {
        DeviceLinkingContent(
            uiState = DeviceLinkingUiState(devices = sampleDevices()),
            onEvent = {},
            onBack = {},
        )
    }
}

@Preview(name = "Devices — empty", showBackground = true)
@Composable
private fun DeviceLinkingContentEmptyPreview() {
    QhathiaTheme {
        DeviceLinkingContent(uiState = DeviceLinkingUiState(), onEvent = {}, onBack = {})
    }
}

@Preview(name = "Devices — loading", showBackground = true)
@Composable
private fun DeviceLinkingContentLoadingPreview() {
    QhathiaTheme {
        DeviceLinkingContent(
            uiState = DeviceLinkingUiState(isLoading = true),
            onEvent = {},
            onBack = {},
        )
    }
}

@Preview(name = "Devices — error", showBackground = true)
@Composable
private fun DeviceLinkingContentErrorPreview() {
    QhathiaTheme {
        DeviceLinkingContent(
            uiState = DeviceLinkingUiState(isError = true),
            onEvent = {},
            onBack = {},
        )
    }
}

@Preview(name = "Devices — pair dialog", showBackground = true)
@Composable
private fun DeviceLinkingContentPairDialogPreview() {
    QhathiaTheme {
        DeviceLinkingContent(
            uiState = DeviceLinkingUiState(
                devices = sampleDevices(),
                isPairDialogVisible = true,
                pairCode = "AB3CD",
            ),
            onEvent = {},
            onBack = {},
        )
    }
}
