package com.qhathia.mobile.staff.feature.productinventory.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qhathia.mobile.staff.feature.productinventory.R
import com.qhathia.mobile.staff.feature.productinventory.model.InventoryUiEffect
import com.qhathia.mobile.staff.feature.productinventory.navigation.INVENTORY_REGISTER_RESULT_PLAN_LIMIT
import com.qhathia.mobile.staff.feature.productinventory.navigation.INVENTORY_REGISTER_RESULT_SUCCESS
import com.qhathia.mobile.staff.feature.productinventory.navigation.INVENTORY_RESULT_PRODUCT_UPDATED

/**
 * Stateful inventory entry point: binds [InventoryViewModel], renders [InventoryContent] and
 * surfaces one-shot errors as a snackbar. Reacts to a [registerResult] handed back from a screen it
 * pushed: a successful registration or edit refreshes the list and confirms with a snackbar; a
 * plan-limit result shows the plan-limit message. The app shell's Scaffold provides the window
 * insets.
 */
@Composable
fun InventoryScreen(
    onNavigateToRegister: () -> Unit,
    onNavigateToProductDetail: (String) -> Unit,
    registerResult: String?,
    onRegisterResultConsumed: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InventoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is InventoryUiEffect.ShowError ->
                    snackbarHostState.showSnackbar(resources.getString(effect.messageRes))
                InventoryUiEffect.NavigateToRegister -> onNavigateToRegister()
                is InventoryUiEffect.NavigateToProductDetail ->
                    onNavigateToProductDetail(effect.productId)
            }
        }
    }

    LaunchedEffect(registerResult) {
        when (registerResult) {
            INVENTORY_REGISTER_RESULT_SUCCESS -> {
                viewModel.refresh()
                snackbarHostState.showSnackbar(resources.getString(R.string.inventory_register_success))
                onRegisterResultConsumed()
            }
            INVENTORY_REGISTER_RESULT_PLAN_LIMIT -> {
                snackbarHostState.showSnackbar(resources.getString(R.string.inventory_register_plan_limit))
                onRegisterResultConsumed()
            }
            INVENTORY_RESULT_PRODUCT_UPDATED -> {
                viewModel.refresh()
                snackbarHostState.showSnackbar(resources.getString(R.string.inventory_product_updated))
                onRegisterResultConsumed()
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        InventoryContent(uiState = uiState, onEvent = viewModel::onEvent)
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
