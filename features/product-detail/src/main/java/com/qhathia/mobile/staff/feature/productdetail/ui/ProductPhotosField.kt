package com.qhathia.mobile.staff.feature.productdetail.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import com.qhathia.mobile.staff.core.domain.model.SelectedImage
import com.qhathia.mobile.staff.core.ui.component.PhotoTile
import com.qhathia.mobile.staff.core.ui.component.QhathiaPhotoStrip
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.feature.productdetail.R
import com.qhathia.mobile.staff.feature.productdetail.model.FormPhoto
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * The product's photos on the edit form: the shared strip (saved photos and pending picks, primary
 * first) plus a hint when a limit applies — "Máximo 10 fotos" when full, and "El producto necesita
 * al menos una foto" when only one is left and it cannot be removed. A legacy product with no photos
 * keeps its explanatory placeholder above the "Agregar" tile.
 */
@Composable
internal fun ProductPhotosField(
    photos: ImmutableList<FormPhoto>,
    canAdd: Boolean,
    canRemove: Boolean,
    enabled: Boolean,
    onPick: (List<SelectedImage>) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tiles = remember(photos) { photos.map { PhotoTile(key = it.key, model = it.model) }.toImmutableList() }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
        if (photos.isEmpty()) EmptyPhotos()
        QhathiaPhotoStrip(
            photos = tiles,
            onPick = { picked -> onPick(picked.map { SelectedImage(uri = it.uri, mimeType = it.mimeType) }) },
            onRemove = onRemove,
            enabled = enabled,
            canAdd = canAdd,
            canRemove = canRemove,
        )
        val hint = when {
            !canAdd -> R.string.detail_photos_max_hint
            photos.size == 1 && !canRemove -> R.string.detail_photos_min_hint
            else -> null
        }
        hint?.let {
            Text(
                text = stringResource(it),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
