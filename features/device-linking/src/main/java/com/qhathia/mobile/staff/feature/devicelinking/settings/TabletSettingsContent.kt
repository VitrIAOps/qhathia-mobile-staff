package com.qhathia.mobile.staff.feature.devicelinking.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LinkOff
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.qhathia.mobile.staff.core.domain.model.BrightnessMode
import com.qhathia.mobile.staff.core.domain.model.Device
import com.qhathia.mobile.staff.core.domain.model.DeviceSettings
import com.qhathia.mobile.staff.core.domain.model.DeviceSettingsChange
import com.qhathia.mobile.staff.core.domain.model.DeviceStatus
import com.qhathia.mobile.staff.core.ui.component.QhathiaSegmentedControl
import com.qhathia.mobile.staff.core.ui.component.settings.QhathiaGradientTopBar
import com.qhathia.mobile.staff.core.ui.component.settings.QhathiaNavigationRow
import com.qhathia.mobile.staff.core.ui.component.settings.QhathiaSettingsSection
import com.qhathia.mobile.staff.core.ui.component.settings.QhathiaSwitchRow
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme
import com.qhathia.mobile.staff.feature.devicelinking.R
import kotlinx.collections.immutable.persistentListOf
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val BrightnessOptions = persistentListOf(
    BrightnessMode.AUTO,
    BrightnessMode.LOW,
    BrightnessMode.MEDIUM,
    BrightnessMode.HIGH,
)

/**
 * Ajustes de la tablet: the three groups of settings the owner controls remotely, then the tablet's
 * Información (hidden when the device list could not be read), a note that changes travel with
 * the tablet's next report, and the "Desvincular tablet" action at the very end.
 */
@Composable
internal fun TabletSettingsContent(
    uiState: TabletSettingsUiState,
    onEvent: (TabletSettingsUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        QhathiaGradientTopBar(title = stringResource(R.string.tablet_settings_title), onBack = onBack)
        val settings = uiState.settings
        when {
            uiState.load == TabletSettingsLoad.LOADING -> CenteredBox {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
            }
            uiState.load == TabletSettingsLoad.NOT_FOUND -> CenteredBox {
                CenteredMessage(stringResource(R.string.tablet_settings_not_found))
            }
            uiState.load == TabletSettingsLoad.ERROR || settings == null -> CenteredBox {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CenteredMessage(stringResource(R.string.tablet_settings_load_error))
                    TextButton(onClick = { onEvent(TabletSettingsUiEvent.Retry) }) {
                        Text(stringResource(R.string.tablet_settings_retry))
                    }
                }
            }
            else -> SettingsList(uiState = uiState, settings = settings, onEvent = onEvent)
        }
    }

    uiState.pinDialog?.let { dialog ->
        ExitPinDialog(
            state = dialog,
            hasPin = uiState.settings?.exitPinSet == true,
            onEvent = onEvent,
        )
    }

    if (uiState.isUnlinkConfirmVisible) {
        val name = uiState.device?.name ?: stringResource(R.string.device_unnamed)
        AlertDialog(
            onDismissRequest = { onEvent(TabletSettingsUiEvent.UnlinkDismissed) },
            title = { Text(stringResource(R.string.device_unlink_title, name)) },
            text = { Text(stringResource(R.string.device_unlink_message)) },
            confirmButton = {
                TextButton(onClick = { onEvent(TabletSettingsUiEvent.UnlinkConfirmed) }) {
                    Text(stringResource(R.string.device_unlink_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(TabletSettingsUiEvent.UnlinkDismissed) }) {
                    Text(stringResource(R.string.device_cancel))
                }
            },
        )
    }
}

