package com.qhathia.mobile.staff.feature.orderrequests.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Size
import com.qhathia.mobile.staff.core.domain.model.OrderDetail
import com.qhathia.mobile.staff.core.domain.model.OrderItem
import com.qhathia.mobile.staff.core.domain.model.OrderStatus
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaColors
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme
import com.qhathia.mobile.staff.feature.orderrequests.R
import com.qhathia.mobile.staff.core.ui.R as CoreUiR
import com.qhathia.mobile.staff.feature.orderrequests.model.OrderDetailUiState
import com.qhathia.mobile.staff.feature.orderrequests.model.RejectionReason
import com.qhathia.mobile.staff.feature.orderrequests.model.RejectionUiState
import java.time.Instant

/**
 * At or below this much tracked stock (and above zero), an item shows the "quedan pocos
 * disponibles" warning. The threshold is client-side: the contract carries the raw `productStock`,
 * not a low-stock flag.
 */
private const val LOW_STOCK_THRESHOLD = 5

private val ThumbnailSize = 48.dp

/**
 * The order detail, as a dialog over the list. It always renders from a fresh
 * `GET /orders/{id}` — the list rows carry no `items[]` — and its confirm/reject actions are the
 * screen's only writes.
 *
 * Dismissing changes nothing: no snackbar, no refresh.
 */
