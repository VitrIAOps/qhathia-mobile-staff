package com.qhathia.mobile.staff.feature.productregister.ui

import androidx.activity.compose.BackHandler
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
import com.qhathia.mobile.staff.feature.productregister.model.ProductRegisterUiEffect
import com.qhathia.mobile.staff.feature.productregister.model.ProductRegisterUiEvent

/**
 * Stateful registration entry point: binds [ProductRegisterViewModel], renders
 * [ProductRegisterContent] (which includes the header) and maps one-shot effects to navigation
 * callbacks / a snackbar. Rendered inside the app shell so the bottom navigation stays visible. A
 * dirty form intercepts system back through the discard guard.
 */
@Composable
fun ProductRegisterScreen(
    onRegistered: (String) -> Unit,
    onPlanLimit: () -> Unit,
    onDiscountFailed: (String) -> Unit,
    onCancelled: () -> Unit,
    onNavigateToOrderRequests: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProductRegisterViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is ProductRegisterUiEffect.NavigateBackWithSuccess -> onRegistered(effect.productId)
                ProductRegisterUiEffect.NavigateBackWithPlanLimit -> onPlanLimit()
                is ProductRegisterUiEffect.NavigateBackWithDiscountFailed ->
                    onDiscountFailed(effect.productId)
                ProductRegisterUiEffect.NavigateBackCancelled -> onCancelled()
                ProductRegisterUiEffect.NavigateToOrderRequests -> onNavigateToOrderRequests()
                is ProductRegisterUiEffect.ShowError ->
                    snackbarHostState.showSnackbar(resources.getString(effect.messageRes))
            }
        }
    }

    BackHandler(enabled = uiState.isDirty && !uiState.isSubmitting) {
        viewModel.onEvent(ProductRegisterUiEvent.CancelRequested)
    }

    Box(modifier = modifier.fillMaxSize()) {
        ProductRegisterContent(uiState = uiState, onEvent = viewModel::onEvent)
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
