package com.qhathia.mobile.staff.core.ui.component.productpreview

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.staff.core.ui.R

/** Figma `644:26093` — Discovery runs on a landscape kiosk screen. */
internal val DiscoveryProductDetailSize = DpSize(1440.dp, 900.dp)

private val FrameRadius = 16.dp
private val ToolbarWidth = 1360.dp
private val ToolbarTop = 39.dp
private val BodyWidth = 1208.dp
private val BodyTop = 111.dp
private val GalleryPhotoSize = 347.6.dp
private val ThumbnailSize = 86.9.dp
private val GalleryGap = 11.587.dp
private val ThumbnailBorder = 1.086.dp
private val ButtonHeight = 48.dp
private val ButtonRadius = 8.dp
private val ButtonIconSize = 16.dp
private val QuantityBoxSize = 34.dp
private val SectionDotSize = 14.dp
private val RelatedBlur = 6.dp
private val MicButtonSlot = 104.dp
private val MicButtonSize = 57.dp

/** Only the photos that fit the gallery's height are worth composing. */
private const val MAX_THUMBNAILS = 4
private const val RELATED_ITEM_COUNT = 4

/** `Modifier.blur` is a no-op before Android 12. */
private val isBlurSupported: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/**
 * Discovery's product detail screen (Figma `644:26093`), drawn at its landscape design size: the
 * gallery, the product's name, availability, price and description, the purchase controls, the
 * "you could pair this with" row and the assistant prompt. Every control is part of the picture and
 * does nothing.
 *
 * A description or stock the merchant has not entered shows the "not entered" placeholder. The
 * related products are not real — they are placeholders obscured so they cannot be mistaken for a
 * suggestion: blurred where the device can ([blurRelated]), drawn as blank blocks where it cannot.
 */
@Composable
internal fun DiscoveryProductDetail(
    data: ProductPreviewData,
    modifier: Modifier = Modifier,
    blurRelated: Boolean = isBlurSupported,
) {
    Box(
        modifier = modifier
            .size(DiscoveryProductDetailSize)
            .clip(RoundedCornerShape(FrameRadius))
            .background(DiscoveryPreviewPalette.Background),
    ) {
        DetailToolbar(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = ToolbarTop)
                .width(ToolbarWidth),
        )
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = BodyTop)
                .width(BodyWidth),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            SectionHeading(stringResource(R.string.preview_discovery_detail_heading))
            Column(verticalArrangement = Arrangement.spacedBy(40.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(39.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DetailGallery(imageModels = data.imageModels)
                    DetailInfo(data = data, modifier = Modifier.weight(1f))
                }
                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    SectionHeading(stringResource(R.string.preview_discovery_related_heading))
                    RelatedProducts(data = data, blurred = blurRelated)
                }
            }
        }
        AssistantPrompt(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
        )
    }
}

@Composable
private fun DetailToolbar(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DiscoveryButton(
                text = stringResource(R.string.preview_discovery_back),
                contentColor = DiscoveryPreviewPalette.Navy,
                leadingIcon = Icons.AutoMirrored.Filled.ArrowBack,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(64.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DiscoveryButton(
                    text = stringResource(R.string.preview_discovery_new_search),
                    contentColor = DiscoveryPreviewPalette.Navy,
                    trailingIcon = Icons.Filled.Refresh,
                )
                DiscoveryButton(
                    text = stringResource(R.string.preview_discovery_go_to_cart),
                    contentColor = DiscoveryPreviewPalette.OnFilled,
                    containerColor = DiscoveryPreviewPalette.Accent,
                    trailingIcon = Icons.Filled.ShoppingCart,
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(DiscoveryPreviewPalette.ToolbarBorder),
        )
    }
}

/** A Discovery button: 48dp tall, optionally filled, with a 16dp glyph on either side of its label. */
@Composable
private fun DiscoveryButton(
    text: String,
    contentColor: Color,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Transparent,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
) {
    Row(
        modifier = modifier
            .height(ButtonHeight)
            .background(containerColor, RoundedCornerShape(ButtonRadius))
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leadingIcon?.let { ButtonGlyph(it, contentColor) }
        Text(text = text, style = DiscoveryPreviewType.Button, color = contentColor, maxLines = 1)
        trailingIcon?.let { ButtonGlyph(it, contentColor) }
    }
}

@Composable
private fun ButtonGlyph(icon: ImageVector, tint: Color) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(ButtonIconSize),
    )
}

