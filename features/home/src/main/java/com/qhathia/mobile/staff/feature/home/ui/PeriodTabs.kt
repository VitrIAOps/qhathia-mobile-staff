package com.qhathia.mobile.staff.feature.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qhathia.mobile.staff.core.domain.model.SalesPeriod
import com.qhathia.mobile.staff.core.ui.component.sales.selectorLabel
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaColors
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme

private val TrackRadius = 10.dp
private val PillRadius = 8.dp

/**
 * The Mensual / Semanal / Diario / Por hora selector at the top of the summary card: four pills on
 * a light track, the active one filled in the brand accent.
 *
 * The pills are tabs in one group, so a screen reader announces "Semanal, tab, selected"; at large
 * font sizes their labels shrink and then switch to the short form ("Sem.").
 */
@Composable
internal fun PeriodTabs(
    selectedPeriod: SalesPeriod,
    onSelectPeriod: (SalesPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(QhathiaColors.periodTabTrack, RoundedCornerShape(TrackRadius))
            .padding(Dimens.SpaceXs)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        SalesPeriod.entries.forEach { period ->
            PeriodTab(
                period = period,
                selected = period == selectedPeriod,
                onClick = { onSelectPeriod(period) },
            )
        }
    }
}

@Composable
private fun RowScope.PeriodTab(
    period: SalesPeriod,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val style = MaterialTheme.typography.bodySmall
    Box(
        modifier = Modifier
            .weight(1f)
            .background(
                color = if (selected) QhathiaColors.brandAccentSoft else Color.Transparent,
                shape = RoundedCornerShape(PillRadius),
            )
            // A tab in the pills' group, so a screen reader says which period is selected.
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(vertical = Dimens.SpaceSm),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = period.toUi().selectorLabel(),
            style = style,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) {
                MaterialTheme.colorScheme.onSecondary
            } else {
                QhathiaColors.onPeriodTab
            },
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = style.fontSize),
        )
    }
}

@Preview(name = "Period tabs — light", showBackground = true, backgroundColor = 0xFF10283C)
@Preview(name = "Period tabs — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun PeriodTabsPreview() {
    QhathiaTheme {
        PeriodTabs(
            selectedPeriod = SalesPeriod.WEEKLY,
            onSelectPeriod = {},
            modifier = Modifier.padding(Dimens.SpaceMd),
        )
    }
}