@Composable
private fun SettingsList(
    uiState: TabletSettingsUiState,
    settings: DeviceSettings,
    onEvent: (TabletSettingsUiEvent) -> Unit,
) {
    fun changed(change: DeviceSettingsChange) = onEvent(TabletSettingsUiEvent.SettingChanged(change))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Dimens.SpaceMd)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceLg),
    ) {
        QhathiaSettingsSection(title = stringResource(R.string.tablet_settings_group_display)) {
            QhathiaSwitchRow(
                title = stringResource(R.string.tablet_settings_idle_showcase),
                checked = settings.idleShowcaseEnabled,
                onCheckedChange = { changed(DeviceSettingsChange.IdleShowcase(it)) },
                enabled = !uiState.isSaving(SettingKey.IDLE_SHOWCASE),
            )
            QhathiaSwitchRow(
                title = stringResource(R.string.tablet_settings_voice),
                checked = settings.voiceAssistantEnabled,
                onCheckedChange = { changed(DeviceSettingsChange.VoiceAssistant(it)) },
                enabled = !uiState.isSaving(SettingKey.VOICE_ASSISTANT),
            )
            Column(
                modifier = Modifier.padding(vertical = Dimens.SpaceSm),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
            ) {
                Text(
                    text = stringResource(R.string.tablet_settings_brightness),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                QhathiaSegmentedControl(
                    options = BrightnessOptions,
                    selected = settings.brightnessMode,
                    // One brightness request at a time; the ViewModel ignores taps meanwhile.
                    onSelect = { changed(DeviceSettingsChange.Brightness(it)) },
                    label = { brightnessLabel(it) },
                )
            }
        }

        QhathiaSettingsSection(title = stringResource(R.string.tablet_settings_group_security)) {
            QhathiaSwitchRow(
                title = stringResource(R.string.tablet_settings_kiosk),
                subtitle = if (uiState.isKioskWithoutPin) {
                    stringResource(R.string.tablet_settings_kiosk_no_pin_warning)
                } else {
                    null
                },
                checked = settings.kioskMode,
                onCheckedChange = { changed(DeviceSettingsChange.KioskMode(it)) },
                enabled = !uiState.isSaving(SettingKey.KIOSK_MODE),
            )
            QhathiaNavigationRow(
                title = stringResource(R.string.tablet_settings_pin),
                subtitle = stringResource(
                    if (settings.exitPinSet) R.string.tablet_settings_pin_set else R.string.tablet_settings_pin_not_set,
                ),
                onClick = { onEvent(TabletSettingsUiEvent.OpenPinDialog) },
                showDivider = false,
            )
        }

        QhathiaSettingsSection(title = stringResource(R.string.tablet_settings_group_catalog)) {
            QhathiaSwitchRow(
                title = stringResource(R.string.tablet_settings_auto_sync),
                checked = settings.autoSyncEnabled,
                onCheckedChange = { changed(DeviceSettingsChange.AutoSync(it)) },
                enabled = !uiState.isSaving(SettingKey.AUTO_SYNC),
                showDivider = false,
            )
        }

        uiState.device?.let { device -> InformationSection(device) }

        Text(
            text = stringResource(R.string.tablet_settings_footnote),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )

        TextButton(
            onClick = { onEvent(TabletSettingsUiEvent.UnlinkRequested) },
            enabled = !uiState.isUnlinking,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Outlined.LinkOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(Dimens.IconSm),
            )
            Spacer(Modifier.width(Dimens.SpaceXs))
            Text(text = stringResource(R.string.device_unlink), color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun InformationSection(device: Device) {
    val missing = stringResource(R.string.device_missing)
    val rows = listOf(
        R.string.tablet_settings_info_name to (device.name ?: missing),
        R.string.tablet_settings_info_model to (device.model ?: missing),
        R.string.tablet_settings_info_serial to (device.serialNumber ?: missing),
        R.string.tablet_settings_info_linked to (formatLinkedAt(device.linkedAt) ?: missing),
        R.string.tablet_settings_info_app_version to (device.appVersion ?: missing),
        R.string.tablet_settings_info_store to (device.storeName ?: missing),
    )
    QhathiaSettingsSection(title = stringResource(R.string.tablet_settings_group_info)) {
        rows.forEachIndexed { index, (labelRes, value) ->
            QhathiaNavigationRow(
                title = stringResource(labelRes),
                onClick = null,
                showDivider = index < rows.lastIndex,
                trailing = {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        }
    }
}

@Composable
private fun brightnessLabel(mode: BrightnessMode): String = stringResource(
    when (mode) {
        BrightnessMode.AUTO, BrightnessMode.UNKNOWN -> R.string.tablet_settings_brightness_auto
        BrightnessMode.LOW -> R.string.tablet_settings_brightness_low
        BrightnessMode.MEDIUM -> R.string.tablet_settings_brightness_medium
        BrightnessMode.HIGH -> R.string.tablet_settings_brightness_high
    },
)

/** "15 abr 2026" in the device's locale and zone, or null when absent or unreadable. */
private fun formatLinkedAt(raw: String?): String? = runCatching {
    val instant = Instant.parse(raw ?: return null)
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withZone(ZoneId.systemDefault()).format(instant)
}.getOrNull()

@Composable
private fun CenteredBox(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(Dimens.SpaceLg), contentAlignment = Alignment.Center) {
        content()
    }
}

@Composable
private fun CenteredMessage(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

// ---- Previews ----

private val PreviewDevice = Device(
    id = "d1",
    serialNumber = "HA1P1C2K",
    status = DeviceStatus.ACTIVE,
    appVersion = "1.4.0",
    linkedAt = "2026-04-15T18:30:00Z",
    name = "Tablet mostrador",
    storeName = "Stand 1",
    model = "Lenovo Tab M11",
)

private val PreviewSettings = DeviceSettings(
    idleShowcaseEnabled = true,
    voiceAssistantEnabled = true,
    brightnessMode = BrightnessMode.MEDIUM,
    kioskMode = true,
    exitPinSet = true,
    autoSyncEnabled = true,
)

@Preview(name = "Ajustes de la tablet", showBackground = true, heightDp = 1100)
@Preview(name = "Ajustes de la tablet dark", showBackground = true, heightDp = 1100, uiMode = 0x20)
@Composable
private fun TabletSettingsContentPreview() {
    QhathiaTheme {
        TabletSettingsContent(
            uiState = TabletSettingsUiState(
                load = TabletSettingsLoad.LOADED,
                settings = PreviewSettings,
                device = PreviewDevice,
            ),
            onEvent = {},
            onBack = {},
        )
    }
}

// Kiosk mode with no PIN warns on the kiosk row; a toggle in flight is disabled.
@Preview(name = "Ajustes de la tablet — no PIN, saving", showBackground = true, heightDp = 800)
@Composable
private fun TabletSettingsNoPinPreview() {
    QhathiaTheme {
        TabletSettingsContent(
            uiState = TabletSettingsUiState(
                load = TabletSettingsLoad.LOADED,
                settings = PreviewSettings.copy(exitPinSet = false),
                saving = setOf(SettingKey.VOICE_ASSISTANT),
            ),
            onEvent = {},
            onBack = {},
        )
    }
}

@Preview(name = "Ajustes de la tablet — error", showBackground = true)
@Composable
private fun TabletSettingsErrorPreview() {
    QhathiaTheme {
        TabletSettingsContent(uiState = TabletSettingsUiState(load = TabletSettingsLoad.ERROR), onEvent = {}, onBack = {})
    }
}

@Preview(name = "Ajustes de la tablet — not found", showBackground = true)
@Composable
private fun TabletSettingsNotFoundPreview() {
    QhathiaTheme {
        TabletSettingsContent(uiState = TabletSettingsUiState(load = TabletSettingsLoad.NOT_FOUND), onEvent = {}, onBack = {})
    }
}

@Preview(name = "Ajustes de la tablet — PIN dialog", showBackground = true)
@Preview(name = "Ajustes de la tablet — PIN dialog dark", showBackground = true, uiMode = 0x20)
@Composable
private fun TabletSettingsPinDialogPreview() {
    QhathiaTheme {
        TabletSettingsContent(
            uiState = TabletSettingsUiState(
                load = TabletSettingsLoad.LOADED,
                settings = PreviewSettings,
                pinDialog = ExitPinDialogState(pin = "1234", confirmation = "12"),
            ),
            onEvent = {},
            onBack = {},
        )
    }
}
