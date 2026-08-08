package com.qhathia.mobile.staff.feature.orderrequests.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.qhathia.mobile.staff.core.domain.model.Order
import com.qhathia.mobile.staff.core.domain.model.OrderStatus
import com.qhathia.mobile.staff.core.ui.theme.Dimens
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
    onReject: (String) -> Unit,
    onOpenDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = order.displayTitle(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                val expiresAt = order.expiresAt
                if (expiresAt != null && order.status == OrderStatus.PENDING) {
                    ExpiryCountdown(expiresAt = expiresAt, now = now)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = stringResource(R.string.order_card_code, order.displayCode()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(
                        R.string.order_card_total,
                        stringResource(R.string.order_price, order.total),
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
                if (canAct) {
                    OutlinedButton(
                        onClick = { onReject(order.id) },
                        shape = RoundedCornerShape(Dimens.ButtonRadius),
                        modifier = Modifier
                            .weight(1f)
                            .height(Dimens.ButtonHeight),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = stringResource(R.string.order_action_reject),
                            style = MaterialTheme.typography.labelLarge,
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
                    modifier = Modifier
                        .weight(1f)
                        .height(Dimens.ButtonHeight),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.List,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(R.string.order_action_detail),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(start = Dimens.SpaceSm),
                    )
                }
            }
        }
    }
}

/** Customer name, or `#<orderCode>` when the kiosk recorded no name. */
internal fun Order.displayTitle(): String = customerName ?: "#${displayCode()}"

/** The order code, falling back to a short id prefix for orders created before backend V15. */
internal fun Order.displayCode(): String =
    orderCode ?: id.take(ID_FALLBACK_LENGTH).uppercase()

// ---- Previews ----

@Preview(name = "Order card — light", showBackground = true)
@Preview(name = "Order card — dark", showBackground = true, uiMode = 0x20)
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
