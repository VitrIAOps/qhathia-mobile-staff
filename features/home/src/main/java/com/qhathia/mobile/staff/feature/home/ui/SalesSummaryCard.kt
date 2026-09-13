package com.qhathia.mobile.staff.feature.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.qhathia.mobile.staff.core.domain.model.SalesPeriod
import com.qhathia.mobile.staff.core.domain.model.SalesSummary
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaColors
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme
import com.qhathia.mobile.staff.feature.home.R

private val CardRadius = 20.dp
private val CardPadding = 20.dp

/**
 * The "Resumen" card: the period selector, the headline amount for the period in progress with its
 * variation, the bucket chart and the insight sentence.
 *
 * The card's height is intrinsic rather than fixed, so the 32sp headline can grow with the system
 * font scale and push the card taller instead of being clipped by it. A window that sold nothing at
 * all gets the empty treatment rather than a chart of flat bars.
 */
@Composable
internal fun SalesSummaryCard(
    summary: SalesSummary?,
    selectedPeriod: SalesPeriod,
    onSelectPeriod: (SalesPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(QhathiaColors.summaryCard, RoundedCornerShape(CardRadius))
            .padding(CardPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
    ) {
        PeriodTabs(selectedPeriod = selectedPeriod, onSelectPeriod = onSelectPeriod)

        if (summary == null || !summary.hasSales) {
            Text(
                text = stringResource(R.string.home_summary_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = QhathiaColors.onSummaryCardMuted,
            )
            return@Column
        }

        Text(
            text = stringResource(summary.period.currentLabelRes),
            style = MaterialTheme.typography.bodySmall,
            color = QhathiaColors.onSummaryCardMuted,
        )

        Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)) {
            Text(
                text = summary.total.toSolesLabel(),
                style = MaterialTheme.typography.headlineMedium,
                color = QhathiaColors.onSummaryCard,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // Null means the previous period sold nothing: say nothing rather than print "+100%".
            summary.deltaPercent?.let { delta ->
                Text(
                    text = stringResource(summary.period.deltaRes, delta.toSignedPercentLabel()),
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

        SalesBarChart(
            buckets = summary.buckets,
            highlightedIndex = summary.highlightedBucketIndex,
            totalLabel = summary.total.toSolesLabel(),
        )
        SalesInsightStrip(summary = summary)
    }
}

/**
 * The period in progress is routinely at zero early on while the periods before it sold, and the
 * chart is still worth showing then. Only a window with no sales anywhere is empty.
 */
private val SalesSummary.hasSales: Boolean
    get() = total.signum() > 0 || buckets.any { it.amount.signum() > 0 }

@Preview(name = "Summary card — light", showBackground = true)
@Preview(name = "Summary card — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun SalesSummaryCardPreview() {
    QhathiaTheme {
        SalesSummaryCard(
            summary = HomePreviewData.summary,
            selectedPeriod = SalesPeriod.WEEKLY,
            onSelectPeriod = {},
            modifier = Modifier.padding(Dimens.SpaceMd),
        )
    }
}

@Preview(name = "Summary card, declining — light", showBackground = true)
@Preview(name = "Summary card, declining — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun SalesSummaryCardDecliningPreview() {
    QhathiaTheme {
        SalesSummaryCard(
            summary = HomePreviewData.decliningSummary,
            selectedPeriod = SalesPeriod.WEEKLY,
            onSelectPeriod = {},
            modifier = Modifier.padding(Dimens.SpaceMd),
        )
    }
}

@Preview(name = "Summary card, empty period — light", showBackground = true)
@Preview(name = "Summary card, empty period — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun SalesSummaryCardEmptyPreview() {
    QhathiaTheme {
        SalesSummaryCard(
            summary = HomePreviewData.emptySummary,
            selectedPeriod = SalesPeriod.HOURLY,
            onSelectPeriod = {},
            modifier = Modifier.padding(Dimens.SpaceMd),
        )
    }
}
