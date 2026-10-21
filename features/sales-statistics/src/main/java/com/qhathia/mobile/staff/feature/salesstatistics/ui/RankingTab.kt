package com.qhathia.mobile.staff.feature.salesstatistics.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.staff.core.domain.model.SalesRanking
import com.qhathia.mobile.staff.core.domain.model.StalledProduct
import com.qhathia.mobile.staff.core.domain.model.TopSellingProduct
import com.qhathia.mobile.staff.core.ui.component.sales.toSolesLabel
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaColors
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme
import com.qhathia.mobile.staff.feature.salesstatistics.R

private val SummaryBoxRadius = 8.dp
private val SummaryBoxPadding = 9.dp
private val TrendIconSize = 32.dp

/**
 * The Ranking tab: "Lo que más vende" and "Lo que necesita atención". Rows are plain items inside
 * the screen's scrolling column — the lists are short and never scroll on their own.
 */
@Composable
internal fun RankingTab(
    ranking: SalesRanking,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
    ) {
        RankingCard(
            title = stringResource(R.string.statistics_top_sellers_title),
            totalLabel = ranking.topSellersRevenue.toSolesLabel(),
            caption = pluralStringResource(
                R.plurals.statistics_top_sellers_caption,
                ranking.topSellers.size,
                ranking.topSellers.size,
            ),
            accent = QhathiaColors.success,
            trendIcon = Icons.AutoMirrored.Filled.TrendingUp,
            emptyMessage = stringResource(R.string.statistics_top_sellers_empty),
            isEmpty = ranking.topSellers.isEmpty(),
        ) {
            ranking.topSellers.forEachIndexed { index, product ->
                if (index > 0) HorizontalDivider(color = QhathiaColors.rowDivider)
                key(product.id) { TopSellerRow(position = index + 1, product = product) }
            }
        }

        RankingCard(
            title = stringResource(R.string.statistics_needs_attention_title),
            totalLabel = ranking.immobilizedTotal.toSolesLabel(),
            // Immobilized stock, not revenue — and only the listed products' share of it.
            caption = when (
                val caption = needsAttentionCaption(
                    listed = ranking.needsAttention.size,
                    totalCount = ranking.needsAttentionTotalCount,
                )
            ) {
                is NeedsAttentionCaption.All -> pluralStringResource(
                    R.plurals.statistics_needs_attention_caption,
                    caption.listed,
                    caption.listed,
                )
                is NeedsAttentionCaption.Partial -> pluralStringResource(
                    R.plurals.statistics_needs_attention_caption_partial,
                    caption.total,
                    caption.listed,
                    caption.total,
                )
            },
            accent = MaterialTheme.colorScheme.error,
            trendIcon = Icons.AutoMirrored.Filled.TrendingDown,
            emptyMessage = stringResource(R.string.statistics_needs_attention_empty),
            isEmpty = ranking.needsAttention.isEmpty(),
        ) {
            ranking.needsAttention.forEachIndexed { index, product ->
                if (index > 0) HorizontalDivider(color = QhathiaColors.rowDivider)
                // The list is ordered by immobilized money, so the first row is the one to flag.
                key(product.id) { StalledRow(product = product, critical = index == 0) }
            }
        }
    }
}

@Composable
private fun RankingCard(
    title: String,
    totalLabel: String,
    caption: String,
    accent: Color,
    trendIcon: ImageVector,
    emptyMessage: String,
    isEmpty: Boolean,
    modifier: Modifier = Modifier,
    rows: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(Dimens.CardRadius))
            .padding(Dimens.SpaceMd),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        // An empty card says only that it is empty: a zero total, a trend glyph and a caption
        // counting zero products would contradict the empty message.
        if (isEmpty) {
            Text(
                text = emptyMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = Dimens.SpaceSm),
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, accent, RoundedCornerShape(SummaryBoxRadius))
                    .padding(SummaryBoxPadding)
                    .semantics(mergeDescendants = true) {},
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = trendIcon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(TrendIconSize),
                )
                Text(
                    text = totalLabel,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                    maxLines = 1,
                )
                Text(
                    text = caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = accent,
                    modifier = Modifier.weight(1f),
                )
            }
            Column { rows() }
        }
    }
}

@Composable
private fun TopSellerRow(position: Int, product: TopSellingProduct) {
    val units = pluralStringResource(R.plurals.statistics_units_sold, product.unitsSold, product.unitsSold)
    val amount = product.revenue.toSolesLabel()
    RankingProductRow(
        name = product.name,
        imageUrl = product.imageUrl,
        detail = units,
        amount = amount,
        contentDescription = stringResource(
            R.string.statistics_top_seller_row_cd,
            position,
            product.name,
            units,
            amount,
        ),
        leading = { RankBadge(position = position) },
    )
}

@Composable
private fun StalledRow(product: StalledProduct, critical: Boolean) {
    val units = pluralStringResource(R.plurals.statistics_units_unsold, product.stock, product.stock)
    val amount = product.immobilizedAmount.toSolesLabel()
    RankingProductRow(
        name = product.name,
        imageUrl = product.imageUrl,
        detail = units,
        amount = amount,
        contentDescription = stringResource(R.string.statistics_stalled_row_cd, product.name, units, amount),
        leading = { AttentionGlyph(critical = critical) },
    )
}

@Preview(name = "Ranking tab — light", showBackground = true, heightDp = 900)
@Preview(name = "Ranking tab — dark", showBackground = true, uiMode = 0x20, heightDp = 900)
@Composable
private fun RankingTabPreview() {
    QhathiaTheme {
        RankingTab(
            ranking = SalesStatisticsPreviewData.ranking,
            modifier = Modifier.padding(Dimens.SpaceMd),
        )
    }
}

@Preview(name = "Ranking tab, empty — light", showBackground = true)
@Preview(name = "Ranking tab, empty — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun RankingTabEmptyPreview() {
    QhathiaTheme {
        RankingTab(
            ranking = SalesStatisticsPreviewData.emptyRanking,
            modifier = Modifier.padding(Dimens.SpaceMd),
        )
    }
}
