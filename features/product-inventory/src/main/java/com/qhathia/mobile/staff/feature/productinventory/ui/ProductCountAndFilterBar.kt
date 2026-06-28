package com.qhathia.mobile.staff.feature.productinventory.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.qhathia.mobile.staff.core.domain.model.Availability
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.feature.productinventory.R

/**
 * Shows the visible product count and the availability sub-filter control. The filter button opens
 * a menu with "Todas" (clears the filter) plus the three availability options.
 */
@Composable
internal fun ProductCountAndFilterBar(
    productCount: Int,
    selectedAvailability: Availability?,
    onSelectAvailability: (Availability) -> Unit,
    onClearAvailability: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(R.string.inventory_product_count, productCount),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Box {
            TextButton(onClick = { menuExpanded = true }) {
                Icon(
                    imageVector = Icons.Filled.FilterList,
                    contentDescription = stringResource(R.string.inventory_filter_cd),
                    modifier = Modifier.padding(end = Dimens.SpaceXs),
                )
                Text(
                    text = selectedAvailability?.let { stringResource(it.filterLabelRes()) }
                        ?: stringResource(R.string.inventory_filter),
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.inventory_availability_all)) },
                    onClick = {
                        menuExpanded = false
                        onClearAvailability()
                    },
                )
                Availability.entries.forEach { availability ->
                    DropdownMenuItem(
                        text = { Text(stringResource(availability.filterLabelRes())) },
                        onClick = {
                            menuExpanded = false
                            onSelectAvailability(availability)
                        },
                    )
                }
            }
        }
    }
}
