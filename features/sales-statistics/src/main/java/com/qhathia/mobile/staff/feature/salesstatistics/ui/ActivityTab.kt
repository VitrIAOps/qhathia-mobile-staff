package com.qhathia.mobile.staff.feature.salesstatistics.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TabletAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.staff.core.domain.model.SalesPeriod
import com.qhathia.mobile.staff.core.domain.model.StoreActivity
import com.qhathia.mobile.staff.core.ui.component.sales.PercentTrend
import com.qhathia.mobile.staff.core.ui.component.sales.percentTrend
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaColors
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme
import com.qhathia.mobile.staff.feature.salesstatistics.R

private val FigureIconSize = 24.dp
private val PillIconSize = 12.dp
private val DividerHeight = 44.dp

/**
 * The Actividad tab: customers who used the tablet and products they looked at, each with its
 * variation. Zeros are shown as zeros — a tablet that emits no events is a reading, not a failure.
 */
@Composable
internal fun ActivityTab(
    activity: StoreActivity,
    period: SalesPeriod,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(Dimens.CardRadius))
            .padding(Dimens.SpaceMd),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.TabletAndroid,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = stringResource(R.string.statistics_activity_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() },
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .background(MaterialTheme.colorScheme.background, RoundedCornerShape(12.dp))
                .padding(Dimens.SpaceMd),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ActivityFigure(
                icon = Icons.Outlined.PersonOutline,
                value = activity.uniqueSessions,
                label = stringResource(R.string.statistics_activity_customers),
                caption = stringResource(R.string.statistics_activity_customers_caption),
                deltaPercent = activity.uniqueSessionsDeltaPercent,
                period = period,
                modifier = Modifier.weight(1f),
            )
            VerticalDivider(
                modifier = Modifier.height(DividerHeight),
                color = QhathiaColors.rowDivider,
            )
            ActivityFigure(
                icon = Icons.Outlined.Search,
                value = activity.productViews,
                label = stringResource(R.string.statistics_activity_products),
                caption = stringResource(R.string.statistics_activity_products_caption),
                deltaPercent = activity.productViewsDeltaPercent,
                period = period,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ActivityFigure(
    icon: ImageVector,
    value: Int,
    label: String,
    caption: String,
    deltaPercent: Double?,
    period: SalesPeriod,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(FigureIconSize),
            )
            Column {
                Text(
                    text = value.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = caption,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        // No base to compare against: no pill, rather than a variation that did not happen.
        deltaPercent?.let { DeltaPill(deltaPercent = it, period = period) }
    }
}

/** "+ 12% vs semana pasada" in success colours, or in error colours when the figure fell. */
@Composable
private fun DeltaPill(deltaPercent: Double, period: SalesPeriod) {
    val fell = deltaPercent.percentTrend == PercentTrend.DOWN
    val container = if (fell) MaterialTheme.colorScheme.errorContainer else QhathiaColors.successContainer
    val content = if (fell) MaterialTheme.colorScheme.onErrorContainer else QhathiaColors.onSuccessContainer
    Row(
        modifier = Modifier
            .background(container, RoundedCornerShape(Dimens.SpaceSm))
            .padding(horizontal = Dimens.SpaceSm, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (fell) Icons.AutoMirrored.Filled.TrendingDown else Icons.AutoMirrored.Filled.TrendingUp,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(PillIconSize),
        )
        Text(
            text = stringResource(period.activityDeltaRes, deltaPercent.toSpacedSignedPercentLabel()),
            style = MaterialTheme.typography.labelSmall,
            color = content,
            maxLines = 1,
        )
    }
}

@Preview(name = "Actividad tab — light", showBackground = true)
@Preview(name = "Actividad tab — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun ActivityTabPreview() {
    QhathiaTheme {
        ActivityTab(
            activity = SalesStatisticsPreviewData.activity,
            period = SalesPeriod.WEEKLY,
            modifier = Modifier.padding(Dimens.SpaceMd),
        )
    }
}
