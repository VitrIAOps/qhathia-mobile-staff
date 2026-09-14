package com.qhathia.mobile.staff.feature.salesstatistics.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Size
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaColors
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme

private val ThumbnailSize = 48.dp
private val LeadingSize = 24.dp
private val RowSpacing = 12.dp

/**
 * One product row of a ranking card: a leading marker (position badge or alert glyph), the
 * thumbnail, the name with its detail line, and the amount on the right.
 *
 * The row is announced as a single node from [contentDescription], so a screen reader reads
 * "1. Taladro percutor Bosch GSB, 34 vendidos, S/ 1,870" instead of four loose fragments.
 */
@Composable
internal fun RankingProductRow(
    name: String,
    imageUrl: String?,
    detail: String,
    amount: String,
    contentDescription: String,
    leading: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { this.contentDescription = contentDescription }
            .padding(vertical = RowSpacing),
        horizontalArrangement = Arrangement.spacedBy(RowSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(LeadingSize), contentAlignment = Alignment.Center) {
            leading()
        }
        ProductThumbnail(imageUrl = imageUrl)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        Text(
            text = amount,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}

/** The green 1-based position circle of a top seller. */
@Composable
internal fun RankBadge(position: Int) {
    Box(
        modifier = Modifier
            .size(LeadingSize)
            .background(QhathiaColors.successContainer, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = position.toString(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = QhathiaColors.onSuccessContainer,
        )
    }
}

/** The "!" marker of stalled stock: error colour for the most money tied up, warning for the rest. */
@Composable
internal fun AttentionGlyph(critical: Boolean) {
    Icon(
        imageVector = Icons.Outlined.ErrorOutline,
        contentDescription = null,
        tint = if (critical) MaterialTheme.colorScheme.error else QhathiaColors.onWarningContainer,
        modifier = Modifier.size(LeadingSize),
    )
}

/**
 * The product photo, sized explicitly so Coil never decodes a bitmap larger than the slot. A
 * missing or failed image falls back to the neutral placeholder rather than a blank hole.
 */
@Composable
private fun ProductThumbnail(imageUrl: String?) {
    Box(
        modifier = Modifier
            .size(ThumbnailSize)
            .clip(RoundedCornerShape(Dimens.SpaceSm))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (imageUrl.isNullOrBlank()) return@Box
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .size(Size(THUMBNAIL_PX, THUMBNAIL_PX))
                .build(),
            // The product name sits right beside it; a description would double it up.
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(ThumbnailSize),
        )
    }
}

/** Two pixels per dp covers xhdpi without decoding more than the slot can show. */
private const val THUMBNAIL_PX = 96

@Preview(name = "Ranking row — light", showBackground = true)
@Preview(name = "Ranking row — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun RankingProductRowPreview() {
    QhathiaTheme {
        val product = SalesStatisticsPreviewData.ranking.topSellers.first()
        RankingProductRow(
            name = product.name,
            imageUrl = product.imageUrl,
            detail = "34 vendidos",
            amount = "S/ 1,870",
            contentDescription = "",
            leading = { RankBadge(position = 1) },
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = Dimens.SpaceMd),
        )
    }
}
