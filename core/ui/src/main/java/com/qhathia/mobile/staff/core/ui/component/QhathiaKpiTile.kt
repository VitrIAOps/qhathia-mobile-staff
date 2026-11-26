package com.qhathia.mobile.staff.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme

private val TileRadius = 8.dp
private val TileIconSize = 24.dp

/** The figure shrinks to this before it would stop fitting the tile. */
private val MinValueSize = 14.sp

/**
 * A small white tile with a glyph, a caption and one figure — "Ventas 74". The tile is read as
 * a single node, so a screen reader says the caption and the figure together.
 */
@Composable
fun QhathiaKpiTile(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(TileRadius))
            .padding(Dimens.SpaceSm)
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            // The caption beside it names the figure.
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(TileIconSize),
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                // Two lines, never "Cantidad de …": the row's tiles share one height (S17-03).
                maxLines = 2,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
                autoSize = TextAutoSize.StepBased(minFontSize = MinValueSize, maxFontSize = MaterialTheme.typography.titleLarge.fontSize),
            )
        }
    }
}

@Preview(name = "KPI tile — light", showBackground = true)
@Preview(name = "KPI tile — dark", showBackground = true, uiMode = 0x20)
@Preview(name = "KPI tile, narrow font 1.3", showBackground = true, fontScale = 1.3f, widthDp = 120)
@Composable
private fun QhathiaKpiTilePreview() {
    QhathiaTheme {
        QhathiaKpiTile(
            icon = Icons.Outlined.ShoppingBag,
            label = "Cantidad de ventas",
            value = "74",
            modifier = Modifier.padding(Dimens.SpaceMd),
        )
    }
}
