package com.qhathia.mobile.staff.feature.orderrequests.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.feature.orderrequests.R
import com.qhathia.mobile.staff.feature.orderrequests.model.OrderRequestsFilter

/**
 * The three status tabs. Only Pendientes carries a count — it is the queue the merchant has to work
 * through, and it stays accurate on the other tabs because the ViewModel refreshes it with every
 * load.
 */
@Composable
internal fun OrderFilterChips(
    selectedFilter: OrderRequestsFilter,
    pendingCount: Int,
    onSelectFilter: (OrderRequestsFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
    ) {
        OrderFilterChip(
            label = stringResource(R.string.order_filter_pending, pendingCount),
            selected = selectedFilter == OrderRequestsFilter.PENDING,
            onClick = { onSelectFilter(OrderRequestsFilter.PENDING) },
        )
        OrderFilterChip(
            label = stringResource(R.string.order_filter_confirmed),
            selected = selectedFilter == OrderRequestsFilter.CONFIRMED,
            onClick = { onSelectFilter(OrderRequestsFilter.CONFIRMED) },
        )
        OrderFilterChip(
            label = stringResource(R.string.order_filter_rejected),
            selected = selectedFilter == OrderRequestsFilter.REJECTED,
            onClick = { onSelectFilter(OrderRequestsFilter.REJECTED) },
        )
    }
}

/** Selected: filled brand pill. Unselected: hairline outline — never colour alone. */
@Composable
private fun OrderFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(percent = 50)
    Text(
        text = label,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        color = if (selected) colors.onSecondary else colors.onSurface,
        modifier = Modifier
            .clip(shape)
            .then(
                if (selected) {
                    Modifier.background(colors.secondary, shape)
                } else {
                    Modifier.border(1.dp, colors.outlineVariant, shape)
                },
            )
            // A tab of the filter group, as QhathiaSegmentedControl does.
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = Dimens.SpaceMd, vertical = 12.dp),
    )
}
