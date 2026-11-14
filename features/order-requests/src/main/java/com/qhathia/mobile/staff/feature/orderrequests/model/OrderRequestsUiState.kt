package com.qhathia.mobile.staff.feature.orderrequests.model

import androidx.annotation.StringRes
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
 * server-side, so there is no second "visible" list. [pendingCount] is the app-wide count held by
 * the order data layer, not a copy this screen maintains, so it stays correct while the merchant
 * looks at another tab and always matches the Solicitudes tab badge and the Inicio bell.
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
    /**
     * Bumped when the list must show its top (F-80): a new order arrived above the old first one, or
     * another filter or search was loaded. A settled order or the same page reloaded never bumps it.
     */
    val scrollToTopKey: Int = 0,
)

/**
 * The detail dialog, nested in the list state because its outcome *is* a list mutation. A null
 * [OrderRequestsUiState.detail] means no dialog is open.
 *
 * [isSubmitting] is true while a confirm or reject issued from the dialog is in flight, which is
 * what stops a second tap from firing a second request.
 *
 * A non-null [rejection] swaps the dialog's body for the reject form. A rejection started from a
 * list card opens the dialog straight into that form, so [order] can still be null while the form
 * is on screen — the form only needs the code the card already had.
 */
@Immutable
data class OrderDetailUiState(
    val orderId: String,
    val order: OrderDetail? = null,
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val isSubmitting: Boolean = false,
    val rejection: RejectionUiState? = null,
    val submitError: SubmitError? = null,
)

/**
 * Why the dialog's own action (confirm, reject) did not go through, shown inside the dialog: the
 * screen's snackbar sits under the dialog's scrim. [arg] fills the message's placeholder, if any.
 */
data class SubmitError(
    @param:StringRes val messageRes: Int,
    val arg: String? = null,
)

/**
 * The reject form: which motive is selected and, for [RejectionReason.OTHER], the typed one.
 *
 * [canSubmit] is the whole validation rule — a motive must be chosen, and choosing "Otro motivo"
 * means actually writing it, since an empty free-text option would send a rejection with no reason
 * at all.
 */
@Immutable
data class RejectionUiState(
    val orderCode: String?,
    val selectedReason: RejectionReason? = null,
    val otherReason: String = "",
) {
    val canSubmit: Boolean
        get() = when (selectedReason) {
            null -> false
            RejectionReason.OTHER -> otherReason.isNotBlank()
            else -> true
        }
}
