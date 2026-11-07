package com.qhathia.mobile.staff.core.ui.component.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.staff.core.ui.R
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaColors
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme

private val RowMinHeight = 56.dp
private val RowIconSize = 24.dp

/**
 * A DS settings group: an optional group label ("Pedidos", "Sesiones activas") above a rounded
 * card holding the rows.
 */
@Composable
fun QhathiaSettingsSection(
    title: String?,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .padding(bottom = Dimens.SpaceSm)
                    .semantics { heading() },
            )
        }
        Surface(
            shape = RoundedCornerShape(Dimens.CardRadius),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = Dimens.SpaceMd, vertical = Dimens.SpaceXs),
                content = content,
            )
        }
    }
}

/**
 * A row that opens something: optional leading icon, title, optional subtitle and a trailing
 * accent chevron (or custom [trailing]). With [comingSoon] the row is inert, tagged
 * "Próximamente", and announced as such.
 */
@Composable
fun QhathiaNavigationRow(
    title: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    comingSoon: Boolean = false,
    showDivider: Boolean = true,
    trailing: @Composable RowScope.() -> Unit = { RowChevron() },
) {
    val interactive = onClick != null && !comingSoon
    val comingSoonLabel = stringResource(R.string.coming_soon_cd, title)
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = RowMinHeight)
                .then(
                    if (interactive) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier,
                )
                .then(
                    if (comingSoon) {
                        Modifier.clearAndSetSemantics {
                            contentDescription = comingSoonLabel
                            disabled()
                        }
                    } else {
                        Modifier
                    },
                )
                .padding(vertical = Dimens.SpaceSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(RowIconSize),
                )
                Spacer(Modifier.width(Dimens.SpaceMd))
            }
            RowTexts(title = title, subtitle = subtitle, comingSoon = comingSoon, modifier = Modifier.weight(1f))
            if (!comingSoon) trailing()
        }
        if (showDivider) HorizontalDivider(color = QhathiaColors.rowDivider)
    }
}

/**
 * A row holding a switch. The whole row is one `Switch`-role node, so tapping anywhere toggles it
 * and a screen reader reads title, subtitle and state together. With [comingSoon] there is no
 * switch: the row is inert, tagged "Próximamente", and announced as such.
 */
@Composable
fun QhathiaSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
    comingSoon: Boolean = false,
    showDivider: Boolean = true,
) {
    val interactive = enabled && onCheckedChange != null
    val comingSoonLabel = stringResource(R.string.coming_soon_cd, title)
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = RowMinHeight)
                .then(
                    if (comingSoon) {
                        Modifier.clearAndSetSemantics {
                            contentDescription = comingSoonLabel
                            disabled()
                        }
                    } else {
                        Modifier.toggleable(
                            value = checked,
                            enabled = interactive,
                            role = Role.Switch,
                            onValueChange = { onCheckedChange?.invoke(it) },
                        )
                    },
                )
                .padding(vertical = Dimens.SpaceSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RowTexts(title = title, subtitle = subtitle, comingSoon = comingSoon, modifier = Modifier.weight(1f))
            if (comingSoon) return@Row
            Spacer(Modifier.width(Dimens.SpaceSm))
            Switch(
                checked = checked,
                onCheckedChange = null,
                enabled = interactive,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = QhathiaColors.switchChecked,
                    checkedThumbColor = MaterialTheme.colorScheme.surface,
                    checkedBorderColor = QhathiaColors.switchChecked,
                ),
            )
        }
        if (showDivider) HorizontalDivider(color = QhathiaColors.rowDivider)
    }
}

/** The small neutral "Próximamente" pill. */
@Composable
fun QhathiaComingSoonTag(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.coming_soon),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .background(QhathiaColors.segmentTrack, RoundedCornerShape(Dimens.PillRadius))
            .padding(horizontal = Dimens.SpaceSm, vertical = 2.dp),
    )
}

/** The accent chevron closing a navigation row. */
@Composable
fun RowChevron(modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.secondary,
        modifier = modifier,
    )
}

@Composable
private fun RowTexts(
    title: String,
    subtitle: String?,
    comingSoon: Boolean,
    modifier: Modifier = Modifier,
) {
    val alpha = if (comingSoon) 0.6f else 1f
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                modifier = Modifier.weight(1f, fill = false),
            )
            if (comingSoon) {
                Spacer(Modifier.width(Dimens.SpaceSm))
                QhathiaComingSoonTag()
            }
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
            )
        }
    }
}

@Preview(name = "Settings rows — light", showBackground = true)
@Preview(name = "Settings rows — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun QhathiaSettingsRowsPreview() {
    QhathiaTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(Dimens.SpaceMd),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceLg),
        ) {
            QhathiaSettingsSection(title = null) {
                QhathiaNavigationRow(
                    title = "Datos personales",
                    subtitle = "Tu cuenta y los datos de tu tienda",
                    icon = Icons.Outlined.Person,
                    onClick = {},
                )
                QhathiaNavigationRow(
                    title = "Seguridad",
                    subtitle = "Contraseña, huella y sesiones",
                    icon = Icons.Outlined.Lock,
                    onClick = {},
                    showDivider = false,
                )
            }
            QhathiaSettingsSection(title = "Pedidos") {
                QhathiaSwitchRow(title = "Nuevo pedido", checked = true, onCheckedChange = {})
                QhathiaSwitchRow(
                    title = "Pedido por expirar",
                    subtitle = "Aviso 10 minutos antes de que venza",
                    checked = false,
                    onCheckedChange = {},
                )
                QhathiaSwitchRow(title = "Batería baja", checked = true, onCheckedChange = {}, comingSoon = true)
                QhathiaNavigationRow(
                    title = "Primeros pasos con tu tablet vitrina",
                    onClick = {},
                    comingSoon = true,
                    showDivider = false,
                )
            }
        }
    }
}
