package com.qhathia.mobile.staff.core.ui.component.productpreview

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.StayCurrentLandscape
import androidx.compose.material.icons.filled.StayCurrentPortrait
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.qhathia.mobile.staff.core.ui.R
import com.qhathia.mobile.staff.core.ui.component.QhathiaSegmentedControl
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaColors
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme
import kotlinx.collections.immutable.persistentListOf

/** Which Discovery screen the dialog is showing the product on. */
internal enum class ProductPreviewTab { Item, Detail }

/** Which of the two catalog cards the Item tab shows. */
internal enum class ProductPreviewOrientation { Vertical, Horizontal }

private val PreviewTabs = persistentListOf(ProductPreviewTab.Item, ProductPreviewTab.Detail)
private val PreviewOrientations =
    persistentListOf(ProductPreviewOrientation.Vertical, ProductPreviewOrientation.Horizontal)

private const val LOCKED_ALPHA = 0.5f

/** Breathing room around a catalog card, so it is not drawn edge to edge on the canvas. */
private val ItemCanvasMargin = 24.dp

/**
 * Full-screen preview of how a product will look to shoppers in Qhathia Discovery, drawn from the
 * values currently on a product form ([data]) — nothing is saved to show it.
 *
 * Two tabs: **Item**, the catalog card in its vertical or horizontal layout, and **Detalle**, the
 * product detail screen, which only exists in landscape. Each preview behaves like a picture: it can
 * be zoomed and dragged but nothing in it responds to taps. The tab and the orientation survive a
 * device rotation; the close action and system back both call [onDismiss].
 */
@Composable
fun QhathiaProductPreviewDialog(
    data: ProductPreviewData,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var tab by rememberSaveable { mutableStateOf(ProductPreviewTab.Item) }
    var orientation by rememberSaveable { mutableStateOf(ProductPreviewOrientation.Vertical) }
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        ProductPreviewDialogContent(
            data = data,
            tab = tab,
            orientation = orientation,
            isLandscape = isLandscape,
            onTabSelect = { tab = it },
            onOrientationSelect = { orientation = it },
            onDismiss = onDismiss,
            modifier = modifier,
        )
    }
}

/**
 * The dialog's stateless body. In portrait the controls stack above the canvas; in landscape, where
 * height is what is scarce, they share a single row with the title.
 */
