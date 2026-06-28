package com.qhathia.mobile.staff.feature.productinventory.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.staff.core.domain.model.Availability
import com.qhathia.mobile.staff.core.domain.model.Category
import com.qhathia.mobile.staff.core.domain.model.Product
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme
import com.qhathia.mobile.staff.feature.productinventory.R
import com.qhathia.mobile.staff.feature.productinventory.model.InventoryUiEvent
import com.qhathia.mobile.staff.feature.productinventory.model.InventoryUiState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * Stateless inventory UI — a pure function of [uiState]. Lays out the header card, search, category
 * segmented control and count bar over a gray background, with the product list inside a white card
 * (loading / empty / error states handled), plus the registration FAB floating over it.
 */
@Composable
fun InventoryContent(
    uiState: InventoryUiState,
    onEvent: (InventoryUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = Dimens.SpaceSm)
                .padding(horizontal = Dimens.SpaceMd)
        ) {
            InventoryHeader()
            Spacer(Modifier.height(Dimens.SpaceMd))
            InventorySearchField(
                query = uiState.searchQuery,
                onQueryChange = { onEvent(InventoryUiEvent.Search(it)) },
            )
            Spacer(Modifier.height(Dimens.SpaceSm))
            CategoryFilterRow(
                categories = uiState.categories,
                selectedCategoryId = uiState.selectedCategoryId,
                onSelectCategory = { onEvent(InventoryUiEvent.SelectCategory(it)) }
            )
            ProductCountAndFilterBar(
                productCount = uiState.productCount,
                selectedAvailability = uiState.selectedAvailability,
                onSelectAvailability = { onEvent(InventoryUiEvent.SelectAvailability(it)) },
                onClearAvailability = { onEvent(InventoryUiEvent.ClearAvailability) }
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = Dimens.SpaceSm),
            ) {
                when {
                    uiState.isLoading && uiState.loadedProducts.isEmpty() ->
                        LoadingState()
                    uiState.isError && uiState.loadedProducts.isEmpty() ->
                        ErrorState(onRetry = { onEvent(InventoryUiEvent.Retry) })
                    uiState.visibleProducts.isEmpty() ->
                        EmptyState()
                    else ->
                        ProductListCard(products = uiState.visibleProducts)
                }
            }
        }

        FloatingActionButton(
            onClick = { onEvent(InventoryUiEvent.RegisterProductClicked) },
            shape = RoundedCornerShape(Dimens.ButtonRadius),
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(18.dp),
        ) {
            Icon(
                modifier = Modifier.size(32.dp),
                imageVector = Icons.Rounded.Add,
                contentDescription = stringResource(R.string.inventory_register_cd),
            )
        }
    }
}

@Composable
private fun ProductListCard(
    products: ImmutableList<Product>,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(Dimens.SpaceMd),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(Dimens.SpaceMd),
        ) {
            itemsIndexed(products, key = { _, product -> product.id }) { index, product ->
                ProductRow(product = product)
                if (index < products.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.inventory_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(Dimens.SpaceLg),
        )
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
                text = stringResource(R.string.inventory_error),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.inventory_retry))
            }
        }
    }
}

// ---- Previews ----

private fun sampleProducts(): ImmutableList<Product> =
    List(6) { index ->
        Product(
            id = "p$index",
            categoryId = "c1",
            categoryName = "Electrónica",
            name = "Taladro Percutor Bosch GSB 13 RE",
            price = 189.0,
            availability = Availability.AVAILABLE,
            primaryImageUrl = null,
        )
    }.toImmutableList()

private fun sampleCategories() = persistentListOf(
    Category(id = "c1", name = "Electrónica"),
    Category(id = "c2", name = "Gasfitería"),
    Category(id = "c3", name = "Pinturas"),
    Category(id = "c4", name = "Pisos"),
    Category(id = "c5", name = "Iluminación"),
)

@Preview(name = "Inventory — light", showBackground = true)
@Preview(name = "Inventory — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun InventoryContentPreview() {
    QhathiaTheme {
        InventoryContent(
            uiState = InventoryUiState(
                loadedProducts = sampleProducts(),
                visibleProducts = sampleProducts(),
                categories = sampleCategories(),
            ),
            onEvent = {},
        )
    }
}

@Preview(name = "Inventory — empty", showBackground = true)
@Composable
private fun InventoryContentEmptyPreview() {
    QhathiaTheme {
        InventoryContent(
            uiState = InventoryUiState(categories = sampleCategories()),
            onEvent = {},
        )
    }
}

@Preview(name = "Inventory — error", showBackground = true)
@Composable
private fun InventoryContentErrorPreview() {
    QhathiaTheme {
        InventoryContent(
            uiState = InventoryUiState(isError = true),
            onEvent = {},
        )
    }
}

@Preview(name = "Inventory — loading", showBackground = true)
@Composable
private fun InventoryContentLoadingPreview() {
    QhathiaTheme {
        InventoryContent(
            uiState = InventoryUiState(isLoading = true),
            onEvent = {},
        )
    }
}
