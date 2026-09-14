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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
        ) {
            QhathiaKpiTile(
                icon = Icons.Outlined.ShoppingBag,
                label = stringResource(R.string.statistics_kpi_sales),
                value = summary.kpis.salesCount.toString(),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            QhathiaKpiTile(
                icon = Icons.Outlined.Receipt,
                label = stringResource(R.string.statistics_kpi_ticket),
                value = summary.kpis.averageTicket.toSolesLabel(),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            QhathiaKpiTile(
                icon = Icons.Outlined.Inventory2,
                label = stringResource(R.string.statistics_kpi_units),
                value = summary.kpis.unitsSold.toString(),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
    }
}

@Preview(name = "Resumen tab — light", showBackground = true)
@Preview(name = "Resumen tab — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun SummaryTabPreview() {
    QhathiaTheme {
        SummaryTab(
            summary = SalesStatisticsPreviewData.summary,
            modifier = Modifier.padding(Dimens.SpaceMd),
        )
    }
}
