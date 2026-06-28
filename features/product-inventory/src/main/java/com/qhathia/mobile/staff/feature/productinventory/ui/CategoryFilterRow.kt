package com.qhathia.mobile.staff.feature.productinventory.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qhathia.mobile.staff.core.domain.model.Category
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.feature.productinventory.R
import kotlinx.collections.immutable.ImmutableList

/**
 * Horizontal segmented control (DS "Tabs"): a rounded track holding pill tabs. A leading "Todas"
 * tab (null selection) lists all products; selecting a category re-queries by its id. The selected
 * tab is a white, shadowed pill; the rest are plain text.
 */
@Composable
internal fun CategoryFilterRow(
    categories: ImmutableList<Category>,
    selectedCategoryId: String?,
    onSelectCategory: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .horizontalScroll(rememberScrollState())
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        CategoryTab(
            label = stringResource(R.string.inventory_category_all),
            selected = selectedCategoryId == null,
            onClick = { onSelectCategory(null) },
        )
        categories.forEach { category ->
            CategoryTab(
                label = category.name,
                selected = selectedCategoryId == category.id,
                onClick = { onSelectCategory(category.id) },
            )
        }
    }
}

@Composable
private fun CategoryTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    if (selected) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.3.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = Dimens.SpaceMd, vertical = Dimens.SpaceSm),
            )
        }
    } else {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = Dimens.SpaceMd, vertical = Dimens.SpaceSm),
        )
    }
}