@Composable
internal fun OrderDetailDialog(
    state: OrderDetailUiState,
    now: () -> Instant,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onStartRejection: () -> Unit,
    onSelectRejectionReason: (RejectionReason) -> Unit,
    onOtherRejectionReasonChange: (String) -> Unit,
    onCancelRejection: () -> Unit,
    onConfirmRejection: (String) -> Unit,
) {
    val rejection = state.rejection
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(Dimens.SpaceLg),
            color = MaterialTheme.colorScheme.surface,
        ) {
            // The reject form is a step of this same dialog, and it brings its own centered title,
            // so the detail header is not drawn over it.
            if (rejection != null) {
                OrderRejectForm(
                    state = rejection,
                    isSubmitting = state.isSubmitting,
                    onSelectReason = onSelectRejectionReason,
                    onOtherReasonChange = onOtherRejectionReasonChange,
                    onCancel = onCancelRejection,
                    onConfirm = onConfirmRejection,
                    modifier = Modifier.padding(
                        horizontal = Dimens.SpaceLg,
                        vertical = Dimens.SpaceXl,
                    ),
                )
                return@Surface
            }

            Column(modifier = Modifier.padding(Dimens.SpaceMd)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = state.order.dialogTitle(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.order_detail_close_cd),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                when {
                    state.isLoading -> DialogLoadingState()
                    state.isError || state.order == null -> DialogErrorState()
                    else -> OrderDetailBody(
                        detail = state.order,
                        isSubmitting = state.isSubmitting,
                        now = now,
                        onConfirm = onConfirm,
                        onReject = onStartRejection,
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderDetailBody(
    detail: OrderDetail,
    isSubmitting: Boolean,
    now: () -> Instant,
    onConfirm: (String) -> Unit,
    onReject: () -> Unit,
) {
    val footer = orderDetailFooter(detail.order.status, detail.order.expiresAt, now())

    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
        Column(
            modifier = Modifier
                .heightIn(max = 320.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
        ) {
            detail.items.forEach { item -> OrderItemRow(item = item) }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(Dimens.FieldRadius),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Row(
                modifier = Modifier.padding(Dimens.SpaceMd),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.order_detail_total),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(R.string.order_price, detail.order.total),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        when (footer) {
            OrderDetailFooter.Actions -> OrderDetailActions(
                isSubmitting = isSubmitting,
                onConfirm = { onConfirm(detail.order.id) },
                onReject = onReject,
            )
            is OrderDetailFooter.Resolved -> Text(
                text = stringResource(footer.messageRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Dimens.SpaceSm),
            )
        }
    }
}

/**
 * Reject and confirm, side by side. While a request is in flight both stay visible but disabled,
 * with progress on the confirm button.
 */
@Composable
private fun OrderDetailActions(
    isSubmitting: Boolean,
    onConfirm: () -> Unit,
    onReject: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(
            onClick = onReject,
            enabled = !isSubmitting,
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = stringResource(R.string.order_action_reject),
                style = MaterialTheme.typography.labelLarge,
            )
        }
        Button(
            onClick = onConfirm,
            enabled = !isSubmitting,
            shape = RoundedCornerShape(Dimens.ButtonRadius),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            // A minimum, not a fixed height: at half the dialog width the label can wrap, and a
            // fixed 48 dp clipped its second line.
            modifier = Modifier
                .weight(1f)
                .heightIn(min = Dimens.ButtonHeight),
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp),
                )
            } else {
                Text(
                    text = stringResource(R.string.order_action_confirm),
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * One item: the product thumbnail, the product name, unit price, quantity and line total — plus the
 * low-stock warning when the product still tracks stock and is running out.
 */
@Composable
private fun OrderItemRow(
    item: OrderItem,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(Dimens.FieldRadius)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, colors.outline, shape)
            .clip(shape)
            .padding(Dimens.SpaceSm),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
        ) {
            OrderItemThumbnail(imageUrl = item.productImageUrl)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.productName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd)) {
                    Text(
                        text = stringResource(R.string.order_price, item.unitPrice),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                    Text(
                        text = stringResource(R.string.order_item_quantity, item.quantity),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
            Text(
                text = stringResource(R.string.order_price, item.lineTotal),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface,
            )
        }

        // Null stock means a consult-mode product: nothing is tracked, so nothing is warned about.
        // Zero gets its own warning: "quedan pocos" would soften a stock-out into a low-stock one,
        // which is how a merchant once confirmed an order that could not be shipped.
        val stock = item.productStock
        when {
            stock == null -> Unit
            stock <= 0 -> OutOfStockWarning()
            stock <= LOW_STOCK_THRESHOLD -> LowStockWarning()
        }
    }
}

/**
 * The product photo, which arrives with the order detail itself — no per-item request. The neutral
 * placeholder stays up until the photo is actually on screen, so a product without images, a slow
 * load and a failed one all read the same instead of leaving a blank hole.
 */
@Composable
private fun OrderItemThumbnail(
    imageUrl: String?,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    var isLoaded by remember(imageUrl) { mutableStateOf(false) }
    Box(
        modifier = modifier
            .size(ThumbnailSize)
            .clip(RoundedCornerShape(Dimens.FieldRadius))
            .background(colors.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (!isLoaded) {
            Icon(
                imageVector = Icons.Filled.Image,
                contentDescription = null,
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }
        if (imageUrl != null) {
            // Sized explicitly so Coil never decodes a bitmap larger than the slot.
            val sizePx = with(LocalDensity.current) { ThumbnailSize.roundToPx() }
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUrl)
                    .size(Size(sizePx, sizePx))
                    .build(),
                // The product name sits right beside it; a description would double it up.
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onSuccess = { isLoaded = true },
                onError = { isLoaded = false },
                modifier = Modifier.size(ThumbnailSize),
            )
        }
    }
}

@Composable
private fun LowStockWarning(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(QhathiaColors.warningContainer, RoundedCornerShape(Dimens.SpaceXs))
            .padding(horizontal = Dimens.SpaceSm, vertical = Dimens.SpaceXs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXs, Alignment.CenterHorizontally),
    ) {
        Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = QhathiaColors.onWarningContainer,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = stringResource(R.string.order_item_low_stock),
            style = MaterialTheme.typography.bodySmall,
            color = QhathiaColors.onWarningContainer,
        )
    }
}

/** Same shape as [LowStockWarning], in the error tones and with the alert icon, so it reads as worse. */
@Composable
private fun OutOfStockWarning(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.errorContainer, RoundedCornerShape(Dimens.SpaceXs))
            .padding(horizontal = Dimens.SpaceSm, vertical = Dimens.SpaceXs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXs, Alignment.CenterHorizontally),
    ) {
        Icon(
            painter = painterResource(CoreUiR.drawable.ic_alert),
            contentDescription = null,
            tint = colors.onErrorContainer,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = stringResource(R.string.order_item_out_of_stock),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onErrorContainer,
        )
    }
}

@Composable
private fun DialogLoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Dimens.SpaceXl),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun DialogErrorState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(Dimens.SpaceLg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.order_error_detail),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** `Detalle de solicitud #<code>`, dropping the suffix for orders that predate `orderCode`. */
@Composable
private fun OrderDetail?.dialogTitle(): String {
    val code = this?.order?.orderCode
    return if (code == null) {
        stringResource(R.string.order_detail_title_no_code)
    } else {
        stringResource(R.string.order_detail_title, code)
    }
}

