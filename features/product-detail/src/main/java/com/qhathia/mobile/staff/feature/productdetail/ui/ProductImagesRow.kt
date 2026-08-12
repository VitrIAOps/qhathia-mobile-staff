package com.qhathia.mobile.staff.feature.productdetail.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Image
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
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.feature.productdetail.R
import kotlinx.collections.immutable.ImmutableList

private val TileSize = 92.dp

/**
 * Read-only gallery of the product's registered photos, in backend order (primary first). Adding and
 * removing photos is not part of this slice, so the tiles carry no affordances; a product with no
 * media shows an explanatory placeholder instead of an empty strip.
 */
@Composable
internal fun ProductImagesRow(
    imageUrls: ImmutableList<String>,
    modifier: Modifier = Modifier,
) {
    if (imageUrls.isEmpty()) {
        EmptyPhotos(modifier)
        return
    }
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
    ) {
        items(imageUrls) { url ->
            Box(
                modifier = Modifier
                    .size(TileSize)
                    .clip(RoundedCornerShape(Dimens.FieldRadius))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(url)
                        .crossfade(true)
                        .build(),
                    contentDescription = stringResource(R.string.detail_photo_cd),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = { TileStateIcon(Icons.Filled.Image) },
                    error = { TileStateIcon(Icons.Filled.BrokenImage) },
                )
            }
        }
    }
}

@Composable
private fun EmptyPhotos(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.FieldRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(Dimens.SpaceMd),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.detail_photos_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A centered, tinted icon used as a tile's loading / error state. */
@Composable
private fun TileStateIcon(icon: ImageVector) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
    }
}
