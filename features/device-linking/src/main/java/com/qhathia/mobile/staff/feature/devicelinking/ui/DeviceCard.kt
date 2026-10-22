package com.qhathia.mobile.staff.feature.devicelinking.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.TabletAndroid
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.staff.core.domain.model.Device
import com.qhathia.mobile.staff.core.domain.model.DeviceCommand
import com.qhathia.mobile.staff.core.domain.model.DeviceCommandStatus
import com.qhathia.mobile.staff.core.domain.model.DeviceCommandType
import com.qhathia.mobile.staff.core.ui.component.settings.QhathiaComingSoonTag
import com.qhathia.mobile.staff.core.ui.component.time.label
import com.qhathia.mobile.staff.core.ui.component.time.relativeTimeBetween
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaColors
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme
import com.qhathia.mobile.staff.feature.devicelinking.R
import java.time.Instant

/**
 * One tablet: identity and status (with the settings shortcut at the top right), the visible catalog,
 * the last reported telemetry, and the two remote actions with the state of their latest command.
 */
@Composable
internal fun DeviceCard(
    device: Device,
    catalogCount: Int?,
    now: Instant,
    refreshCommand: DeviceCommand?,
    restartCommand: DeviceCommand?,
    isRefreshBusy: Boolean,
    isRestartBusy: Boolean,
    onRefreshCatalog: () -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit = {},
) {
    val connected = device.isConnected(now)
    Surface(
        shape = RoundedCornerShape(Dimens.CardRadius),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(Dimens.SpaceMd), verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd)) {
            DeviceIdentity(device = device, connected = connected, onOpenSettings = onOpenSettings)
            CatalogBox(
                catalogCount = catalogCount,
                updatedAt = listOfNotNull(device.lastSyncAt, device.lastPingAt).maxOrNull(),
                now = now,
            )
            TileRow(
                start = Tile(
                    icon = Icons.Rounded.BatteryChargingFull,
                    iconTint = QhathiaColors.success,
                    value = device.batteryLevel?.let { "$it%" } ?: stringResource(R.string.device_missing),
                    label = stringResource(R.string.device_battery),
                ),
                end = Tile(
                    icon = Icons.Rounded.Wifi,
                    iconTint = MaterialTheme.colorScheme.secondary,
                    value = networkLabel(device.networkStatus),
                    label = stringResource(R.string.device_connection),
                ),
            )
            TileRow(
                start = Tile(
                    icon = Icons.Outlined.Storefront,
                    iconTint = MaterialTheme.colorScheme.secondary,
                    value = device.storeName ?: stringResource(R.string.device_missing),
                    label = stringResource(R.string.device_store),
                ),
                end = Tile(
                    icon = Icons.Outlined.Groups,
                    iconTint = MaterialTheme.colorScheme.secondary,
                    value = stringResource(R.string.device_missing),
                    label = stringResource(R.string.device_customers_today),
                    comingSoon = true,
                ),
            )
            CommandActions(
                isRefreshBusy = isRefreshBusy,
                isRestartBusy = isRestartBusy,
                onRefreshCatalog = onRefreshCatalog,
                onRestart = onRestart,
            )
            CommandStatusLine(labelRes = R.string.device_command_refresh_label, command = refreshCommand)
            CommandStatusLine(labelRes = R.string.device_command_restart_label, command = restartCommand)
        }
    }
}

@Composable
private fun DeviceIdentity(device: Device, connected: Boolean, onOpenSettings: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(
            imageVector = Icons.Outlined.TabletAndroid,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp),
        )
        Spacer(Modifier.width(Dimens.SpaceMd))
        Column(modifier = Modifier.weight(1f).semantics(mergeDescendants = true) { heading() }) {
            Text(
                text = device.name ?: stringResource(R.string.device_unnamed),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                val statusColor = if (connected) QhathiaColors.success else MaterialTheme.colorScheme.onSurfaceVariant
                Box(modifier = Modifier.size(8.dp).background(statusColor, CircleShape))
                Spacer(Modifier.width(Dimens.SpaceXs))
                Text(
                    text = stringResource(if (connected) R.string.device_connected else R.string.device_disconnected),
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor,
                )
            }
        }
        // Always available: settings are stored and applied when the tablet next reports.
        IconButton(onClick = onOpenSettings) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = stringResource(R.string.tablet_settings_entry),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Product count and "Actualizado hace X". [updatedAt] is the most recent of the tablet's catalog
 * sync and its last report, so the line shows the last time the tablet was in touch.
 */
