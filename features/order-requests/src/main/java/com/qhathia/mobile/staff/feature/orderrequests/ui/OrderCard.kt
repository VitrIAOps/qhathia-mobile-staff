package com.qhathia.mobile.staff.feature.orderrequests.ui

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.staff.core.domain.model.NotificationPreferences
import com.qhathia.mobile.staff.core.domain.model.Order
import com.qhathia.mobile.staff.core.domain.model.OrderStatus
import com.qhathia.mobile.staff.core.common.format.toPenPrice
import com.qhathia.mobile.staff.core.ui.component.AdaptiveButtonRow
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaColors
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme
import com.qhathia.mobile.staff.feature.orderrequests.R
import java.time.Instant

/** How much of the order id stands in for a missing `orderCode`. */
private const val ID_FALLBACK_LENGTH = 8

/**
 * One order in the list: identity and total on the left, the expiry countdown top-right, and the
 * Rechazar / Ver detalle actions.
 *
 * Two display fallbacks are deliberate rather than incidental — an order with no customer name is
 * titled by its code, and an order created before the backend added `orderCode` falls back to a
 * short prefix of its id, so no card can ever render an empty or `null` identifier.
 */
@Composable
internal fun OrderCard(
    order: Order,
    now: () -> Instant,
    onReject: (Order) -> Unit,
    onOpenDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    warningMinutes: Int = NotificationPreferences.DEFAULT_WARNING_MINUTES,
    minuteClock: Instant = now(),
    isRecovering: Boolean = false,
    onRecover: (String) -> Unit = {},
) {
    // derivedStateOf keeps the ticker from recomposing the card every second: only the moment the
    // order actually crosses its deadline invalidates this read.
    val expired by remember(order.expiresAt, now) {
        derivedStateOf { isExpired(order.expiresAt, now()) }
    }
    val canAct = order.status == OrderStatus.PENDING && !expired

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.SpaceMd),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(Dimens.SpaceMd),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
        ) {
            // With large text the countdown moves to the next line instead of eating the name (F-84 e).
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = order.displayTitle(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                val expiresAt = order.expiresAt
                if (expiresAt != null && order.status == OrderStatus.PENDING) {
                    ExpiryCountdown(expiresAt = expiresAt, now = now, warningMinutes = warningMinutes)
                } else {
                    // A closed order says what happened instead of a countdown.
                    order.outcomeLabel()?.let { label ->
                        StatusChip(
                            text = label,
                            containerColor = QhathiaColors.segmentTrack,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Code and total share a row only when both fit; otherwise the total moves below (F-84 b).
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
                itemVerticalAlignment = Alignment.Bottom,
            ) {
                val createdAt = order.createdAt
                Text(
                    text = if (createdAt != null) {
                        stringResource(
                            R.string.order_card_code_arrived,
                            order.displayCode(),
                            arrivedAgoLabel(createdAt, minuteClock),
                        )
                    } else {
                        stringResource(R.string.order_card_code, order.displayCode())
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    // Never ellipsized: "hace 31…" lost the minutes at font 1.3. At its own width, so
                    // when it does not fit beside the total, the total moves below (F-84 b).
                    modifier = Modifier.padding(end = Dimens.SpaceSm),
                )
                Text(
                    text = stringResource(
                        R.string.order_card_total,
                        order.total.toPenPrice(),
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            // Side by side when every label fits its share, stacked otherwise (F-84).
            AdaptiveButtonRow(modifier = Modifier.fillMaxWidth()) {
                // An expired request can come back to Pendientes; a manual rejection cannot.
                if (order.status == OrderStatus.EXPIRED) {
                    OutlinedButton(
                        onClick = { onRecover(order.id) },
                        enabled = !isRecovering,
                        shape = RoundedCornerShape(Dimens.ButtonRadius),
                        modifier = Modifier.heightIn(min = Dimens.ButtonHeight),
                    ) {
                        if (isRecovering) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Restore,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Text(
                            text = stringResource(R.string.order_action_recover),
                            style = MaterialTheme.typography.labelLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(start = Dimens.SpaceSm),
                        )
                    }
                }
                if (canAct) {
                    OutlinedButton(
                        onClick = { onReject(order) },
                        shape = RoundedCornerShape(Dimens.ButtonRadius),
                        modifier = Modifier.heightIn(min = Dimens.ButtonHeight),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = stringResource(R.string.order_action_reject),
                            style = MaterialTheme.typography.labelLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(start = Dimens.SpaceSm),
                        )
                    }
                }
                Button(
                    onClick = { onOpenDetail(order.id) },
                    shape = RoundedCornerShape(Dimens.ButtonRadius),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    modifier = Modifier.heightIn(min = Dimens.ButtonHeight),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.List,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(R.string.order_action_detail),
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(start = Dimens.SpaceSm),
                    )
                }
            }
        }
    }
}

/** What happened to a closed order, for its chip: "Vencida", the rejection reason, … */
@Composable
private fun Order.outcomeLabel(): String? = when (status) {
    OrderStatus.EXPIRED -> stringResource(R.string.order_expired)
    OrderStatus.REJECTED -> rejectionReason?.trim()?.takeIf { it.isNotEmpty() }
        ?: stringResource(R.string.order_status_rejected)
    OrderStatus.CANCELLED -> stringResource(R.string.order_status_cancelled)
    OrderStatus.CONFIRMED, OrderStatus.PENDING -> null
}

/** Customer name, or `#<orderCode>` when the kiosk recorded no name. */
internal fun Order.displayTitle(): String = customerName ?: "#${displayCode()}"

/** The order code, falling back to a short id prefix for orders created before backend V15. */
internal fun Order.displayCode(): String =
    orderCode ?: id.take(ID_FALLBACK_LENGTH).uppercase()

// ---- Previews ----

@Preview(name = "Order card — light", showBackground = true)
@Preview(name = "Order card — dark", showBackground = true, uiMode = 0x20)
@Preview(name = "Order card — font 1.3, light", showBackground = true, fontScale = 1.3f)
@Preview(name = "Order card — font 1.3, dark", showBackground = true, fontScale = 1.3f, uiMode = 0x20)
@Preview(name = "Order card — font 2.0", showBackground = true, fontScale = 2f, widthDp = 360)
@Composable
private fun OrderCardPreview() {
    QhathiaTheme {
        OrderCard(
            order = previewOrder(),
            now = { PREVIEW_NOW },
            onReject = {},
            onOpenDetail = {},
            modifier = Modifier.padding(Dimens.SpaceMd),
        )
    }
}

@Preview(name = "Order card — expiring soon, no name or code", showBackground = true)
@Composable
private fun OrderCardExpiringSoonPreview() {
    QhathiaTheme {
        OrderCard(
            order = previewOrder(
                id = "859656aa-1111",
                customerName = null,
                orderCode = null,
                expiresIn = 2 * 60L,
            ),
            now = { PREVIEW_NOW },
            onReject = {},
            onOpenDetail = {},
            modifier = Modifier.padding(Dimens.SpaceMd),
        )
    }
}

@Preview(name = "Order card — expiring, amber", showBackground = true)
@Preview(name = "Order card — expiring, amber, dark", showBackground = true, uiMode = 0x20)
@Composable
private fun OrderCardWarningPreview() {
    QhathiaTheme {
        OrderCard(
            order = previewOrder(expiresIn = 8 * 60L),
            now = { PREVIEW_NOW },
            onReject = {},
            onOpenDetail = {},
            modifier = Modifier.padding(Dimens.SpaceMd),
        )
    }
}

@Preview(name = "Order card — expired, recover", showBackground = true)
@Preview(name = "Order card — expired, recover, dark", showBackground = true, uiMode = 0x20)
@Composable
private fun OrderCardExpiredPreview() {
    QhathiaTheme {
        OrderCard(
            order = previewOrder(status = OrderStatus.EXPIRED),
            now = { PREVIEW_NOW },
            onReject = {},
            onOpenDetail = {},
            modifier = Modifier.padding(Dimens.SpaceMd),
        )
    }
}

@Preview(name = "Order card — rejected with a reason", showBackground = true)
@Preview(name = "Order card — rejected with a reason, dark", showBackground = true, uiMode = 0x20)
@Composable
private fun OrderCardRejectedPreview() {
    QhathiaTheme {
        OrderCard(
            order = previewOrder(status = OrderStatus.REJECTED, rejectionReason = "Cantidad insuficiente"),
            now = { PREVIEW_NOW },
            onReject = {},
            onOpenDetail = {},
            modifier = Modifier.padding(Dimens.SpaceMd),
        )
    }
}