@Composable
internal fun ProductPreviewDialogContent(
    data: ProductPreviewData,
    tab: ProductPreviewTab,
    orientation: ProductPreviewOrientation,
    isLandscape: Boolean,
    onTabSelect: (ProductPreviewTab) -> Unit,
    onOrientationSelect: (ProductPreviewOrientation) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(
                    horizontal = Dimens.SpaceMd,
                    vertical = if (isLandscape) Dimens.SpaceMd else Dimens.SpaceLg,
                ),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
        ) {
            if (isLandscape) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
                ) {
                    PreviewTitleBar(onDismiss = onDismiss)
                    PreviewTabSelector(tab = tab, onSelect = onTabSelect, modifier = Modifier.weight(1f))
                    PreviewOrientationSelector(tab = tab, orientation = orientation, onSelect = onOrientationSelect)
                }
            } else {
                PreviewTitleBar(onDismiss = onDismiss, modifier = Modifier.fillMaxWidth())
                PreviewTabSelector(tab = tab, onSelect = onTabSelect)
            }

            PreviewCanvas(
                data = data,
                tab = tab,
                orientation = orientation,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )

            if (!isLandscape) {
                PreviewOrientationSelector(
                    tab = tab,
                    orientation = orientation,
                    onSelect = onOrientationSelect,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }

            Text(
                text = stringResource(
                    if (tab == ProductPreviewTab.Detail && !isLandscape) {
                        R.string.preview_rotate_hint
                    } else {
                        R.string.preview_zoom_hint
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PreviewTitleBar(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.preview_close_cd),
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
        Text(
            text = stringResource(R.string.preview_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            modifier = Modifier
                .padding(start = Dimens.SpaceSm)
                .semantics { heading() },
        )
    }
}

@Composable
private fun PreviewTabSelector(
    tab: ProductPreviewTab,
    onSelect: (ProductPreviewTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    QhathiaSegmentedControl(
        options = PreviewTabs,
        selected = tab,
        onSelect = onSelect,
        label = { option ->
            stringResource(
                when (option) {
                    ProductPreviewTab.Item -> R.string.preview_tab_item
                    ProductPreviewTab.Detail -> R.string.preview_tab_detail
                },
            )
        },
        modifier = modifier,
    )
}

/**
 * Vertical / Horizontal for the catalog card, as a pair of icon buttons. The detail screen has no
 * vertical layout, so on that tab the pair is shown locked on Horizontal instead of disappearing.
 */
@Composable
private fun PreviewOrientationSelector(
    tab: ProductPreviewTab,
    orientation: ProductPreviewOrientation,
    onSelect: (ProductPreviewOrientation) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isItem = tab == ProductPreviewTab.Item
    val selected = if (isItem) orientation else ProductPreviewOrientation.Horizontal
    Row(
        modifier = modifier
            .alpha(if (isItem) 1f else LOCKED_ALPHA)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
    ) {
        PreviewOrientations.forEach { option ->
            OrientationButton(
                orientation = option,
                selected = option == selected,
                enabled = isItem,
                onClick = { onSelect(option) },
            )
        }
    }
}

/** One orientation choice: a phone glyph on a 48dp tile, filled in the brand accent when selected. */
@Composable
private fun OrientationButton(
    orientation: ProductPreviewOrientation,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isVertical = orientation == ProductPreviewOrientation.Vertical
    Box(
        modifier = modifier
            .size(Dimens.ButtonHeight)
            .clip(RoundedCornerShape(Dimens.ButtonRadius))
            .background(if (selected) QhathiaColors.brandAccentSoft else QhathiaColors.segmentTrack)
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isVertical) Icons.Filled.StayCurrentPortrait else Icons.Filled.StayCurrentLandscape,
            contentDescription = stringResource(
                if (isVertical) R.string.preview_orientation_vertical else R.string.preview_orientation_horizontal,
            ),
            tint = if (selected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** The preview for the current tab and orientation, on a zoomable canvas that resets when either changes. */
@Composable
private fun PreviewCanvas(
    data: ProductPreviewData,
    tab: ProductPreviewTab,
    orientation: ProductPreviewOrientation,
    modifier: Modifier = Modifier,
) {
    val price = previewPriceText(data.price)
    val canvasModifier = modifier
        .clip(RoundedCornerShape(Dimens.CardRadius))
        .background(QhathiaColors.segmentTrack)
    when {
        tab == ProductPreviewTab.Detail -> ProductPreviewCanvas(
            designSize = DiscoveryProductDetailSize,
            contentDescription = stringResource(R.string.preview_cd_detail, data.name, price),
            resetKey = tab,
            modifier = canvasModifier,
        ) {
            DiscoveryProductDetail(data = data)
        }

        orientation == ProductPreviewOrientation.Vertical -> ProductPreviewCanvas(
            designSize = DiscoveryVerticalItemSize + ItemCanvasMargin,
            contentDescription = stringResource(R.string.preview_cd_item_vertical, data.name, price),
            resetKey = orientation,
            modifier = canvasModifier,
        ) {
            ItemBackdrop { DiscoveryVerticalItem(data = data) }
        }

        else -> ProductPreviewCanvas(
            designSize = DiscoveryHorizontalItemSize + ItemCanvasMargin,
            contentDescription = stringResource(R.string.preview_cd_item_horizontal, data.name, price),
            resetKey = orientation,
            modifier = canvasModifier,
        ) {
            ItemBackdrop { DiscoveryHorizontalItem(data = data) }
        }
    }
}

/** Discovery's own screen background behind a catalog card, as it sits in the real catalog. */
@Composable
private fun ItemBackdrop(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(Dimens.FieldRadius))
            .background(DiscoveryPreviewPalette.Background),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** A design size grown by [margin] on every side. */
private operator fun DpSize.plus(margin: Dp) = DpSize(width + margin * 2, height + margin * 2)

// ---- Previews ----

@Preview(name = "Preview dialog — item vertical", showBackground = true)
@Preview(name = "Preview dialog — item vertical dark", showBackground = true, uiMode = 0x20)
@Composable
private fun ProductPreviewDialogVerticalPreview() {
    QhathiaTheme {
        ProductPreviewDialogContent(
            data = PreviewSampleData,
            tab = ProductPreviewTab.Item,
            orientation = ProductPreviewOrientation.Vertical,
            isLandscape = false,
            onTabSelect = {},
            onOrientationSelect = {},
            onDismiss = {},
        )
    }
}

@Preview(name = "Preview dialog — item horizontal", showBackground = true)
@Preview(name = "Preview dialog — item horizontal dark", showBackground = true, uiMode = 0x20)
@Composable
private fun ProductPreviewDialogHorizontalPreview() {
    QhathiaTheme {
        ProductPreviewDialogContent(
            data = PreviewSampleMinimalData,
            tab = ProductPreviewTab.Item,
            orientation = ProductPreviewOrientation.Horizontal,
            isLandscape = false,
            onTabSelect = {},
            onOrientationSelect = {},
            onDismiss = {},
        )
    }
}

@Preview(name = "Preview dialog — detail portrait", showBackground = true)
@Preview(name = "Preview dialog — detail portrait dark", showBackground = true, uiMode = 0x20)
@Composable
private fun ProductPreviewDialogDetailPortraitPreview() {
    QhathiaTheme {
        ProductPreviewDialogContent(
            data = PreviewSampleData,
            tab = ProductPreviewTab.Detail,
            orientation = ProductPreviewOrientation.Vertical,
            isLandscape = false,
            onTabSelect = {},
            onOrientationSelect = {},
            onDismiss = {},
        )
    }
}

@Preview(name = "Preview dialog — detail landscape", showBackground = true, widthDp = 852, heightDp = 393)
@Preview(
    name = "Preview dialog — detail landscape dark",
    showBackground = true,
    widthDp = 852,
    heightDp = 393,
    uiMode = 0x20,
)
@Composable
private fun ProductPreviewDialogDetailLandscapePreview() {
    QhathiaTheme {
        ProductPreviewDialogContent(
            data = PreviewSampleMinimalData,
            tab = ProductPreviewTab.Detail,
            orientation = ProductPreviewOrientation.Vertical,
            isLandscape = true,
            onTabSelect = {},
            onOrientationSelect = {},
            onDismiss = {},
        )
    }
}
