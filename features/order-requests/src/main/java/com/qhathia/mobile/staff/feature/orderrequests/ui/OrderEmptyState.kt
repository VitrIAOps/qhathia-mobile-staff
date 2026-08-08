package com.qhathia.mobile.staff.feature.orderrequests.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.feature.orderrequests.R
import com.qhathia.mobile.staff.feature.orderrequests.model.OrderRequestsFilter

/** Centered empty message, worded for the active tab — also what an unmatched search shows. */
@Composable
internal fun OrderEmptyState(
    filter: OrderRequestsFilter,
    modifier: Modifier = Modifier,
) {
    val messageRes = when (filter) {
        OrderRequestsFilter.PENDING -> R.string.order_empty_pending
        OrderRequestsFilter.CONFIRMED -> R.string.order_empty_confirmed
        OrderRequestsFilter.REJECTED -> R.string.order_empty_rejected
    }
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(messageRes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(Dimens.SpaceLg),
        )
    }
}