@Composable
private fun SectionHeading(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(SectionDotSize)
                .background(DiscoveryPreviewPalette.Accent, CircleShape),
        )
        Text(
            text = text,
            style = DiscoveryPreviewType.SectionHeading,
            color = DiscoveryPreviewPalette.TextHeading,
            maxLines = 1,
        )
    }
}

/** The primary photo beside a column of thumbnails, the first one outlined as the selected photo. */
@Composable
private fun DetailGallery(
    imageModels: List<String>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(GalleryPhotoSize)
            .clipToBounds(),
        horizontalArrangement = Arrangement.spacedBy(GalleryGap),
    ) {
        DiscoveryPhoto(
            model = imageModels.firstOrNull(),
            slotSize = GalleryPhotoSize,
            modifier = Modifier.size(GalleryPhotoSize),
        )
        // Taller than the gallery when full, exactly as designed: the last thumbnail is cut off.
        Column(
            modifier = Modifier.wrapContentHeight(align = Alignment.Top, unbounded = true),
            verticalArrangement = Arrangement.spacedBy(GalleryGap),
        ) {
            imageModels.take(MAX_THUMBNAILS).forEachIndexed { index, model ->
                DiscoveryPhoto(
                    model = model,
                    slotSize = ThumbnailSize,
                    modifier = Modifier
                        .size(ThumbnailSize)
                        .then(
                            if (index == 0) {
                                Modifier.border(ThumbnailBorder, DiscoveryPreviewPalette.ToolbarBorder)
                            } else {
                                Modifier
                            },
                        ),
                )
            }
        }
    }
}

@Composable
private fun DetailInfo(
    data: ProductPreviewData,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = data.name,
                style = DiscoveryPreviewType.H3,
                color = DiscoveryPreviewPalette.TextHeading,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            StockLine(stock = data.stock)
            Text(
                text = previewPriceText(data.price),
                style = DiscoveryPreviewType.H1,
                color = DiscoveryPreviewPalette.TextHeading,
                maxLines = 1,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(DiscoveryPreviewPalette.Divider),
            )
            Text(
                text = data.description.orMissingData(),
                style = DiscoveryPreviewType.Body,
                color = DiscoveryPreviewPalette.TextSecondary,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            QuantityStepper()
            DiscoveryButton(
                text = stringResource(R.string.preview_discovery_add_to_cart),
                contentColor = DiscoveryPreviewPalette.OnFilled,
                containerColor = DiscoveryPreviewPalette.Navy,
                trailingIcon = Icons.Filled.AddShoppingCart,
            )
            DiscoveryButton(
                text = stringResource(R.string.preview_discovery_share),
                contentColor = DiscoveryPreviewPalette.OnFilled,
                containerColor = DiscoveryPreviewPalette.Success,
                trailingIcon = Icons.Filled.Share,
            )
        }
    }
}

/** "22 disponibles" in green, or the placeholder in a neutral tone when no stock was entered. */
@Composable
private fun StockLine(stock: Int?, modifier: Modifier = Modifier) {
    val color = if (stock != null) DiscoveryPreviewPalette.Success else DiscoveryPreviewPalette.TextPlaceholder
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape),
        )
        Text(
            text = if (stock != null) {
                pluralStringResource(R.plurals.preview_stock_available, stock, stock)
            } else {
                stringResource(R.string.preview_missing_data)
            },
            style = DiscoveryPreviewType.Body,
            color = color,
            maxLines = 1,
        )
    }
}

