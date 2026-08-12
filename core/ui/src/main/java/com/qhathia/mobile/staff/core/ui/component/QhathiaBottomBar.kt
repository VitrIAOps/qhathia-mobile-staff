package com.qhathia.mobile.staff.core.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qhathia.mobile.staff.core.ui.R
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme

private val BarElevation = 8.dp

/**
 * DS bottom navigation bar: a white surface lifted off the content by a soft upward shadow, holding
 * evenly-distributed [QhathiaBottomBarItem]s.
 *
 * Built on a plain `Row` rather than Material 3's `NavigationBar` because the DS has no selection
 * indicator pill and uses a 16dp glyph over a 14sp label — neither is reachable through
 * `NavigationBar`'s fixed 80dp item layout.
 */
@Composable
fun QhathiaBottomBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(BarElevation)
            .background(MaterialTheme.colorScheme.surface)
            .navigationBarsPadding()
            .padding(top = Dimens.SpaceSm, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/**
 * A single destination inside [QhathiaBottomBar]: glyph over label, both tinted by [selected] —
 * brand accent and bold when active, muted brand blue otherwise.
 */
@Composable
fun RowScope.QhathiaBottomBarItem(
    selected: Boolean,
    onClick: () -> Unit,
    @DrawableRes iconRes: Int,
    label: String,
    modifier: Modifier = Modifier,
) {
    val tint = if (selected) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.tertiary
    }
    Column(
        modifier = modifier
            .weight(1f)
            .semantics { this.selected = selected }
            .clickable(onClick = onClick)
            .padding(vertical = Dimens.SpaceXs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
    ) {
        Icon(
            painter = painterResource(iconRes),
            // The label right below names the destination; a content description would double it up.
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(Dimens.IconSm),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            letterSpacing = if (selected) 0.1.sp else 0.sp,
            color = tint,
            maxLines = 1,
        )
    }
}

@Preview(name = "Bottom bar — light", showBackground = true)
@Preview(name = "Bottom bar — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun QhathiaBottomBarPreview() {
    QhathiaTheme {
        QhathiaBottomBar {
            QhathiaBottomBarItem(false, {}, R.drawable.ic_nav_home, "Inicio")
            QhathiaBottomBarItem(true, {}, R.drawable.ic_nav_inventory, "Inventario")
            QhathiaBottomBarItem(false, {}, R.drawable.ic_nav_requests, "Solicitudes")
            QhathiaBottomBarItem(false, {}, R.drawable.ic_nav_profile, "Perfil")
        }
    }
}
