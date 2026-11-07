package com.qhathia.mobile.staff.feature.salesstatistics.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.staff.core.domain.model.Category
import com.qhathia.mobile.staff.core.domain.model.SalesPeriod
import com.qhathia.mobile.staff.core.ui.component.QhathiaSegmentedControl
import com.qhathia.mobile.staff.core.ui.component.sales.selectorLabel
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaColors
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme
import com.qhathia.mobile.staff.feature.salesstatistics.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

private val PERIODS = SalesPeriod.entries.toImmutableList()
private val PanelInnerPadding = 20.dp

/**
 * The collapsible "Filtros" card: the period pills and, where the tab supports it, the product
 * category dropdown ("Todos" plus the store's categories).
 */
@Composable
internal fun FiltersPanel(
    isExpanded: Boolean,
    period: SalesPeriod,
    isCategoryVisible: Boolean,
    categories: ImmutableList<Category>,
    selectedCategoryId: String?,
    onToggle: () -> Unit,
    onSelectPeriod: (SalesPeriod) -> Unit,
    onSelectCategory: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val expansionState = stringResource(
        if (isExpanded) R.string.statistics_filters_expanded else R.string.statistics_filters_collapsed,
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(Dimens.CardRadius)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Dimens.CardRadius))
                .clickable(role = Role.Button, onClick = onToggle)
                .semantics { stateDescription = expansionState }
                .padding(Dimens.SpaceMd),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.statistics_filters),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                // The row's state description already says whether the panel is open.
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }

        if (!isExpanded) return@Column

        Column(
            modifier = Modifier.padding(
                start = PanelInnerPadding,
                end = PanelInnerPadding,
                bottom = Dimens.SpaceMd,
            ),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
        ) {
            QhathiaSegmentedControl(
                options = PERIODS,
                selected = period,
                onSelect = onSelectPeriod,
                label = { it.toUi().selectorLabel() },
                trackColor = QhathiaColors.filterTrack,
            )
            if (isCategoryVisible) {
                Text(
                    text = stringResource(R.string.statistics_category_label),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                CategoryDropdown(
                    categories = categories,
                    selectedCategoryId = selectedCategoryId,
                    onSelect = onSelectCategory,
                )
            }
        }
    }
}

/** An outlined field anchoring a menu of "Todos" and the store's categories. */
@Composable
private fun CategoryDropdown(
    categories: ImmutableList<Category>,
    selectedCategoryId: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val allLabel = stringResource(R.string.statistics_category_all)
    val selectedName = categories.firstOrNull { it.id == selectedCategoryId }?.name ?: allLabel
    val fieldShape = RoundedCornerShape(Dimens.FieldRadius)

    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Dimens.FieldHeight)
                .clip(fieldShape)
                .border(1.dp, MaterialTheme.colorScheme.outline, fieldShape)
                .clickable(role = Role.DropdownList) { expanded = true }
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = selectedName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(allLabel) },
                onClick = {
                    expanded = false
                    onSelect(null)
                },
            )
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = {
                        expanded = false
                        onSelect(category.id)
                    },
                )
            }
        }
    }
}

@Preview(name = "Filters, expanded — light", showBackground = true)
@Preview(name = "Filters, expanded — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun FiltersPanelExpandedPreview() {
    QhathiaTheme {
        FiltersPanel(
            isExpanded = true,
            period = SalesPeriod.WEEKLY,
            isCategoryVisible = true,
            categories = SalesStatisticsPreviewData.categories,
            selectedCategoryId = null,
            onToggle = {},
            onSelectPeriod = {},
            onSelectCategory = {},
            modifier = Modifier.padding(Dimens.SpaceMd),
        )
    }
}
