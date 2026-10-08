package com.qhathia.mobile.staff.core.ui.component.productpreview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.persistentListOf

/** Figma `644:26084`. */
internal val DiscoveryVerticalItemSize = DpSize(176.dp, 288.dp)

/** Figma `644:26105`. */
internal val DiscoveryHorizontalItemSize = DpSize(300.dp, 120.dp)

private val VerticalPhotoSize = 160.dp
private val HorizontalPhotoSize = 120.dp
private val CardRadius = 8.dp
private val CartButtonSize = 32.dp
private val CartIconSize = 16.dp

/**
 * The product as a card in Discovery's catalog grid (Figma `644:26084`): the primary photo, the
 * name over up to three lines, and the price beside the add-to-cart button. Drawn at its design
 * size; the button is part of the picture and does nothing.
 */
@Composable
internal fun DiscoveryVerticalItem(
    data: ProductPreviewData,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .size(DiscoveryVerticalItemSize)
            .clip(RoundedCornerShape(CardRadius))
            .background(DiscoveryPreviewPalette.Surface)
            .padding(PaddingValues(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 12.dp)),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DiscoveryPhoto(
            model = data.imageModels.firstOrNull(),
            slotSize = VerticalPhotoSize,
            modifier = Modifier
                .size(VerticalPhotoSize)
                .clip(RoundedCornerShape(CardRadius)),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = data.name,
                style = DiscoveryPreviewType.Body,
                color = DiscoveryPreviewPalette.TextBody,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = previewPriceText(data.price),
                    style = DiscoveryPreviewType.H3,
                    color = DiscoveryPreviewPalette.TextBody,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Box(
                    modifier = Modifier
                        .size(CartButtonSize)
                        .background(DiscoveryPreviewPalette.Navy, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.AddShoppingCart,
                        contentDescription = null,
                        tint = DiscoveryPreviewPalette.OnFilled,
                        modifier = Modifier.size(CartIconSize),
                    )
                }
            }
        }
    }
}

/**
 * The product as a row in Discovery's lists (Figma `644:26105`): the primary photo with the
 * category, the name and the price beside it. A category the merchant has not chosen shows the
 * "not entered" placeholder.
 */
@Composable
internal fun DiscoveryHorizontalItem(
    data: ProductPreviewData,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.size(DiscoveryHorizontalItemSize)) {
        DiscoveryPhoto(
            model = data.imageModels.firstOrNull(),
            slotSize = HorizontalPhotoSize,
            modifier = Modifier.size(HorizontalPhotoSize),
        )
        Column(
            modifier = Modifier
                .width(DiscoveryHorizontalItemSize.width - HorizontalPhotoSize)
                .fillMaxHeight()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = data.categoryName.orMissingData(),
                style = DiscoveryPreviewType.CategoryTag,
                color = DiscoveryPreviewPalette.Accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = data.name,
                style = DiscoveryPreviewType.Body,
                color = DiscoveryPreviewPalette.TextBody,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = previewPriceText(data.price),
                style = DiscoveryPreviewType.H3,
                color = DiscoveryPreviewPalette.TextBody,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

// ---- Previews ----

internal val PreviewSampleData = ProductPreviewData(
    name = "Taladro Percutor Bosch GSB 13 RE Professional 12V",
    price = 380.0,
    description = "El Bosch GSB 13 RE Professional es el taladro percutor con cable más compacto y " +
        "versátil de la gama azul Bosch. Diseñado para profesionales y usuarios exigentes, combina " +
        "potencia, ligereza y precisión en un solo equipo.",
    categoryName = "Taladros",
    stock = 22,
    imageModels = persistentListOf("content://sample/1", "content://sample/2", "content://sample/3"),
)

internal val PreviewSampleMinimalData = ProductPreviewData(
    name = "Taladro stilson 14\"",
    price = 3800.0,
    description = null,
    categoryName = null,
    stock = null,
    imageModels = persistentListOf("content://sample/1"),
)

@Preview(name = "Discovery vertical item", showBackground = true, backgroundColor = 0xFFF8F8F8)
@Composable
private fun DiscoveryVerticalItemPreview() {
    DiscoveryVerticalItem(data = PreviewSampleData, modifier = Modifier.padding(8.dp))
}

@Preview(name = "Discovery vertical item — minimal data", showBackground = true, backgroundColor = 0xFFF8F8F8)
@Composable
private fun DiscoveryVerticalItemMinimalPreview() {
    DiscoveryVerticalItem(data = PreviewSampleMinimalData, modifier = Modifier.padding(8.dp))
}

@Preview(name = "Discovery horizontal item", showBackground = true, backgroundColor = 0xFFF8F8F8)
@Composable
private fun DiscoveryHorizontalItemPreview() {
    DiscoveryHorizontalItem(data = PreviewSampleData)
}

@Preview(name = "Discovery horizontal item — no category", showBackground = true, backgroundColor = 0xFFF8F8F8)
@Composable
private fun DiscoveryHorizontalItemMissingPreview() {
    DiscoveryHorizontalItem(data = PreviewSampleMinimalData)
}
