package com.qhathia.mobile.staff.core.ui.component.sales

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.staff.core.ui.R
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaColors
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme
import kotlinx.collections.immutable.ImmutableList
import java.math.BigDecimal

/** Height of the tallest bar, matching the design's 62px column. */
private val MaxBarHeight = 62.dp

/** No bucket ever collapses to nothing: a near-zero month still reads as a bar. */
private val MinBarHeight = 6.dp

private val BarTopRadius = 8.dp

/**
 * A window's buckets as a row of bars, each labelled with its amount above and its name below.
 *
 * It is a Compose layout rather than a `Canvas`: the labels stay real `Text` nodes, so they scale
 * with the system font size and stay reachable by tests. The whole chart is announced as one node —
 * four unlabelled bars read aloud individually tell a screen-reader user nothing.
 */
@Composable
fun QhathiaSalesBarChart(
    buckets: ImmutableList<SalesBucketModel>,
    highlightedIndex: Int,
    totalLabel: String,
    modifier: Modifier = Modifier,
) {
    if (buckets.isEmpty()) return
    val maxAmount = buckets.maxOf { it.amount }
    val highlighted = buckets.getOrNull(highlightedIndex)
    val chartDescription = stringResource(
        R.string.sales_chart_cd,
        totalLabel,
        highlighted?.label.orEmpty(),
        highlighted?.amount?.toSolesLabel().orEmpty(),
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = chartDescription },
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
        verticalAlignment = Alignment.Bottom,
    ) {
        buckets.forEachIndexed { index, bucket ->
            SalesBar(
                bucket = bucket,
                maxAmount = maxAmount,
                highlighted = index == highlightedIndex,
            )
        }
    }
}

@Composable
private fun RowScope.SalesBar(
    bucket: SalesBucketModel,
    maxAmount: BigDecimal,
    highlighted: Boolean,
) {
    val fraction = if (maxAmount.signum() <= 0) {
        0f
    } else {
        (bucket.amount.toFloat() / maxAmount.toFloat()).coerceIn(0f, 1f)
    }
    val barHeight = (MaxBarHeight * fraction).coerceAtLeast(MinBarHeight)

    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
    ) {
        Text(
            text = bucket.amount.toSolesLabel(),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (highlighted) FontWeight.SemiBold else FontWeight.Normal,
            color = if (highlighted) {
                MaterialTheme.colorScheme.secondary
            } else {
                QhathiaColors.onSummaryCard
            },
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .background(
                    color = if (highlighted) {
                        QhathiaColors.brandAccentSoft
                    } else {
                        QhathiaColors.summaryBar
                    },
                    shape = RoundedCornerShape(topStart = BarTopRadius, topEnd = BarTopRadius),
                ),
        )
        Text(
            text = bucket.label,
            style = MaterialTheme.typography.bodySmall,
            color = QhathiaColors.onSummaryCard,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

@Preview(name = "Sales chart — light", showBackground = true, backgroundColor = 0xFF10283C)
@Preview(name = "Sales chart — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun QhathiaSalesBarChartPreview() {
    QhathiaTheme {
        Box(modifier = Modifier.background(QhathiaColors.summaryCard).padding(Dimens.SpaceMd)) {
            QhathiaSalesBarChart(
                buckets = SalesSummaryPreviewData.weekly.buckets,
                highlightedIndex = SalesSummaryPreviewData.weekly.highlightedBucketIndex,
                totalLabel = SalesSummaryPreviewData.weekly.total.toSolesLabel(),
            )
        }
    }
}

/** One tiny bucket next to a large one — the case the minimum bar height exists for. */
@Preview(name = "Sales chart, lopsided — light", showBackground = true, backgroundColor = 0xFF10283C)
@Preview(name = "Sales chart, lopsided — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun QhathiaSalesBarChartLopsidedPreview() {
    QhathiaTheme {
        Box(modifier = Modifier.background(QhathiaColors.summaryCard).padding(Dimens.SpaceMd)) {
            QhathiaSalesBarChart(
                buckets = SalesSummaryPreviewData.lopsidedBuckets,
                highlightedIndex = 3,
                totalLabel = "S/ 1,712",
            )
        }
    }
}
