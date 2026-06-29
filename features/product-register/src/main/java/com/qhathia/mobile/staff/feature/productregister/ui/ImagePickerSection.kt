package com.qhathia.mobile.staff.feature.productregister.ui

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import com.qhathia.mobile.staff.core.domain.model.SelectedImage
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.feature.productregister.R
import kotlinx.collections.immutable.ImmutableList
import java.io.File

private val TILE_SIZE = 92.dp

/**
 * "Fotos del producto" section: title + subtitle, an "Agregar" tile and JPG/PNG photo previews.
 * Tapping "Agregar" offers capturing a new photo with the camera or choosing from the gallery.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagePickerSection(
    images: ImmutableList<SelectedImage>,
    onPick: (List<SelectedImage>) -> Unit,
    onRemove: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val context = LocalContext.current
    var showSourceChooser by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(),
    ) { uris ->
        if (uris.isNotEmpty()) {
            onPick(
                uris.map { uri ->
                    SelectedImage(
                        uri = uri.toString(),
                        mimeType = context.contentResolver.getType(uri).orEmpty(),
                    )
                },
            )
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            onPick(listOf(SelectedImage(uri = uri.toString(), mimeType = "image/jpeg")))
        }
        pendingCameraUri = null
    }

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
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(top = Dimens.SpaceSm),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
        ) {
            AddPhotoTile(enabled = enabled, onClick = { showSourceChooser = true })
            images.forEach { image ->
                ImageThumbnail(image = image, onRemove = { onRemove(image.uri) }, enabled = enabled)
            }
        }
    }

    if (showSourceChooser) {
        ModalBottomSheet(onDismissRequest = { showSourceChooser = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Dimens.SpaceLg),
            ) {
                Text(
                    text = stringResource(R.string.register_photo_source_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = Dimens.SpaceLg, vertical = Dimens.SpaceSm),
                )
                PhotoSourceRow(
                    icon = Icons.Filled.PhotoCamera,
                    label = stringResource(R.string.register_photo_source_camera),
                    onClick = {
                        showSourceChooser = false
                        val uri = createCameraImageUri(context)
                        pendingCameraUri = uri
                        cameraLauncher.launch(uri)
                    },
                )
                PhotoSourceRow(
                    icon = Icons.Filled.PhotoLibrary,
                    label = stringResource(R.string.register_photo_source_gallery),
                    onClick = {
                        showSourceChooser = false
                        galleryLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
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
            .size(TILE_SIZE)
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
            .clickable(enabled = enabled, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = stringResource(R.string.register_add_image_cd),
            tint = accent,
            modifier = Modifier.size(28.dp),
        )
        Text(
            text = stringResource(R.string.register_add_image),
            style = MaterialTheme.typography.labelMedium,
            color = accent,
        )
    }
}

@Composable
private fun ImageThumbnail(
    image: SelectedImage,
    onRemove: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.size(TILE_SIZE)) {
        AsyncImage(
            model = image.uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(TILE_SIZE)
                .clip(RoundedCornerShape(Dimens.FieldRadius)),
        )
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(percent = 50),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(Dimens.SpaceXs),
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.register_remove_image_cd),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .size(20.dp)
                    .clickable(enabled = enabled, onClick = onRemove),
            )
        }
    }
}

/** Creates a temp cache file and returns a FileProvider content URI for the camera to write into. */
private fun createCameraImageUri(context: Context): Uri {
    val file = File.createTempFile("camera_", ".jpg", context.cacheDir)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
