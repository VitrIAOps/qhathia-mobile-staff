package com.qhathia.mobile.staff.feature.productdetail.ui

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
import com.qhathia.mobile.staff.feature.productdetail.model.ProductDetailUiEffect
import com.qhathia.mobile.staff.feature.productdetail.model.ProductDetailUiEvent

/**
 * Stateful detail/edit entry point: binds [ProductDetailViewModel], renders [ProductDetailContent]
 * (which includes the header) and maps one-shot effects to navigation callbacks / a snackbar. A
 * dirty form intercepts system back through the discard guard.
 */
@Composable
fun ProductDetailScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProductDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                ProductDetailUiEffect.NavigateBackSaved -> onSaved()
                ProductDetailUiEffect.NavigateBackCancelled -> onBack()
                is ProductDetailUiEffect.ShowError ->
                    snackbarHostState.showSnackbar(resources.getString(effect.messageRes))
            }
        }
    }

    BackHandler(enabled = uiState.isDirty && !uiState.isSaving) {
        viewModel.onEvent(ProductDetailUiEvent.CancelRequested)
    }

    Box(modifier = modifier.fillMaxSize()) {
        ProductDetailContent(uiState = uiState, onEvent = viewModel::onEvent)
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
