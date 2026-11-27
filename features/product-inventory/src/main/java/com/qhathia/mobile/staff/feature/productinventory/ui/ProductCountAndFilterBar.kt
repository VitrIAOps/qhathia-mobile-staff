package com.qhathia.mobile.staff.feature.productinventory.ui

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.InputChip
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qhathia.mobile.staff.core.domain.model.Availability
import com.qhathia.mobile.staff.core.domain.model.Category
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaColors
import com.qhathia.mobile.staff.feature.productinventory.R
import kotlinx.collections.immutable.ImmutableList

/**
 * The DS bar above the product list: the "Filtros" pill on the left, the visible product count on
 * the right.
 *
 * The pill owns both filter dimensions — category and availability — in one menu, since the DS
 * screen has no separate category control. A check mark marks the active option in each section,
 * so the applied filters stay discoverable now that they are behind a menu.
 */
@Composable
internal fun ProductCountAndFilterBar(
    productCount: Int,
    categories: ImmutableList<Category>,
    selectedCategoryId: String?,
    selectedAvailability: Availability?,
    onSelectCategory: (String?) -> Unit,
    onSelectAvailability: (Availability) -> Unit,
    onClearAvailability: () -> Unit,
    modifier: Modifier = Modifier,
    showCount: Boolean = true,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        // Without the count (inside the compact header, F-84 a) the bar is just the pill and must not
        // take the row's width from the search field.
        modifier = if (showCount) modifier.fillMaxWidth() else modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Box {
            FilterPill(
                activeCount = listOfNotNull(selectedCategoryId, selectedAvailability).size,
                onClick = { menuExpanded = true },
            )
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                MenuSectionLabel(stringResource(R.string.inventory_filter_categories))
                FilterMenuItem(
                    label = stringResource(R.string.inventory_category_all),
                    checked = selectedCategoryId == null,
                    onClick = {
                        menuExpanded = false
                        onSelectCategory(null)
                    },
                )
                categories.forEach { category ->
                    FilterMenuItem(
                        label = category.name,
                        checked = selectedCategoryId == category.id,
                        onClick = {
                            menuExpanded = false
                            onSelectCategory(category.id)
                        },
                    )
                }

                HorizontalDivider(color = QhathiaColors.rowDivider)

                MenuSectionLabel(stringResource(R.string.inventory_filter_availability))
                FilterMenuItem(
                    label = stringResource(R.string.inventory_availability_all),
                    checked = selectedAvailability == null,
                    onClick = {
                        menuExpanded = false
                        onClearAvailability()
                    },
                )
                Availability.entries.forEach { availability ->
                    FilterMenuItem(
                        label = stringResource(availability.filterLabelRes()),
                        checked = selectedAvailability == availability,
                        onClick = {
                            menuExpanded = false
                            onSelectAvailability(availability)
                        },
                    )
                }
            }
        }

        if (showCount) {
            Text(
                text = pluralStringResource(R.plurals.inventory_product_count, productCount, productCount),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * DS "Tab" pill: brand-orange fill outlined a shade darker, lifted by a small drop shadow.
 * Sized by its own padding — [minimumInteractiveComponentSize] grows only the touch target, so the
 * 34dp-tall pill in the design still meets the 48dp minimum.
 */
@Composable
private fun FilterPill(
    activeCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    // "Filtros · 1": a filtered list no longer looks like missing products (S17-02).
    val label = if (activeCount > 0) {
        pluralStringResource(R.plurals.inventory_filter_active, activeCount, activeCount)
    } else {
        stringResource(R.string.inventory_filter)
    }
    val description = if (activeCount > 0) {
        pluralStringResource(R.plurals.inventory_filter_active_cd, activeCount, activeCount)
    } else {
        stringResource(R.string.inventory_filter_cd)
    }
    val shape = RoundedCornerShape(Dimens.PillRadius)
    Row(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .shadow(2.dp, shape)
            .clip(shape)
            .background(QhathiaColors.brandAccentSoft)
            .border(1.dp, colors.secondary, shape)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = description }
            .padding(horizontal = Dimens.SpaceMd, vertical = Dimens.SpaceSm),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.3.sp,
            color = colors.onSecondary,
        )
        Icon(
            imageVector = Icons.Filled.Tune,
            // The pill's own description names it and the active count.
            contentDescription = null,
            tint = colors.onSecondary,
            modifier = Modifier.size(Dimens.IconSm),
        )
    }
}

/** Non-interactive heading separating the two filter dimensions inside the menu. */
@Composable
private fun MenuSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 12.dp, vertical = Dimens.SpaceSm),
    )
}

@Composable
private fun FilterMenuItem(
    label: String,
    checked: Boolean,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(label) },
        onClick = onClick,
        trailingIcon = {
            if (checked) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(Dimens.IconSm),
                )
            }
        },
    )
}

/**
 * One chip per active filter under the bar, each with a ✕ that clears only that filter, so the
 * merchant sees what narrows the list without opening the menu (S17-02). Nothing with no filter.
 * A category id missing from [categories] still counts on the pill but gets no chip.
 */
@Composable
internal fun ActiveFilterChips(
    categories: ImmutableList<Category>,
    selectedCategoryId: String?,
    selectedAvailability: Availability?,
    onClearCategory: () -> Unit,
    onClearAvailability: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val categoryName = selectedCategoryId?.let { id -> categories.firstOrNull { it.id == id }?.name }
    if (categoryName == null && selectedAvailability == null) return
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
    ) {
        categoryName?.let { ActiveFilterChip(label = it, onClear = onClearCategory) }
        selectedAvailability?.let {
            ActiveFilterChip(label = stringResource(it.filterLabelRes()), onClear = onClearAvailability)
        }
    }
}

/** White with a hairline border: the orange belongs to the pill. */
@Composable
private fun ActiveFilterChip(label: String, onClear: () -> Unit) {
    val clearCd = stringResource(R.string.inventory_filter_clear_cd, label)
    InputChip(
        selected = false,
        onClick = onClear,
        label = { Text(text = label, style = MaterialTheme.typography.bodySmall) },
        trailingIcon = {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = null,
                modifier = Modifier.size(InputChipDefaults.IconSize),
            )
        },
        colors = InputChipDefaults.inputChipColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.semantics { contentDescription = clearCd },
    )
}
