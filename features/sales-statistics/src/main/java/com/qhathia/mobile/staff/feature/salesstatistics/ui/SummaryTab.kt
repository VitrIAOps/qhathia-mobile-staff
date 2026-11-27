package com.qhathia.mobile.staff.feature.salesstatistics.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.qhathia.mobile.staff.core.domain.model.SalesSummary
import com.qhathia.mobile.staff.core.ui.component.QhathiaKpiTile
import com.qhathia.mobile.staff.core.ui.component.sales.QhathiaSalesSummaryCard
import com.qhathia.mobile.staff.core.ui.component.sales.toSolesLabel
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme
import com.qhathia.mobile.staff.feature.salesstatistics.R

/**
 * The Resumen tab: the shared dark summary card — with no period tabs of its own, the period lives
 * in "Filtros" — and the three KPI tiles of the period in progress.
 */
@Composable
internal fun SummaryTab(
    summary: SalesSummary,
    modifier: Modifier = Modifier,
) {
    val cardModel = remember(summary) { summary.toCardModel() }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
    ) {
        QhathiaSalesSummaryCard(model = cardModel)
        KpiTiles(summary)
    }
}

/**
 * The three indicators: side by side with one shared height, or stacked at the full width from font
 * scale 1.3, where three labels no longer fit a third of the phone each (F-84 a).
 */
@Composable
private fun KpiTiles(summary: SalesSummary) {
    val tiles: List<Triple<ImageVector, Int, String>> = listOf(
        Triple(Icons.Outlined.ShoppingBag, R.string.statistics_kpi_sales, summary.kpis.salesCount.toString()),
        Triple(Icons.Outlined.Receipt, R.string.statistics_kpi_ticket, summary.kpis.averageTicket.toSolesLabel()),
        Triple(Icons.Outlined.Inventory2, R.string.statistics_kpi_units, summary.kpis.unitsSold.toString()),
    )
    if (LocalDensity.current.fontScale >= StackKpiFontScale) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
            tiles.forEach { (icon, label, value) ->
                QhathiaKpiTile(icon, stringResource(label), value, Modifier.fillMaxWidth())
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
        ) {
            tiles.forEach { (icon, label, value) ->
                QhathiaKpiTile(icon, stringResource(label), value, Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
}

private const val StackKpiFontScale = 1.3f

@Preview(name = "Resumen tab — light", showBackground = true)
@Preview(name = "Resumen tab — dark", showBackground = true, uiMode = 0x20)
@Preview(name = "Resumen tab, font 1.3", showBackground = true, fontScale = 1.3f, widthDp = 360)
@Preview(name = "Resumen tab, font 2.0", showBackground = true, fontScale = 2f, widthDp = 360)
@Composable
private fun SummaryTabPreview() {
    QhathiaTheme {
        SummaryTab(
            summary = SalesStatisticsPreviewData.summary,
            modifier = Modifier.padding(Dimens.SpaceMd),
        )
    }
}
