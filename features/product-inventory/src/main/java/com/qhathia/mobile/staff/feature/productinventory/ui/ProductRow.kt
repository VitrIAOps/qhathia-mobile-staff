package com.qhathia.mobile.staff.feature.productinventory.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.qhathia.mobile.staff.core.domain.model.Product
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaColors
import com.qhathia.mobile.staff.feature.productinventory.R
import kotlin.math.abs

/**
 * A single inventory product (DS "Product-cards"): 59dp thumbnail, name over a price/stock row, and
 * a trailing overflow affordance. Tapping the row opens the product's detail/edit screen.
 * The "N stock" value is mocked via [mockedStock] until backend exposes a real stock quantity.
 */
@Composable
internal fun ProductRow(
    product: Product,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            // The 59dp thumbnail already pushes the row past the 48dp touch target.
            .clickable(onClickLabel = stringResource(R.string.inventory_open_detail_cd), onClick = onClick)
            .padding(Dimens.SpaceSm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
    ) {
        Box(
            modifier = Modifier
                .size(59.dp)
                .clip(RoundedCornerShape(Dimens.FieldRadius))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            if (product.primaryImageUrl == null) {
                // No photo registered yet — a neutral placeholder, not the broken-image error state.
                ThumbnailStateIcon(Icons.Filled.Image)
            } else {
                // SubcomposeAsyncImage renders distinct loading/error thumbnails (a centered, tinted
                // icon over the surfaceVariant background) instead of a stretched painter, and
                // crossfades the loaded photo in. Coil caches by URL, so it is fetched once.
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(product.primaryImageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = stringResource(R.string.inventory_product_image_cd),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = { ThumbnailStateIcon(Icons.Filled.Image) },
                    error = { ThumbnailStateIcon(Icons.Filled.BrokenImage) },
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
        ) {
            Text(
                text = product.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                // The DS lets a long product name wrap onto a second line before truncating —
                // brand + model rarely fits on one, and both halves identify the product.
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = stringResource(R.string.inventory_price_format, product.price),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.inventory_stock_format, mockedStock(product)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        // Visual-only for this slice: the DS row ends in a "⋮" that will open the per-product
        // actions menu once those actions exist.
        Icon(
            imageVector = Icons.Filled.MoreVert,
            contentDescription = null,
            tint = QhathiaColors.iconMuted,
            modifier = Modifier.size(Dimens.IconSm),
        )
    }
}

/** A centered, tinted icon used as the thumbnail's loading / error / no-image state. */
@Composable
private fun ThumbnailStateIcon(icon: ImageVector) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
    }
}

/**
 * Interim stock placeholder. `GET /api/v1/products` exposes no stock quantity yet; backend will add
 * it later. Derived deterministically from the product id so the value is stable across
 * recompositions. **Replace this single helper (and the DTO/domain field) when the real field
 * lands** — the row layout above does not change.
 */
internal fun mockedStock(product: Product): Int = (abs(product.id.hashCode()) % 90) + 10
