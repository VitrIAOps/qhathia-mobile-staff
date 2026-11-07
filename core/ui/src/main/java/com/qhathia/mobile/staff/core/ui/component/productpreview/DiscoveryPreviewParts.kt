package com.qhathia.mobile.staff.core.ui.component.productpreview

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.qhathia.mobile.staff.core.ui.R
import com.qhathia.mobile.staff.core.ui.format.toPenPrice
import kotlin.math.roundToInt

/**
 * A preview is drawn at its design size and then scaled up — to several times that when zoomed — so
 * photos are decoded larger than their slot to stay sharp, without ever loading the original file.
 */
private const val PHOTO_OVERSAMPLING = 3

/**
 * A square product photo cropped to its [slotSize], on a neutral block while it loads, when it
 * fails, or when there is no [model] at all. Decorative: the canvas describes the preview as a whole.
 */
@Composable
internal fun DiscoveryPhoto(
    model: String?,
    slotSize: Dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val sizePx = with(LocalDensity.current) { slotSize.toPx() }.roundToInt() * PHOTO_OVERSAMPLING
    val request = remember(model, sizePx) {
        ImageRequest.Builder(context)
            .data(model)
            .size(sizePx)
            .crossfade(true)
            .build()
    }
    val placeholder = remember { ColorPainter(DiscoveryPreviewPalette.Skeleton) }
    AsyncImage(
        model = request,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        placeholder = placeholder,
        error = placeholder,
        fallback = placeholder,
        modifier = modifier.background(DiscoveryPreviewPalette.Skeleton),
    )
}

/** The price as Discovery writes it, e.g. `S/ 380.00`. */
@Composable
internal fun previewPriceText(price: Double): String = price.toPenPrice()

/** A form value as entered, or the "not entered" placeholder when the merchant left it out. */
@Composable
internal fun String?.orMissingData(): String =
    if (isNullOrBlank()) stringResource(R.string.preview_missing_data) else this
