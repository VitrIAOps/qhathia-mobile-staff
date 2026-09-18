package com.qhathia.mobile.staff.feature.devicelinking.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.qhathia.mobile.staff.core.domain.model.Device
import com.qhathia.mobile.staff.core.domain.model.DeviceCommandStatus
import com.qhathia.mobile.staff.core.domain.model.DeviceCommandType
import com.qhathia.mobile.staff.core.domain.model.DeviceStatus
import com.qhathia.mobile.staff.core.domain.model.DeviceCommand
import com.qhathia.mobile.staff.core.ui.component.settings.QhathiaGradientTopBar
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme
import com.qhathia.mobile.staff.feature.devicelinking.R
import com.qhathia.mobile.staff.feature.devicelinking.model.DeviceLinkingUiEvent
import com.qhathia.mobile.staff.feature.devicelinking.model.DeviceLinkingUiState
import kotlinx.collections.immutable.persistentListOf
import java.time.Instant

/**
 * Stateless Dispositivos — a pure function of [uiState]: the gradient top bar, one status card per
 * tablet (loading / empty / error handled), the "Vincular otro dispositivo" action, and the pairing
 * and restart dialogs on demand.
 */
@Composable
fun DeviceLinkingContent(
    uiState: DeviceLinkingUiState,
    onEvent: (DeviceLinkingUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        QhathiaGradientTopBar(title = stringResource(R.string.device_linking_title), onBack = onBack)
        when {
            uiState.isLoading && !uiState.hasDevices -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary) }
            uiState.isError && !uiState.hasDevices -> ErrorState(onRetry = { onEvent(DeviceLinkingUiEvent.Retry) })
            else -> DeviceList(uiState = uiState, onEvent = onEvent)
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

    uiState.pendingRestartDeviceId?.let { deviceId ->
        val name = uiState.devices.firstOrNull { it.id == deviceId }?.name
            ?: stringResource(R.string.device_unnamed)
        AlertDialog(
            onDismissRequest = { onEvent(DeviceLinkingUiEvent.RestartDismissed) },
            title = { Text(stringResource(R.string.device_restart_title)) },
            text = { Text(stringResource(R.string.device_restart_message, name)) },
            confirmButton = {
                TextButton(onClick = { onEvent(DeviceLinkingUiEvent.RestartConfirmed) }) {
                    Text(stringResource(R.string.device_restart), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(DeviceLinkingUiEvent.RestartDismissed) }) {
                    Text(stringResource(R.string.device_cancel))
                }
            },
        )
    }
}

@Composable
private fun DeviceList(uiState: DeviceLinkingUiState, onEvent: (DeviceLinkingUiEvent) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(Dimens.SpaceMd),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
    ) {
        if (!uiState.hasDevices) {
            item(key = "empty") {
                Text(
                    text = stringResource(R.string.device_linking_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(Dimens.SpaceLg),
                )
            }
        }
        items(uiState.devices, key = { it.id }) { device ->
            DeviceCard(
                device = device,
                catalogCount = uiState.catalogCount,
                now = uiState.now,
                refreshCommand = uiState.commandFor(device.id, DeviceCommandType.REFRESH_CATALOG),
                restartCommand = uiState.commandFor(device.id, DeviceCommandType.RESTART_APP),
                isRefreshBusy = uiState.isCommandBusy(device.id, DeviceCommandType.REFRESH_CATALOG),
                isRestartBusy = uiState.isCommandBusy(device.id, DeviceCommandType.RESTART_APP),
                onRefreshCatalog = { onEvent(DeviceLinkingUiEvent.RefreshCatalog(device.id)) },
                onRestart = { onEvent(DeviceLinkingUiEvent.RestartRequested(device.id)) },
            )
        }
        item(key = "link") {
            TextButton(
                onClick = { onEvent(DeviceLinkingUiEvent.OpenPairDialog) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(imageVector = Icons.Rounded.Add, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Text(
                    text = stringResource(R.string.device_linking_add),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(start = Dimens.SpaceSm),
                )
            }
        }
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
                Text(stringResource(R.string.device_linking_retry), color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

// ---- Previews ----

internal object DeviceLinkingPreviewData {
    val now: Instant = Instant.parse("2026-03-10T15:00:00Z")

    val connected = Device(
        id = "d1",
        serialNumber = "HA1P1C2K",
        status = DeviceStatus.ACTIVE,
        appVersion = "1.4.2",
        linkedAt = "2025-08-15T18:30:00Z",
        name = "Tablet vitrina 1",
        batteryLevel = 82,
        networkStatus = "WIFI",
        storeName = "Stand 1",
        lastPingAt = now.minusSeconds(120),
        lastSyncAt = now.minusSeconds(240),
    )

    val disconnected = connected.copy(
        id = "d2",
        name = "Tablet mostrador",
        batteryLevel = null,
        networkStatus = null,
        lastPingAt = now.minusSeconds(7_200),
        lastSyncAt = null,
    )
}

@Preview(name = "Dispositivos — light", showBackground = true, heightDp = 1100)
@Preview(name = "Dispositivos — dark", showBackground = true, heightDp = 1100, uiMode = 0x20)
@Composable
private fun DeviceLinkingContentPreview() {
    QhathiaTheme {
        DeviceLinkingContent(
            uiState = DeviceLinkingUiState(
                devices = persistentListOf(DeviceLinkingPreviewData.connected, DeviceLinkingPreviewData.disconnected),
                catalogCount = 12,
                now = DeviceLinkingPreviewData.now,
                commands = mapOf(
                    "d1" to mapOf(
                        DeviceCommandType.REFRESH_CATALOG to DeviceCommand(
                            "c1", DeviceCommandType.REFRESH_CATALOG, DeviceCommandStatus.PENDING, DeviceLinkingPreviewData.now, null,
                        ),
                        DeviceCommandType.RESTART_APP to DeviceCommand(
                            "c2", DeviceCommandType.RESTART_APP, DeviceCommandStatus.FAILED, DeviceLinkingPreviewData.now, "sin conexión",
                        ),
                    ),
                ),
            ),
            onEvent = {},
            onBack = {},
        )
    }
}

@Preview(name = "Dispositivos — empty", showBackground = true)
@Composable
private fun DeviceLinkingContentEmptyPreview() {
    QhathiaTheme {
        DeviceLinkingContent(uiState = DeviceLinkingUiState(), onEvent = {}, onBack = {})
    }
}

@Preview(name = "Dispositivos — loading", showBackground = true)
@Composable
private fun DeviceLinkingContentLoadingPreview() {
    QhathiaTheme {
        DeviceLinkingContent(uiState = DeviceLinkingUiState(isLoading = true), onEvent = {}, onBack = {})
    }
}

@Preview(name = "Dispositivos — error", showBackground = true)
@Composable
private fun DeviceLinkingContentErrorPreview() {
    QhathiaTheme {
        DeviceLinkingContent(uiState = DeviceLinkingUiState(isError = true), onEvent = {}, onBack = {})
    }
}

@Preview(name = "Dispositivos — pair dialog", showBackground = true)
@Composable
private fun DeviceLinkingContentPairDialogPreview() {
    QhathiaTheme {
        DeviceLinkingContent(
            uiState = DeviceLinkingUiState(
                devices = persistentListOf(DeviceLinkingPreviewData.connected),
                isPairDialogVisible = true,
                pairCode = "AB3CD",
            ),
            onEvent = {},
            onBack = {},
        )
    }
}
