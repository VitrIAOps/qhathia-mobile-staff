package com.qhathia.mobile.staff.feature.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Size
import com.qhathia.mobile.staff.core.domain.model.RestockSuggestion
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaColors
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme
import com.qhathia.mobile.staff.feature.home.R
import com.qhathia.mobile.staff.core.ui.R as CoreUiR

/** Card and thumbnail sizes from the design; the row scrolls rather than stretching them. */
internal val RestockCardWidth = 303.dp
private val ThumbnailSize = 104.dp
private val BodyWidth = 167.dp
private val ChipIconSize = 12.dp

/**
 * A product that is about to run out: its thumbnail, its name, how little is left, and a
 * `Reabastecer` action.
 *
 * [onRestock] is deliberately a no-op today: the suggestion carries the real product id, but there
 * is no restock flow for it to open yet.
 */
@Composable
internal fun RestockProductCard(
    suggestion: RestockSuggestion,
    onRestock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .width(RestockCardWidth)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(Dimens.SpaceSm))
            .padding(start = Dimens.SpaceSm, end = Dimens.SpaceSm)
            .padding(top = Dimens.SpaceSm, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProductThumbnail(imageUrl = suggestion.imageUrl)

        Column(
            modifier = Modifier.width(BodyWidth),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
        ) {
            Text(
                text = suggestion.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            RemainingStockChip(remainingStock = suggestion.remainingStock)
            SuggestionActionButton(
                text = stringResource(R.string.home_restock_product),
                onClick = onRestock,
            )
        }
    }
}

/**
 * The product photo, sized explicitly so Coil never decodes a bitmap larger than the 104dp slot.
 * A missing or failed image falls back to the neutral placeholder rather than a blank hole.
 */
@Composable
private fun ProductThumbnail(
    imageUrl: String?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(ThumbnailSize)
            .clip(RoundedCornerShape(Dimens.SpaceSm))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (imageUrl.isNullOrBlank()) return@Box
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .size(Size(ThumbnailSize.value.toInt(), ThumbnailSize.value.toInt()))
                .build(),
            // The product name sits right beside it; a description would double it up.
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(ThumbnailSize),
        )
    }
}

/**
 * "Solo queda 1" / "Solo quedan 3" — the plural is part of the warning, not an afterthought. An
 * exhausted product reads "Sin existencias": "Solo quedan 0" would soften a stock-out into a
 * low-stock warning.
 */
@Composable
private fun RemainingStockChip(
    remainingStock: Int,
    modifier: Modifier = Modifier,
) {
    val text = if (remainingStock <= 0) {
        stringResource(R.string.home_out_of_stock)
    } else {
        pluralStringResource(R.plurals.home_remaining_stock, remainingStock, remainingStock)
    }
    Row(
        modifier = modifier
            .background(QhathiaColors.stockAlertContainer, RoundedCornerShape(Dimens.SpaceSm))
            .padding(horizontal = Dimens.SpaceSm, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(CoreUiR.drawable.ic_alert),
            // The chip text beside it carries the meaning.
            contentDescription = null,
            tint = QhathiaColors.onStockAlertContainer,
            modifier = Modifier.size(ChipIconSize),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = QhathiaColors.onStockAlertContainer,
            maxLines = 1,
        )
    }
}

@Preview(name = "Restock card — light", showBackground = true)
@Preview(name = "Restock card — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun RestockProductCardPreview() {
    QhathiaTheme {
        RestockProductCard(
            suggestion = HomePreviewData.restockProducts.first(),
            onRestock = {},
            modifier = Modifier.padding(Dimens.SpaceMd),
        )
    }
}

@Preview(name = "Restock card, plural — light", showBackground = true)
@Preview(name = "Restock card, plural — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun RestockProductCardPluralPreview() {
    QhathiaTheme {
        RestockProductCard(
            suggestion = HomePreviewData.restockProducts[1],
            onRestock = {},
            modifier = Modifier.padding(Dimens.SpaceMd),
        )
    }
}

@Preview(name = "Restock card, out of stock — light", showBackground = true)
@Preview(name = "Restock card, out of stock — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun RestockProductCardOutOfStockPreview() {
    QhathiaTheme {
        RestockProductCard(
            suggestion = HomePreviewData.restockProducts.first().copy(remainingStock = 0),
            onRestock = {},
            modifier = Modifier.padding(Dimens.SpaceMd),
        )
    }
}
