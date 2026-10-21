package com.qhathia.mobile.staff.feature.productinventory.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qhathia.mobile.staff.feature.productinventory.R
import com.qhathia.mobile.staff.feature.productinventory.model.InventoryUiEffect
import com.qhathia.mobile.staff.feature.productinventory.model.InventoryUiEvent
import com.qhathia.mobile.staff.feature.productinventory.navigation.INVENTORY_REGISTER_RESULT_DISCOUNT_FAILED
import com.qhathia.mobile.staff.feature.productinventory.navigation.INVENTORY_REGISTER_RESULT_PLAN_LIMIT
import com.qhathia.mobile.staff.feature.productinventory.navigation.INVENTORY_REGISTER_RESULT_SUCCESS
import com.qhathia.mobile.staff.feature.productinventory.navigation.INVENTORY_RESULT_PRODUCT_UPDATED
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Stateful inventory entry point: binds [InventoryViewModel], renders [InventoryContent] and
 * surfaces one-shot errors as a snackbar. Reacts to a [registerResult] handed back from a screen it
 * pushed: a successful registration or edit refreshes the list and confirms with a snackbar; a
 * plan-limit result shows the plan-limit message; a registration whose discount failed refreshes
 * and says so. The app shell's Scaffold provides the window insets.
 *
 * A registration also hands back [registeredProductId], and its snackbar then offers "Ver producto",
 * which scrolls the list to that product's row.
 */
@Composable
fun InventoryScreen(
    onNavigateToRegister: () -> Unit,
    onNavigateToProductDetail: (String) -> Unit,
    onNavigateToOrderRequests: () -> Unit,
    registerResult: String?,
    registeredProductId: String?,
    onRegisterResultConsumed: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InventoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val resources = LocalResources.current

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is InventoryUiEffect.ShowError ->
                    snackbarHostState.showSnackbar(resources.getString(effect.messageRes))
                is InventoryUiEffect.ShowMessage ->
                    snackbarHostState.showSnackbar(resources.getString(effect.messageRes))
                InventoryUiEffect.NavigateToRegister -> onNavigateToRegister()
                is InventoryUiEffect.NavigateToProductDetail ->
                    onNavigateToProductDetail(effect.productId)
                is InventoryUiEffect.ScrollToProduct ->
                    // Launched so a long scroll does not hold back the next effect. The list this
                    // index belongs to may still be a frame away from being laid out.
                    launch {
                        snapshotFlow { listState.layoutInfo.totalItemsCount }.first { it > effect.index }
                        listState.animateScrollToItem(effect.index)
                    }
            }
        }
    }

    // An action label makes a snackbar indefinite by default, hence the explicit duration.
    suspend fun showRegisteredSnackbar(@StringRes messageRes: Int, productId: String?) {
        val result = snackbarHostState.showSnackbar(
            message = resources.getString(messageRes),
            actionLabel = productId?.let {
                resources.getString(R.string.inventory_register_view_product)
            },
            duration = SnackbarDuration.Long,
        )
        if (result == SnackbarResult.ActionPerformed && productId != null) {
            viewModel.onEvent(InventoryUiEvent.ViewRegisteredProductClicked(productId))
        }
    }

    // The result is consumed first, before any snackbar is awaited. `showSnackbar` suspends until
    // the snackbar is dismissed, so consuming after it meant that leaving the screen mid-snackbar
    // cancelled the effect, left the result in the back-stack entry, and showed it again on return.
    // Consuming clears `registerResult`, which restarts this effect and cancels it, so the snackbar
    // runs in `resultScope` instead; that scope ends when the screen leaves composition. The product
    // id is captured because consuming clears it too.
    val resultScope = rememberCoroutineScope()
    LaunchedEffect(registerResult) {
        val result = registerResult ?: return@LaunchedEffect
        val productId = registeredProductId
        onRegisterResultConsumed()

        resultScope.launch {
            when (result) {
                INVENTORY_REGISTER_RESULT_SUCCESS -> {
                    viewModel.refresh()
                    showRegisteredSnackbar(R.string.inventory_register_success, productId)
                }
                INVENTORY_REGISTER_RESULT_PLAN_LIMIT ->
                    snackbarHostState.showSnackbar(resources.getString(R.string.inventory_register_plan_limit))
                INVENTORY_REGISTER_RESULT_DISCOUNT_FAILED -> {
                    viewModel.refresh()
                    showRegisteredSnackbar(R.string.inventory_register_discount_failed, productId)
                }
                INVENTORY_RESULT_PRODUCT_UPDATED -> {
                    viewModel.refresh()
                    snackbarHostState.showSnackbar(resources.getString(R.string.inventory_product_updated))
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        InventoryContent(
            uiState = uiState,
            onEvent = viewModel::onEvent,
            listState = listState,
            onOpenOrderRequests = onNavigateToOrderRequests,
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        ) { data ->
            // "Ver producto" in the brand orange rather than the M3 default action tint.
            Snackbar(snackbarData = data, actionColor = MaterialTheme.colorScheme.secondary)
        }
    }
}