// ---- Previews ----

@Preview(name = "Order detail — light", showBackground = true)
@Preview(name = "Order detail — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun OrderDetailDialogPreview() {
    QhathiaTheme {
        OrderDetailDialog(
            state = OrderDetailUiState(orderId = "order-1", order = previewOrderDetail()),
            now = { PREVIEW_NOW },
            onDismiss = {},
            onConfirm = {},
            onStartRejection = {},
            onSelectRejectionReason = {},
            onOtherRejectionReasonChange = {},
            onCancelRejection = {},
            onConfirmRejection = {},
        )
    }
}

// A resolved order: no actions, only its outcome.
@Preview(name = "Order detail — confirmed", showBackground = true)
@Preview(name = "Order detail — confirmed dark", showBackground = true, uiMode = 0x20)
@Composable
private fun OrderDetailDialogConfirmedPreview() {
    OrderDetailDialogStatePreview(
        OrderDetailUiState(
            orderId = "order-1",
            order = previewOrderDetail(status = OrderStatus.CONFIRMED, expiresIn = null),
        ),
    )
}

// Still PENDING on the server, but past its deadline: reads as expired.
@Preview(name = "Order detail — pending past expiry", showBackground = true)
@Composable
private fun OrderDetailDialogExpiredPreview() {
    OrderDetailDialogStatePreview(
        OrderDetailUiState(
            orderId = "order-1",
            order = previewOrderDetail(expiresIn = -60L),
        ),
    )
}

// A confirm in flight: both actions stay, disabled, with progress on confirm.
@Preview(name = "Order detail — submitting", showBackground = true)
@Composable
private fun OrderDetailDialogSubmittingPreview() {
    OrderDetailDialogStatePreview(
        OrderDetailUiState(
            orderId = "order-1",
            order = previewOrderDetail(),
            isSubmitting = true,
        ),
    )
}

@Composable
private fun OrderDetailDialogStatePreview(state: OrderDetailUiState) {
    QhathiaTheme {
        OrderDetailDialog(
            state = state,
            now = { PREVIEW_NOW },
            onDismiss = {},
            onConfirm = {},
            onStartRejection = {},
            onSelectRejectionReason = {},
            onOtherRejectionReasonChange = {},
            onCancelRejection = {},
            onConfirmRejection = {},
        )
    }
}

@Preview(name = "Order detail — loading", showBackground = true)
@Composable
private fun OrderDetailDialogLoadingPreview() {
    QhathiaTheme {
        OrderDetailDialog(
            state = OrderDetailUiState(orderId = "order-1", isLoading = true),
            now = { PREVIEW_NOW },
            onDismiss = {},
            onConfirm = {},
            onStartRejection = {},
            onSelectRejectionReason = {},
            onOtherRejectionReasonChange = {},
            onCancelRejection = {},
            onConfirmRejection = {},
        )
    }
}

@Preview(name = "Order detail — reject form", showBackground = true)
@Composable
private fun OrderDetailDialogRejectPreview() {
    QhathiaTheme {
        OrderDetailDialog(
            state = OrderDetailUiState(
                orderId = "order-1",
                order = previewOrderDetail(),
                rejection = RejectionUiState(
                    orderCode = "458795",
                    selectedReason = RejectionReason.INSUFFICIENT_QUANTITY,
                ),
            ),
            now = { PREVIEW_NOW },
            onDismiss = {},
            onConfirm = {},
            onStartRejection = {},
            onSelectRejectionReason = {},
            onOtherRejectionReasonChange = {},
            onCancelRejection = {},
            onConfirmRejection = {},
        )
    }
}

@Preview(name = "Order detail — error", showBackground = true)
@Composable
private fun OrderDetailDialogErrorPreview() {
    QhathiaTheme {
        OrderDetailDialog(
            state = OrderDetailUiState(orderId = "order-1", isError = true),
            now = { PREVIEW_NOW },
            onDismiss = {},
            onConfirm = {},
            onStartRejection = {},
            onSelectRejectionReason = {},
            onOtherRejectionReasonChange = {},
            onCancelRejection = {},
            onConfirmRejection = {},
        )
    }
}
