package com.qhathia.mobile.staff.feature.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.staff.core.domain.model.SalesPeriod
import com.qhathia.mobile.staff.core.domain.model.SalesSummary
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaColors
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme
import com.qhathia.mobile.staff.feature.home.R
import com.qhathia.mobile.staff.core.ui.R as CoreUiR

private val StripRadius = 8.dp

/**
 * The one-sentence read on the period, under the chart: which bucket did best, and by how much
 * against the bucket before it. It quotes the bucket the chart highlights, so the picture and the
 * prose agree by construction.
 */
@Composable
internal fun SalesInsightStrip(
    summary: SalesSummary,
    modifier: Modifier = Modifier,
) {
    val bucket = summary.highlightedBucket ?: return
    val sentence = insightSentence(
        period = summary.period,
        bucketLabel = bucket.label,
        amountLabel = bucket.amount.toSolesLabel(),
        deltaPercent = summary.highlightedDeltaPercent,
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(QhathiaColors.insightStrip, RoundedCornerShape(StripRadius))
            .padding(horizontal = Dimens.SpaceMd, vertical = Dimens.SpaceSm),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(CoreUiR.drawable.ic_star),
            // The sentence beside it says the same thing; a description would double it up.
            contentDescription = null,
            tint = QhathiaColors.success,
            modifier = Modifier.size(Dimens.SpaceLg),
        )
        Text(
            text = sentence,
            style = MaterialTheme.typography.bodyMedium,
            color = QhathiaColors.onSummaryCard,
        )
    }
}

/**
 * "Sem 13 generó S/ 1,690, 24% más que la semana anterior." The comparison is dropped when the best
 * bucket has no preceding bucket with sales — a variation against nothing is not a fact.
 */
@Composable
private fun insightSentence(
    period: SalesPeriod,
    bucketLabel: String,
    amountLabel: String,
    deltaPercent: Double?,
): String {
    if (deltaPercent == null) {
        return stringResource(R.string.home_insight_no_comparison, bucketLabel, amountLabel)
    }
    val previous = stringResource(period.previousRes)
    val comparison = when (deltaPercent.percentTrend) {
        PercentTrend.UP -> stringResource(
            R.string.home_comparison_up,
            deltaPercent.toPercentMagnitudeLabel(),
            previous,
        )
        PercentTrend.DOWN -> stringResource(
            R.string.home_comparison_down,
            deltaPercent.toPercentMagnitudeLabel(),
            previous,
        )
        PercentTrend.FLAT -> stringResource(R.string.home_comparison_flat, previous)
    }
    return stringResource(R.string.home_insight_with_comparison, bucketLabel, amountLabel, comparison)
}

@Preview(name = "Insight strip — light", showBackground = true)
@Preview(name = "Insight strip — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun SalesInsightStripPreview() {
    QhathiaTheme {
        Box(
            modifier = Modifier
                .background(QhathiaColors.summaryCard)
                .padding(Dimens.SpaceMd),
        ) {
            SalesInsightStrip(summary = HomePreviewData.summary)
        }
    }
}

@Preview(name = "Insight strip, no comparison — light", showBackground = true)
@Preview(name = "Insight strip, no comparison — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun SalesInsightStripNoComparisonPreview() {
    QhathiaTheme {
        Box(
            modifier = Modifier
                .background(QhathiaColors.summaryCard)
                .padding(Dimens.SpaceMd),
        ) {
            SalesInsightStrip(
                summary = HomePreviewData.summary.copy(highlightedDeltaPercent = null),
            )
        }
    }
}
