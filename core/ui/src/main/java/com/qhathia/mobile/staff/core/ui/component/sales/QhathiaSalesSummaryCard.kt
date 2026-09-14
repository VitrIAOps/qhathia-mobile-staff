package com.qhathia.mobile.staff.core.ui.component.sales

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.staff.core.ui.R
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaColors
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme

private val CardRadius = 20.dp
private val CardPadding = 20.dp

/**
 * The dark sales summary card: the headline amount of the period in progress with its variation,
 * the bucket chart and the insight sentence. [header] renders at the top of the card — Inicio puts
 * its period tabs there; a screen that picks the period elsewhere passes nothing.
 *
 * The card's height is intrinsic rather than fixed, so the headline can grow with the system font
 * scale instead of being clipped. A null [model], or a window that sold nothing at all, gets the
 * empty treatment rather than a chart of flat bars.
 */
@Composable
fun QhathiaSalesSummaryCard(
    model: SalesSummaryCardModel?,
    modifier: Modifier = Modifier,
    header: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(QhathiaColors.summaryCard, RoundedCornerShape(CardRadius))
            .padding(CardPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
    ) {
        header()

        if (model == null || !model.hasSales) {
            Text(
                text = stringResource(R.string.sales_summary_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = QhathiaColors.onSummaryCardMuted,
            )
            return@Column
        }

        Text(
            text = stringResource(model.period.currentLabelRes),
            style = MaterialTheme.typography.bodySmall,
            color = QhathiaColors.onSummaryCardMuted,
        )

        Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)) {
            Text(
                text = model.total.toSolesLabel(),
                style = MaterialTheme.typography.headlineMedium,
                color = QhathiaColors.onSummaryCard,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // Null means the previous period sold nothing: say nothing rather than print "+100%".
            model.deltaPercent?.let { delta ->
                Text(
                    text = stringResource(model.period.deltaRes, delta.toSignedPercentLabel()),
                    style = MaterialTheme.typography.bodyMedium,
                    // Colour never carries the meaning alone — the sign is part of the text.
                    color = if (delta.percentTrend == PercentTrend.DOWN) {
                        MaterialTheme.colorScheme.error
                    } else {
                        QhathiaColors.success
                    },
                )
            }
        }

        QhathiaSalesBarChart(
            buckets = model.buckets,
            highlightedIndex = model.highlightedBucketIndex,
            totalLabel = model.total.toSolesLabel(),
        )
        QhathiaSalesInsightStrip(model = model)
    }
}

@Preview(name = "Summary card — light", showBackground = true)
@Preview(name = "Summary card — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun QhathiaSalesSummaryCardPreview() {
    QhathiaTheme {
        QhathiaSalesSummaryCard(
            model = SalesSummaryPreviewData.weekly,
            modifier = Modifier.padding(Dimens.SpaceMd),
        )
    }
}

@Preview(name = "Summary card, declining — light", showBackground = true)
@Preview(name = "Summary card, declining — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun QhathiaSalesSummaryCardDecliningPreview() {
    QhathiaTheme {
        QhathiaSalesSummaryCard(
            model = SalesSummaryPreviewData.declining,
            modifier = Modifier.padding(Dimens.SpaceMd),
        )
    }
}

@Preview(name = "Summary card, empty window — light", showBackground = true)
@Preview(name = "Summary card, empty window — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun QhathiaSalesSummaryCardEmptyPreview() {
    QhathiaTheme {
        QhathiaSalesSummaryCard(
            model = SalesSummaryPreviewData.empty,
            modifier = Modifier.padding(Dimens.SpaceMd),
        )
    }
}
