package com.qhathia.mobile.staff.core.ui.component

import androidx.compose.foundation.layout.sizeIn
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil3.compose.SubcomposeAsyncImage
import com.qhathia.mobile.staff.core.ui.R
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.io.File

private val TileSize = 92.dp

/** The backend's per-product photo limit, the most a single pick can bring. */
private const val MAX_PICK = 10

/**
 * One photo in a [QhathiaPhotoStrip]. [key] identifies it for removal; [model] is anything Coil can
 * load — a remote URL for a saved photo, a content uri for one just picked.
 */
@Immutable
data class PhotoTile(
    val key: String,
    val model: String,
)

/** A photo the merchant just picked: its content [uri] and resolved [mimeType] (may be empty). */
@Immutable
data class PickedPhoto(
    val uri: String,
    val mimeType: String,
)

/**
 * The product photo strip shared by registration and editing: the photos, each with a remove
 * control, followed by an "Agregar" tile. "Agregar" offers taking a photo with the camera or choosing
 * from the gallery. The strip only reports picks and removals; which ones are accepted is the
 * caller's decision.
 *
 * The gallery never lets the merchant pick more than [remainingSlots] (a single picker for one slot,
 * since the multiple picker needs at least two). "Agregar" stays last and the strip scrolls to it when
 * photos are added, so it is never left out of view (UX-C-03).
 *
 * [canAdd] disables the tile (e.g. at the photo limit) and [canRemove] hides the remove controls
 * (e.g. on the last photo); [enabled] locks the whole strip while the form is busy.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QhathiaPhotoStrip(
    photos: ImmutableList<PhotoTile>,
    onPick: (List<PickedPhoto>) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    canAdd: Boolean = true,
    canRemove: Boolean = true,
    remainingSlots: Int = MAX_PICK,
) {
    val context = LocalContext.current
    var showSourceChooser by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val report: (List<Uri>) -> Unit = { uris ->
        if (uris.isNotEmpty()) {
            onPick(
                uris.map { uri ->
                    PickedPhoto(uri = uri.toString(), mimeType = context.contentResolver.getType(uri).orEmpty())
                },
            )
        }
    }
    // The contract carries the limit, so it is rebuilt when the remaining slots change.
    val multipleContract = remember(remainingSlots) {
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = remainingSlots.coerceAtLeast(2))
    }
    val galleryLauncher = rememberLauncherForActivityResult(multipleContract) { uris -> report(uris) }
    val singleGalleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> report(listOfNotNull(uri)) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            onPick(listOf(PickedPhoto(uri = uri.toString(), mimeType = "image/jpeg")))
        }
        pendingCameraUri = null
    }

    val scrollState = rememberScrollState()
    // Photos just added push "Agregar" right: follow it so it stays in view.
    var shownCount by remember { mutableIntStateOf(photos.size) }
    LaunchedEffect(photos.size) {
        if (photos.size > shownCount) scrollState.animateScrollTo(scrollState.maxValue)
        shownCount = photos.size
    }
    Row(
        modifier = modifier.horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
    ) {
        photos.forEachIndexed { index, photo ->
            PhotoThumbnail(
                photo = photo,
                position = index + 1,
                total = photos.size,
                showRemove = canRemove,
                enabled = enabled,
                onRemove = { onRemove(photo.key) },
            )
        }
        AddPhotoTile(enabled = enabled && canAdd, onClick = { showSourceChooser = true })
    }

    if (showSourceChooser) {
        ModalBottomSheet(onDismissRequest = { showSourceChooser = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Dimens.SpaceLg),
            ) {
                Text(
                    text = stringResource(R.string.photo_source_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = Dimens.SpaceLg, vertical = Dimens.SpaceSm),
                )
                PhotoSourceRow(
                    icon = Icons.Filled.PhotoCamera,
                    label = stringResource(R.string.photo_source_camera),
                    onClick = {
                        showSourceChooser = false
                        val uri = createCameraImageUri(context)
                        pendingCameraUri = uri
                        cameraLauncher.launch(uri)
                    },
                )
                PhotoSourceRow(
                    icon = Icons.Filled.PhotoLibrary,
                    label = stringResource(R.string.photo_source_gallery),
                    onClick = {
                        showSourceChooser = false
                        val request = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        if (remainingSlots <= 1) singleGalleryLauncher.launch(request) else galleryLauncher.launch(request)
                    },
                )
            }
        }
    }
}

@Composable
private fun PhotoSourceRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.SpaceLg, vertical = Dimens.SpaceMd),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun AddPhotoTile(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.secondary
    Column(
        modifier = modifier
            // A minimum, not a fixed size: at large text "Agregar" grew out of the tile (T-6).
            .sizeIn(minWidth = TileSize, minHeight = TileSize)
            // A disabled tile (busy form, or the photo limit) reads as unavailable, not just inert.
            .alpha(if (enabled) 1f else 0.38f)
            .clip(RoundedCornerShape(Dimens.FieldRadius))
            .background(accent.copy(alpha = 0.08f))
            .drawBehind {
                drawRoundRect(
                    color = accent,
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f),
                    ),
                    cornerRadius = CornerRadius(Dimens.FieldRadius.toPx()),
                )
            }
            .clickable(enabled = enabled, onClick = onClick)
            .padding(Dimens.SpaceXs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = stringResource(R.string.photo_add_cd),
            tint = accent,
            modifier = Modifier.size(28.dp),
        )
        Text(
            text = stringResource(R.string.photo_add),
            style = MaterialTheme.typography.labelMedium,
            color = accent,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun PhotoThumbnail(
    photo: PhotoTile,
    position: Int,
    total: Int,
    showRemove: Boolean,
    enabled: Boolean,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // "Foto 1 de 3, principal": the first photo is the one the inventory and the tablet show.
    val description = stringResource(R.string.photo_position_cd, position, total) +
        if (position == 1) stringResource(R.string.photo_main_suffix_cd) else ""
    Box(modifier = modifier.size(TileSize)) {
        SubcomposeAsyncImage(
            model = photo.model,
            contentDescription = description,
            contentScale = ContentScale.Crop,
            loading = { TileStateIcon(Icons.Filled.Image) },
            error = { TileStateIcon(Icons.Filled.BrokenImage) },
            modifier = Modifier
                .size(TileSize)
                .clip(RoundedCornerShape(Dimens.FieldRadius))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        if (showRemove) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(percent = 50),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(Dimens.SpaceXs),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.photo_remove_cd),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(enabled = enabled, onClick = onRemove),
                )
            }
        }
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

/** Creates a temp cache file and returns a FileProvider content URI for the camera to write into. */
private fun createCameraImageUri(context: Context): Uri {
    val file = File.createTempFile("camera_", ".jpg", context.cacheDir)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

private val PreviewPhotos = persistentListOf(
    PhotoTile(key = "m1", model = "https://cdn.qhathia.test/1.jpg"),
    PhotoTile(key = "m2", model = "https://cdn.qhathia.test/2.jpg"),
)

@Preview(name = "Photo strip", showBackground = true)
@Preview(name = "Photo strip dark", showBackground = true, uiMode = 0x20)
@Composable
private fun QhathiaPhotoStripPreview() {
    QhathiaTheme {
        QhathiaPhotoStrip(photos = PreviewPhotos, onPick = {}, onRemove = {})
    }
}

@Preview(name = "Photo strip — empty", showBackground = true)
@Composable
private fun QhathiaPhotoStripEmptyPreview() {
    QhathiaTheme {
        QhathiaPhotoStrip(photos = persistentListOf(), onPick = {}, onRemove = {})
    }
}

// At the limit the tile is disabled; with a single photo it has no remove control.
@Preview(name = "Photo strip — limit, last photo", showBackground = true)
@Preview(name = "Photo strip — limit, last photo dark", showBackground = true, uiMode = 0x20)
@Composable
private fun QhathiaPhotoStripLockedPreview() {
    QhathiaTheme {
        QhathiaPhotoStrip(
            photos = persistentListOf(PreviewPhotos.first()),
            onPick = {},
            onRemove = {},
            canAdd = false,
            canRemove = false,
        )
    }
}