@Composable
private fun CatalogBox(catalogCount: Int?, updatedAt: Instant?, now: Instant) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(QhathiaColors.filterTrack, RoundedCornerShape(Dimens.ButtonRadius))
            .padding(Dimens.SpaceMd)
            .semantics(mergeDescendants = true) {},
    ) {
        Text(
            text = stringResource(R.string.device_catalog_label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = catalogCount?.let { pluralStringResource(R.plurals.device_catalog_products, it, it) }
                ?: stringResource(R.string.device_missing),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (updatedAt != null) {
            Text(
                text = stringResource(R.string.device_catalog_updated, relativeTimeBetween(updatedAt, now).label()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private data class Tile(
    val icon: ImageVector,
    val iconTint: androidx.compose.ui.graphics.Color,
    val value: String,
    val label: String,
    val comingSoon: Boolean = false,
)

@Composable
private fun TileRow(start: Tile, end: Tile) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(Dimens.FieldRadius))
            .padding(vertical = Dimens.SpaceSm),
    ) {
        TileCell(tile = start, modifier = Modifier.weight(1f))
        VerticalDivider(modifier = Modifier.fillMaxHeight(), color = MaterialTheme.colorScheme.outline)
        TileCell(tile = end, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun TileCell(tile: Tile, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .padding(horizontal = Dimens.SpaceMd)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = tile.icon, contentDescription = null, tint = tile.iconTint)
        Spacer(Modifier.width(Dimens.SpaceSm))
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = tile.value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (tile.comingSoon) {
                    Spacer(Modifier.width(Dimens.SpaceXs))
                    QhathiaComingSoonTag()
                }
            }
            Text(
                text = tile.label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CommandActions(
    isRefreshBusy: Boolean,
    isRestartBusy: Boolean,
    onRefreshCatalog: () -> Unit,
    onRestart: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm), modifier = Modifier.fillMaxWidth()) {
        Button(
            onClick = onRefreshCatalog,
            enabled = !isRefreshBusy,
            shape = RoundedCornerShape(Dimens.FieldRadius),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            modifier = Modifier.weight(1.4f).height(Dimens.ButtonHeight),
        ) {
            Icon(imageVector = Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(Dimens.IconSm))
            Spacer(Modifier.width(Dimens.SpaceXs))
            Text(text = stringResource(R.string.device_refresh_catalog), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        OutlinedButton(
            onClick = onRestart,
            enabled = !isRestartBusy,
            shape = RoundedCornerShape(Dimens.FieldRadius),
            modifier = Modifier.weight(1f).height(Dimens.ButtonHeight),
        ) {
            Icon(imageVector = Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(Dimens.IconSm))
            Spacer(Modifier.width(Dimens.SpaceXs))
            Text(text = stringResource(R.string.device_restart), maxLines = 1)
        }
    }
}

/** "Catálogo: Enviado" — shown only once a command of that type exists. */
@Composable
private fun CommandStatusLine(labelRes: Int, command: DeviceCommand?) {
    val status = command?.statusLabel() ?: return
    val color = when (command.status) {
        DeviceCommandStatus.ACKED -> QhathiaColors.success
        DeviceCommandStatus.FAILED, DeviceCommandStatus.EXPIRED -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        text = stringResource(R.string.device_command_status_line, stringResource(labelRes), status),
        style = MaterialTheme.typography.bodySmall,
        color = color,
    )
}

@Composable
private fun DeviceCommand.statusLabel(): String? = when (status) {
    DeviceCommandStatus.PENDING, DeviceCommandStatus.DELIVERED -> expiresAt
        ?.let { stringResource(R.string.device_command_sent_retry_at, it.toShortLocalTime()) }
        ?: stringResource(R.string.device_command_sent)
    DeviceCommandStatus.ACKED -> stringResource(R.string.device_command_ready)
    DeviceCommandStatus.FAILED -> resultDetail
        ?.let { stringResource(R.string.device_command_failed_detail, it) }
        ?: stringResource(R.string.device_command_failed)
    DeviceCommandStatus.EXPIRED -> stringResource(R.string.device_command_expired)
    DeviceCommandStatus.UNKNOWN -> null
}

@Composable
private fun networkLabel(networkStatus: String?): String = when (networkStatus?.uppercase()) {
    null -> stringResource(R.string.device_missing)
    "WIFI" -> stringResource(R.string.device_network_wifi)
    "MOBILE", "CELLULAR", "LTE", "4G", "5G" -> stringResource(R.string.device_network_mobile)
    "ONLINE" -> stringResource(R.string.device_network_online)
    "LIMITED" -> stringResource(R.string.device_network_limited)
    else -> networkStatus
}

@Preview(name = "Device card — light", showBackground = true)
@Preview(name = "Device card — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun DeviceCardPreview() {
    QhathiaTheme {
        DeviceCard(
            device = DeviceLinkingPreviewData.connected,
            catalogCount = 12,
            now = DeviceLinkingPreviewData.now,
            refreshCommand = null,
            restartCommand = null,
            isRefreshBusy = false,
            isRestartBusy = false,
            onRefreshCatalog = {},
            onRestart = {},
            modifier = Modifier.padding(Dimens.SpaceMd),
        )
    }
}

@Preview(name = "Device card — command in flight", showBackground = true, widthDp = 360)
@Composable
private fun DeviceCardCommandInFlightPreview() {
    QhathiaTheme {
        DeviceCard(
            device = DeviceLinkingPreviewData.disconnected,
            catalogCount = 12,
            now = DeviceLinkingPreviewData.now,
            refreshCommand = DeviceCommand(
                id = "c1",
                type = DeviceCommandType.REFRESH_CATALOG,
                status = DeviceCommandStatus.PENDING,
                requestedAt = DeviceLinkingPreviewData.now,
                expiresAt = DeviceLinkingPreviewData.now.plusSeconds(600),
                resultDetail = null,
            ),
            restartCommand = null,
            isRefreshBusy = true,
            isRestartBusy = false,
            onRefreshCatalog = {},
            onRestart = {},
            modifier = Modifier.padding(Dimens.SpaceMd),
        )
    }
}
