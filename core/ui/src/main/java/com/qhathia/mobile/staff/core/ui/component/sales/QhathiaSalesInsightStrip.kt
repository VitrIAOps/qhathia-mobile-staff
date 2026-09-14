package com.qhathia.mobile.staff.core.ui.component.sales

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
import com.qhathia.mobile.staff.core.ui.R
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaColors
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme

private val StripRadius = 8.dp

/**
 * The one-sentence read on a window, under the chart: which bucket did best, and by how much against
 * the bucket before it. It quotes the bucket the chart highlights, so the picture and the prose
 * agree by construction. Nothing renders when there is no best bucket.
 */
@Composable
fun QhathiaSalesInsightStrip(
    model: SalesSummaryCardModel,
    modifier: Modifier = Modifier,
) {
    val bucket = model.highlightedBucket ?: return
    val sentence = insightSentence(
        period = model.period,
        bucketLabel = bucket.label,
        amountLabel = bucket.amount.toSolesLabel(),
        deltaPercent = model.highlightedDeltaPercent,
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
            painter = painterResource(R.drawable.ic_star),
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
    period: SalesPeriodUi,
    bucketLabel: String,
    amountLabel: String,
    deltaPercent: Double?,
): String {
    if (deltaPercent == null) {
        return stringResource(R.string.sales_insight_no_comparison, bucketLabel, amountLabel)
    }
    val previous = stringResource(period.previousRes)
    val comparison = when (deltaPercent.percentTrend) {
        PercentTrend.UP -> stringResource(
            R.string.sales_comparison_up,
            deltaPercent.toPercentMagnitudeLabel(),
            previous,
        )
        PercentTrend.DOWN -> stringResource(
            R.string.sales_comparison_down,
            deltaPercent.toPercentMagnitudeLabel(),
            previous,
        )
        PercentTrend.FLAT -> stringResource(R.string.sales_comparison_flat, previous)
    }
    return stringResource(R.string.sales_insight_with_comparison, bucketLabel, amountLabel, comparison)
}

@Preview(name = "Insight strip — light", showBackground = true)
@Preview(name = "Insight strip — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun QhathiaSalesInsightStripPreview() {
    QhathiaTheme {
        Box(
            modifier = Modifier
                .background(QhathiaColors.summaryCard)
                .padding(Dimens.SpaceMd),
        ) {
            QhathiaSalesInsightStrip(model = SalesSummaryPreviewData.weekly)
        }
    }
}

@Preview(name = "Insight strip, no comparison — light", showBackground = true)
@Preview(name = "Insight strip, no comparison — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun QhathiaSalesInsightStripNoComparisonPreview() {
    QhathiaTheme {
        Box(
            modifier = Modifier
                .background(QhathiaColors.summaryCard)
                .padding(Dimens.SpaceMd),
        ) {
            QhathiaSalesInsightStrip(
                model = SalesSummaryPreviewData.weekly.copy(highlightedDeltaPercent = null),
            )
        }
    }
}
