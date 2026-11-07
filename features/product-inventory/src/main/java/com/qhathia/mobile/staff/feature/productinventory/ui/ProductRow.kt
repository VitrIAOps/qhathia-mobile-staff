package com.qhathia.mobile.staff.feature.productinventory.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.qhathia.mobile.staff.core.domain.model.Product
import com.qhathia.mobile.staff.core.ui.format.toPenPrice
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaColors
import com.qhathia.mobile.staff.feature.productinventory.R

/**
 * A single inventory product (DS "Product-cards"): 59dp thumbnail, name over a price/stock row, and
 * a trailing "⋮" that opens the per-product actions menu. Tapping the row itself opens the
 * product's detail/edit screen, the same destination as the menu's "Editar producto".
 *
 * [hasDiscount] is only known once the menu has been opened ([onActionsOpened] triggers the
 * lookup): it picks between "Registrar descuento" and "Editar descuento".
 *
 * Stock and discounted price are shown exactly as the products API sends them: an unknown stock
 * shows nothing, and the discounted price is never computed here.
 */
@Composable
internal fun ProductRow(
    product: Product,
    onClick: () -> Unit,
    onEditDiscount: () -> Unit,
    onDelete: () -> Unit,
    onActionsOpened: () -> Unit,
    hasDiscount: Boolean?,
    isDiscountLookupLoading: Boolean,
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
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
                verticalAlignment = Alignment.Bottom,
            ) {
                ProductPrice(
                    price = product.price,
                    discountedPrice = product.discountedPrice,
                    modifier = Modifier.weight(1f),
                )
                stockLabel(product.stock)?.let { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        ProductRowActions(
            onEditProduct = onClick,
            onEditDiscount = onEditDiscount,
            onDelete = onDelete,
            onOpened = onActionsOpened,
            hasDiscount = hasDiscount,
            isDiscountLookupLoading = isDiscountLookupLoading,
        )
    }
}

/**
 * The row's "⋮" button and the menu it anchors, so the options are drawn from the icon itself.
 * Whether the menu is open is plain UI state and stays here.
 *
 * The discount option reads "Registrar descuento" when the product is known to have none and
 * "Editar descuento" otherwise; while that is being looked up it shows a spinner and waits.
 * "Eliminar producto" comes last, past a divider and in the error colour: it cannot be undone.
 */
@Composable
private fun ProductRowActions(
    onEditProduct: () -> Unit,
    onEditDiscount: () -> Unit,
    onDelete: () -> Unit,
    onOpened: () -> Unit,
    hasDiscount: Boolean?,
    isDiscountLookupLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        IconButton(
            onClick = {
                expanded = true
                onOpened()
            },
        ) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.inventory_row_actions_cd),
                tint = QhathiaColors.iconMuted,
                modifier = Modifier.size(Dimens.IconSm),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(Dimens.CardRadius),
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.inventory_action_edit_product)) },
                leadingIcon = { Icon(imageVector = Icons.Outlined.Edit, contentDescription = null) },
                onClick = {
                    expanded = false
                    onEditProduct()
                },
            )
            DropdownMenuItem(
                text = {
                    Text(
                        stringResource(
                            if (hasDiscount == false) {
                                R.string.inventory_action_register_discount
                            } else {
                                R.string.inventory_action_edit_discount
                            },
                        ),
                    )
                },
                leadingIcon = { Icon(imageVector = Icons.Outlined.LocalOffer, contentDescription = null) },
                trailingIcon = {
                    if (isDiscountLookupLoading) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(Dimens.IconSm),
                        )
                    }
                },
                enabled = !isDiscountLookupLoading,
                onClick = {
                    expanded = false
                    onEditDiscount()
                },
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.inventory_action_delete_product),
                        color = MaterialTheme.colorScheme.error,
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                },
                onClick = {
                    expanded = false
                    onDelete()
                },
            )
        }
    }
}

/**
 * The row's price. With a discount in effect the original price is struck through in a light
 * weight, so it reads as a reference, and the server's [discountedPrice] follows it styled exactly
 * like a plain price; the two wrap onto a second line rather than squeeze the stock label. Screen readers get one sentence
 * naming both prices instead of two bare numbers.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProductPrice(price: Double, discountedPrice: Double?, modifier: Modifier = Modifier) {
    if (discountedPrice == null) {
        Text(
            text = price.toPenPrice(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = modifier,
        )
        return
    }
    val description = stringResource(R.string.inventory_price_discounted_cd, price.toPenPrice(), discountedPrice.toPenPrice())
    FlowRow(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
        itemVerticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = price.toPenPrice(),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textDecoration = TextDecoration.LineThrough,
        )
        Text(
            text = discountedPrice.toPenPrice(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * "N stock", or "Sin existencias" at zero so a stock-out does not read as a small number, or
 * nothing at all when the API does not know the stock.
 */
@Composable
private fun stockLabel(stock: Int?): String? = when (stock) {
    null -> null
    0 -> stringResource(R.string.inventory_out_of_stock)
    else -> stringResource(R.string.inventory_stock_format, stock)
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
