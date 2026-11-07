package com.qhathia.mobile.staff.feature.productinventory.ui

import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
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
import com.qhathia.mobile.staff.core.domain.model.DiscountDraft
import com.qhathia.mobile.staff.core.domain.model.Product
import com.qhathia.mobile.staff.core.ui.R as CoreUiR
import com.qhathia.mobile.staff.core.ui.component.QhathiaDeleteProductDialog
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaColors
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme
import com.qhathia.mobile.staff.feature.productinventory.R
import com.qhathia.mobile.staff.feature.productinventory.model.InventoryUiEvent
import com.qhathia.mobile.staff.feature.productinventory.model.InventoryUiState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * Stateless inventory UI — a pure function of [uiState]. Lays out the full-bleed gradient header
 * (title + search) over a gray background, then the filter/count bar and the product list inside a
 * white card (loading / empty / error states handled), plus the registration FAB floating over it.
 */
@Composable
fun InventoryContent(
    uiState: InventoryUiState,
    onEvent: (InventoryUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    onOpenOrderRequests: () -> Unit = {},
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            InventoryHeader(
                searchQuery = uiState.searchQuery,
                onSearch = { onEvent(InventoryUiEvent.Search(it)) },
                onOpenOrderRequests = onOpenOrderRequests,
            )
            Spacer(Modifier.height(Dimens.SpaceMd))
            ProductCountAndFilterBar(
                productCount = uiState.productCount,
                categories = uiState.categories,
                selectedCategoryId = uiState.selectedCategoryId,
                selectedAvailability = uiState.selectedAvailability,
                onSelectCategory = { onEvent(InventoryUiEvent.SelectCategory(it)) },
                onSelectAvailability = { onEvent(InventoryUiEvent.SelectAvailability(it)) },
                onClearAvailability = { onEvent(InventoryUiEvent.ClearAvailability) },
                modifier = Modifier.padding(horizontal = Dimens.SpaceMd),
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.SpaceMd)
                    .padding(top = Dimens.SpaceMd, bottom = Dimens.SpaceSm),
            ) {
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = { onEvent(InventoryUiEvent.PullToRefresh) },
                    modifier = Modifier.fillMaxSize(),
                ) {
                when {
                    uiState.isLoading && uiState.loadedProducts.isEmpty() ->
                        LoadingState()
                    uiState.isError && uiState.loadedProducts.isEmpty() ->
                        ErrorState(onRetry = { onEvent(InventoryUiEvent.Retry) })
                    uiState.visibleProducts.isEmpty() ->
                        EmptyState()
                    else ->
                        ProductListCard(
                            products = uiState.visibleProducts,
                            listState = listState,
                            onProductClick = { onEvent(InventoryUiEvent.ProductClicked(it)) },
                            onEditDiscount = { onEvent(InventoryUiEvent.EditDiscountClicked(it)) },
                            onDelete = { onEvent(InventoryUiEvent.DeleteProductClicked(it)) },
                            onActionsOpened = { onEvent(InventoryUiEvent.RowActionsOpened(it)) },
                            hasDiscount = uiState::hasDiscount,
                            discountLookupProductId = uiState.discountLookupProductId,
                        )
                }
                }
            }
        }

        // DS button: a 40dp square with 8dp corners, not the 56dp circular M3 default.
        SmallFloatingActionButton(
            onClick = { onEvent(InventoryUiEvent.RegisterProductClicked) },
            shape = RoundedCornerShape(Dimens.FieldRadius),
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(18.dp),
        ) {
            Icon(
                modifier = Modifier.size(24.dp),
                imageVector = Icons.Rounded.Add,
                contentDescription = stringResource(R.string.inventory_register_cd),
            )
        }

        if (uiState.isDiscountLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        uiState.discountDraft?.let { draft ->
            DiscountDialog(
                draft = draft,
                isSaving = uiState.isDiscountSaving,
                onDraftChange = { onEvent(InventoryUiEvent.DiscountDraftChanged(it)) },
                onApply = { onEvent(InventoryUiEvent.DiscountApplied) },
                onDismiss = { onEvent(InventoryUiEvent.DiscountDismissed) },
            )
        }

        uiState.deleteCandidate?.let { product ->
            QhathiaDeleteProductDialog(
                productName = product.name,
                isDeleting = uiState.isDeleting,
                onConfirm = { onEvent(InventoryUiEvent.DeleteConfirmed) },
                onDismiss = { onEvent(InventoryUiEvent.DeleteDismissed) },
                pendingOrdersDetail = uiState.pendingOrders?.let { warning ->
                    warning.detail ?: stringResource(
                        CoreUiR.string.delete_product_pending_orders_fallback,
                        warning.orderCodes.joinToString { "#$it" },
                    )
                },
            )
        }
    }
}

