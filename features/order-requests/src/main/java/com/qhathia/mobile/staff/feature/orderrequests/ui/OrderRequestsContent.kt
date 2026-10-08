package com.qhathia.mobile.staff.feature.orderrequests.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.qhathia.mobile.staff.core.domain.model.Order
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme
import com.qhathia.mobile.staff.feature.orderrequests.R
import com.qhathia.mobile.staff.feature.orderrequests.model.RejectionReason
import com.qhathia.mobile.staff.feature.orderrequests.model.OrderDetailUiState
import com.qhathia.mobile.staff.feature.orderrequests.model.OrderRequestsFilter
import com.qhathia.mobile.staff.feature.orderrequests.model.OrderRequestsUiEvent
import com.qhathia.mobile.staff.feature.orderrequests.model.OrderRequestsUiState
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.delay
import java.time.Instant

private const val TICK_INTERVAL_MS = 1_000L

/**
 * Stateless Solicitudes UI — a pure function of [uiState]: gradient header, status tabs and the
 * order list, with the detail dialog layered on top when one is open.
 *
 * A single ticker drives every countdown on screen. One per card would multiply recompositions
 * across the list for a value that changes identically for all of them.
 */
@Composable
fun OrderRequestsContent(
    uiState: OrderRequestsUiState,
    onEvent: (OrderRequestsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var now by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(TICK_INTERVAL_MS)
            now = Instant.now()
        }
    }
    // Handed down as a lambda so the ticking read happens inside each countdown label rather than
    // in the list or the cards.
    val nowProvider = remember { { now } }

    val onSearch = remember(onEvent) { { query: String -> onEvent(OrderRequestsUiEvent.Search(query)) } }
    val onRefresh = remember(onEvent) { { onEvent(OrderRequestsUiEvent.Refresh) } }
    val onSelectFilter = remember(onEvent) {
        { filter: OrderRequestsFilter -> onEvent(OrderRequestsUiEvent.SelectFilter(filter)) }
    }
    val onOpenDetail = remember(onEvent) {
        { id: String -> onEvent(OrderRequestsUiEvent.OpenDetail(id)) }
    }
    // Rejecting always goes through the motive form, whether it starts on a card or in the dialog.
    val onReject = remember(onEvent) {
        { order: Order ->
            onEvent(OrderRequestsUiEvent.StartRejection(order.id, order.orderCode))
        }
    }
    val onConfirm = remember(onEvent) {
        { id: String -> onEvent(OrderRequestsUiEvent.ConfirmOrder(id)) }
    }
    val onDismissDetail = remember(onEvent) { { onEvent(OrderRequestsUiEvent.DismissDetail) } }
    val onSelectRejectionReason = remember(onEvent) {
        { reason: RejectionReason -> onEvent(OrderRequestsUiEvent.SelectRejectionReason(reason)) }
    }
    val onOtherRejectionReasonChange = remember(onEvent) {
        { text: String -> onEvent(OrderRequestsUiEvent.EditOtherRejectionReason(text)) }
    }
    val onCancelRejection = remember(onEvent) { { onEvent(OrderRequestsUiEvent.CancelRejection) } }
    val onConfirmRejection = remember(onEvent) {
        { reason: String -> onEvent(OrderRequestsUiEvent.ConfirmRejection(reason)) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        OrderRequestsHeader(
            lastUpdatedAt = uiState.lastUpdatedAt,
            searchQuery = uiState.searchQuery,
            onSearch = onSearch,
            onRefresh = onRefresh,
        )

        Spacer(Modifier.height(Dimens.SpaceMd))

        OrderFilterChips(
            selectedFilter = uiState.selectedFilter,
            pendingCount = uiState.pendingCount,
            onSelectFilter = onSelectFilter,
            modifier = Modifier.padding(horizontal = Dimens.SpaceMd),
        )

        Box(modifier = Modifier.weight(1f)) {
            when {
                uiState.isLoading && uiState.orders.isEmpty() -> LoadingState()
                uiState.isError && uiState.orders.isEmpty() ->
                    ErrorState(onRetry = { onEvent(OrderRequestsUiEvent.Retry) })
                uiState.orders.isEmpty() -> OrderEmptyState(filter = uiState.selectedFilter)
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(Dimens.SpaceMd),
                    verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
                ) {
                    items(uiState.orders, key = { order -> order.id }) { order ->
                        OrderCard(
                            order = order,
                            now = nowProvider,
                            onReject = onReject,
                            onOpenDetail = onOpenDetail,
                        )
                    }
                }
            }
        }
    }

    uiState.detail?.let { detail ->
        OrderDetailDialog(
            state = detail,
            now = nowProvider,
            onDismiss = onDismissDetail,
            onConfirm = onConfirm,
            onStartRejection = {
                onEvent(
                    OrderRequestsUiEvent.StartRejection(
                        orderId = detail.orderId,
                        orderCode = detail.order?.order?.orderCode,
                    ),
                )
            },
            onSelectRejectionReason = onSelectRejectionReason,
            onOtherRejectionReasonChange = onOtherRejectionReasonChange,
            onCancelRejection = onCancelRejection,
            onConfirmRejection = onConfirmRejection,
        )
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorState(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
            modifier = Modifier.padding(Dimens.SpaceLg),
        ) {
            Text(
                text = stringResource(R.string.order_error_load),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.order_retry))
            }
        }
    }
}

// ---- Previews ----

private fun previewState(
    filter: OrderRequestsFilter = OrderRequestsFilter.PENDING,
    isLoading: Boolean = false,
    isError: Boolean = false,
    detail: OrderDetailUiState? = null,
) = OrderRequestsUiState(
    orders = listOf(
        previewOrder(),
        previewOrder(
            id = "859656bb-2222",
            customerName = null,
            orderCode = "859656",
            expiresIn = 2 * 60L,
        ),
    ).toImmutableList(),
    selectedFilter = filter,
    pendingCount = 2,
    isLoading = isLoading,
    isError = isError,
    lastUpdatedAt = PREVIEW_NOW,
    detail = detail,
)

@Preview(name = "Solicitudes — light", showBackground = true)
@Preview(name = "Solicitudes — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun OrderRequestsContentPreview() {
    QhathiaTheme {
        OrderRequestsContent(uiState = previewState(), onEvent = {})
    }
}

@Preview(name = "Solicitudes — searching", showBackground = true)
@Preview(name = "Solicitudes — searching dark", showBackground = true, uiMode = 0x20)
@Composable
private fun OrderRequestsContentSearchingPreview() {
    QhathiaTheme {
        OrderRequestsContent(uiState = previewState().copy(searchQuery = "Mari"), onEvent = {})
    }
}

@Preview(name = "Solicitudes — empty", showBackground = true)
@Composable
private fun OrderRequestsContentEmptyPreview() {
    QhathiaTheme {
        OrderRequestsContent(
            uiState = OrderRequestsUiState(
                selectedFilter = OrderRequestsFilter.REJECTED,
                lastUpdatedAt = PREVIEW_NOW,
            ),
            onEvent = {},
        )
    }
}

@Preview(name = "Solicitudes — error", showBackground = true)
@Composable
private fun OrderRequestsContentErrorPreview() {
    QhathiaTheme {
        OrderRequestsContent(
            uiState = OrderRequestsUiState(isError = true),
            onEvent = {},
        )
    }
}

@Preview(name = "Solicitudes — loading", showBackground = true)
@Composable
private fun OrderRequestsContentLoadingPreview() {
    QhathiaTheme {
        OrderRequestsContent(
            uiState = OrderRequestsUiState(isLoading = true),
            onEvent = {},
        )
    }
}
