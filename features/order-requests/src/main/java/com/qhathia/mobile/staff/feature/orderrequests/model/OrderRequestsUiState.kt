package com.qhathia.mobile.staff.feature.orderrequests.model

import androidx.compose.runtime.Immutable
import com.qhathia.mobile.staff.core.domain.model.Order
import com.qhathia.mobile.staff.core.domain.model.OrderDetail
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.time.Instant

/**
 * Immutable snapshot of the Solicitudes screen.
 *
 * [orders] is whatever the active [selectedFilter] and [searchQuery] returned — the filtering is
 * server-side, so there is no second "visible" list. [pendingCount] comes from its own `size=1`
 * pending query and therefore stays correct while the merchant looks at another tab.
 *
 * A failed load sets [isError] but leaves [orders] and [lastUpdatedAt] untouched, so a refresh that
 * fails degrades to "stale data plus an error" rather than an empty screen.
 */
@Immutable
data class OrderRequestsUiState(
    val orders: ImmutableList<Order> = persistentListOf(),
    val selectedFilter: OrderRequestsFilter = OrderRequestsFilter.PENDING,
    val pendingCount: Int = 0,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val lastUpdatedAt: Instant? = null,
    val detail: OrderDetailUiState? = null,
)

/**
 * The detail dialog, nested in the list state because its outcome *is* a list mutation. A null
 * [OrderRequestsUiState.detail] means no dialog is open.
 *
 * [isSubmitting] is true while a confirm or reject issued from the dialog is in flight, which is
 * what stops a second tap from firing a second request.
 */
@Immutable
data class OrderDetailUiState(
    val orderId: String,
    val order: OrderDetail? = null,
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val isSubmitting: Boolean = false,
)