/**
 * The DS list card: a white, rounded, softly-shadowed sheet the rows scroll inside. Every row
 * carries a hairline underneath — including the last, so a partially-scrolled list reads as
 * continuing past the fold rather than ending there.
 */
@Composable
private fun ProductListCard(
    products: ImmutableList<Product>,
    listState: LazyListState,
    onProductClick: (String) -> Unit,
    onEditDiscount: (String) -> Unit,
    onDelete: (String) -> Unit,
    onActionsOpened: (String) -> Unit,
    hasDiscount: (String) -> Boolean?,
    discountLookupProductId: String?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(Dimens.CardRadius),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(Dimens.SpaceMd),
        ) {
            items(products, key = { product -> product.id }) { product ->
                ProductRow(
                    product = product,
                    onClick = { onProductClick(product.id) },
                    onEditDiscount = { onEditDiscount(product.id) },
                    onDelete = { onDelete(product.id) },
                    onActionsOpened = { onActionsOpened(product.id) },
                    hasDiscount = hasDiscount(product.id),
                    isDiscountLookupLoading = discountLookupProductId == product.id,
                )
                HorizontalDivider(color = QhathiaColors.rowDivider)
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
    PullableCenter(modifier) {
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
    PullableCenter(modifier) {
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

/** Rows covering every stock state (n, 0, unknown) and both price states (plain, discounted). */
private fun sampleProducts(): ImmutableList<Product> =
    listOf(
        12 to null,
        1 to 151.2,
        0 to null,
        null to null,
        99 to 174.0,
        5 to null,
    ).mapIndexed { index, (stock, discountedPrice) ->
        Product(
            id = "p$index",
            categoryId = "c1",
            categoryName = "Electrónica",
            name = "Taladro Percutor Bosch GSB 13 RE",
            price = 189.0,
            availability = Availability.AVAILABLE,
            primaryImageUrl = null,
            stock = stock,
            discountedPrice = discountedPrice,
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

// A single result reads "1 producto", not "1 productos".
@Preview(name = "Inventory — one result", showBackground = true)
@Preview(name = "Inventory — one result dark", showBackground = true, uiMode = 0x20)
@Composable
private fun InventoryContentOneResultPreview() {
    QhathiaTheme {
        InventoryContent(
            uiState = InventoryUiState(
                loadedProducts = sampleProducts(),
                visibleProducts = sampleProducts().take(1).toImmutableList(),
                categories = sampleCategories(),
                searchQuery = "Taladro",
            ),
            onEvent = {},
        )
    }
}

@Preview(name = "Inventory — search and discount dialog", showBackground = true)
@Preview(name = "Inventory — search and discount dialog dark", showBackground = true, uiMode = 0x20)
@Composable
private fun InventoryContentDiscountDialogPreview() {
    QhathiaTheme {
        InventoryContent(
            uiState = InventoryUiState(
                loadedProducts = sampleProducts(),
                visibleProducts = sampleProducts(),
                categories = sampleCategories(),
                searchQuery = "Taladro",
                discountProductId = sampleProducts().first().id,
                discountDraft = DiscountDraft(price = sampleProducts().first().price, input = "20"),
            ),
            onEvent = {},
        )
    }
}

@Preview(name = "Inventory — delete confirmation", showBackground = true)
@Preview(name = "Inventory — delete confirmation dark", showBackground = true, uiMode = 0x20)
@Composable
private fun InventoryContentDeleteDialogPreview() {
    QhathiaTheme {
        InventoryContent(
            uiState = InventoryUiState(
                loadedProducts = sampleProducts(),
                visibleProducts = sampleProducts(),
                categories = sampleCategories(),
                deleteCandidate = sampleProducts().first(),
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

/**
 * Centers [content] in the space it gets while staying scrollable, so pull-to-refresh also works on
 * a state with nothing to scroll (empty list, error).
 */
@Composable
private fun PullableCenter(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
                .heightIn(min = maxHeight),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}
