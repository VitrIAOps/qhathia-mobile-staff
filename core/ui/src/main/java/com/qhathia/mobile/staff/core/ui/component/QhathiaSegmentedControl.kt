package com.qhathia.mobile.staff.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaColors
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

private val TrackRadius = 10.dp
private val SegmentRadius = 8.dp

/**
 * Equal-width options on a rounded track, the selected one filled in the brand accent — the DS tab
 * row used for "Resumen / Ranking / Actividad" and for the period pills inside "Filtros".
 *
 * Each option is a `Tab`-role selectable inside a selectable group, so screen readers announce the
 * position and the selected state. [trackColor] switches between the neutral track on the screen
 * background ([QhathiaColors.segmentTrack]) and the light blue one inside a card
 * ([QhathiaColors.filterTrack]).
 */
@Composable
fun <T> QhathiaSegmentedControl(
    options: ImmutableList<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    trackColor: Color = QhathiaColors.segmentTrack,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(trackColor, RoundedCornerShape(TrackRadius))
            .padding(Dimens.SpaceXs)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(SegmentRadius))
                    .background(if (isSelected) QhathiaColors.brandAccentSoft else Color.Transparent)
                    .selectable(
                        selected = isSelected,
                        role = Role.Tab,
                        onClick = { onSelect(option) },
                    )
                    .padding(vertical = Dimens.SpaceSm),
                contentAlignment = Alignment.Center,
            ) {
                val style = MaterialTheme.typography.bodyMedium
                Text(
                    text = label(option),
                    style = style,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onSecondary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = style.fontSize),
                )
            }
        }
    }
}

@Preview(name = "Segmented control — light", showBackground = true)
@Preview(name = "Segmented control — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun QhathiaSegmentedControlPreview() {
    QhathiaTheme {
        QhathiaSegmentedControl(
            options = persistentListOf("Resumen", "Ranking", "Actividad"),
            selected = "Ranking",
            onSelect = {},
            label = { it },
            modifier = Modifier.padding(Dimens.SpaceMd),
        )
    }
}