@Composable
private fun QuantityStepper(modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(QuantityBoxSize)
                .background(DiscoveryPreviewPalette.Surface),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = null,
                tint = DiscoveryPreviewPalette.Danger,
                modifier = Modifier.size(ButtonIconSize),
            )
        }
        Box(
            modifier = Modifier.size(width = 64.dp, height = QuantityBoxSize),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.preview_discovery_quantity),
                style = DiscoveryPreviewType.Body,
                color = DiscoveryPreviewPalette.TextStrong,
            )
        }
        Box(
            modifier = Modifier
                .size(QuantityBoxSize)
                .background(DiscoveryPreviewPalette.Surface),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.preview_discovery_quantity_plus),
                style = DiscoveryPreviewType.Body,
                color = DiscoveryPreviewPalette.NavyDark,
            )
        }
    }
}

/**
 * Stand-ins for the products Discovery would suggest. They reuse the previewed product only to fill
 * the row, so they are never left readable.
 */
@Composable
private fun RelatedProducts(
    data: ProductPreviewData,
    blurred: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(RELATED_ITEM_COUNT) {
            if (blurred) {
                DiscoveryHorizontalItem(data = data, modifier = Modifier.blur(RelatedBlur))
            } else {
                RelatedProductSkeleton()
            }
        }
    }
}

@Composable
private fun RelatedProductSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .size(DiscoveryHorizontalItemSize)
            .alpha(0.6f),
    ) {
        Box(
            modifier = Modifier
                .size(DiscoveryHorizontalItemSize.height)
                .background(DiscoveryPreviewPalette.Skeleton),
        )
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SkeletonLine(width = 48.dp, height = 10.dp)
            SkeletonLine(width = 132.dp, height = 14.dp)
            SkeletonLine(width = 96.dp, height = 20.dp)
        }
    }
}

@Composable
private fun SkeletonLine(width: Dp, height: Dp) {
    Box(
        modifier = Modifier
            .size(width = width, height = height)
            .background(DiscoveryPreviewPalette.Skeleton, RoundedCornerShape(4.dp)),
    )
}

/** The assistant strip pinned to the bottom of the screen: a question, its input and the mic. */
@Composable
private fun AssistantPrompt(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(horizontal = 64.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Lightbulb,
                contentDescription = null,
                tint = DiscoveryPreviewPalette.Accent,
                modifier = Modifier.size(32.dp),
            )
            Text(
                text = stringResource(R.string.preview_discovery_project_question),
                style = DiscoveryPreviewType.H3,
                color = DiscoveryPreviewPalette.TextStrong,
                maxLines = 1,
            )
        }
        Box(
            modifier = Modifier
                .height(44.dp)
                .background(DiscoveryPreviewPalette.Surface, RoundedCornerShape(ButtonRadius))
                .border(1.dp, DiscoveryPreviewPalette.InputBorder, RoundedCornerShape(ButtonRadius))
                .padding(start = 16.dp, end = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = stringResource(R.string.preview_discovery_project_placeholder),
                style = DiscoveryPreviewType.Body,
                color = DiscoveryPreviewPalette.TextPlaceholder,
                maxLines = 1,
            )
        }
        Box(modifier = Modifier.size(MicButtonSlot), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(MicButtonSize)
                    .background(DiscoveryPreviewPalette.Accent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Mic,
                    contentDescription = null,
                    tint = DiscoveryPreviewPalette.OnFilled,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

// ---- Previews ----

@Preview(name = "Discovery detail", widthDp = 1440, heightDp = 900)
@Composable
private fun DiscoveryProductDetailPreview() {
    DiscoveryProductDetail(data = PreviewSampleData, blurRelated = true)
}

@Preview(name = "Discovery detail — minimal data, no blur", widthDp = 1440, heightDp = 900)
@Composable
private fun DiscoveryProductDetailMinimalPreview() {
    DiscoveryProductDetail(data = PreviewSampleMinimalData, blurRelated = false)
}
