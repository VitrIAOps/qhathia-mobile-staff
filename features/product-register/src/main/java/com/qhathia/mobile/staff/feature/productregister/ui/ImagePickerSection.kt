package com.qhathia.mobile.staff.feature.productregister.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.qhathia.mobile.staff.core.domain.model.SelectedImage
import com.qhathia.mobile.staff.core.ui.component.PhotoTile
import com.qhathia.mobile.staff.core.ui.component.QhathiaPhotoStrip
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.feature.productregister.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * "Fotos del producto" section: title + subtitle over the shared photo strip, whose "Agregar" tile
 * offers capturing a new photo with the camera or choosing from the gallery.
 */
@Composable
fun ImagePickerSection(
    images: ImmutableList<SelectedImage>,
    onPick: (List<SelectedImage>) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    canAdd: Boolean = true,
) {
    val tiles = remember(images) { images.map { PhotoTile(key = it.uri, model = it.uri) }.toImmutableList() }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)) {
        Text(
            text = stringResource(R.string.register_photos_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.register_photos_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        QhathiaPhotoStrip(
            photos = tiles,
            onPick = { picked -> onPick(picked.map { SelectedImage(uri = it.uri, mimeType = it.mimeType) }) },
            onRemove = onRemove,
            enabled = enabled,
            canAdd = canAdd,
            modifier = Modifier.padding(top = Dimens.SpaceSm),
        )
    }
}
