package com.qhathia.mobile.staff.feature.productregister.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.qhathia.mobile.staff.core.domain.model.Category
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.feature.productregister.R
import kotlinx.collections.immutable.ImmutableList

/**
 * Single-select category picker (required): wrapping pill chips, with loading and retry states. The
 * selected chip is filled with the brand accent and shows a check; the rest are outlined.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategorySelector(
    categories: ImmutableList<Category>,
    selectedCategoryId: String?,
    isLoading: Boolean,
    isError: Boolean,
    onSelect: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = MaterialTheme.colorScheme
    Box(modifier = modifier) {
        CatalogStateSlot(isLoading = isLoading, isError = isError, onRetry = onRetry) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
            ) {
                categories.forEach { category ->
                    val selected = category.id == selectedCategoryId
                    FilterChip(
                        selected = selected,
                        onClick = { onSelect(category.id) },
                        enabled = enabled,
                        shape = CircleShape,
                        label = { Text(category.name) },
                        leadingIcon = if (selected) {
                            { Icon(Icons.Filled.Check, contentDescription = null) }
                        } else {
                            null
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = colors.surface,
                            labelColor = colors.onSurface,
                            selectedContainerColor = colors.secondary,
                            selectedLabelColor = colors.onSecondary,
                            selectedLeadingIconColor = colors.onSecondary,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = enabled,
                            selected = selected,
                            borderColor = colors.secondary,
                            selectedBorderColor = colors.secondary,
                        ),
                    )
                }
            }
        }
    }
}

/** Shared loading / error-retry / content slot for the catalog selectors. */
@Composable
internal fun CatalogStateSlot(
    isLoading: Boolean,
    isError: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    when {
        isLoading -> Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(Dimens.SpaceSm),
            contentAlignment = Alignment.CenterStart,
        ) {
            CircularProgressIndicator()
        }
        isError -> TextButton(onClick = onRetry, modifier = modifier) {
            Text(stringResource(R.string.register_retry))
        }
        else -> content()
    }
}
