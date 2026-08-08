package com.qhathia.mobile.staff.feature.orderrequests.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qhathia.mobile.staff.core.ui.theme.QhathiaColors
import com.qhathia.mobile.staff.feature.orderrequests.R
import com.qhathia.mobile.staff.feature.orderrequests.model.OrderRequestsUiEffect
import com.qhathia.mobile.staff.feature.orderrequests.model.SnackbarSeverity

/**
 * Stateful Solicitudes entry point: binds [OrderRequestsViewModel], renders [OrderRequestsContent]
 * and turns one-shot effects into the design's coloured bottom snackbars — green for a confirmed
 * order, red for a rejected one or an error. The app shell's Scaffold provides the window insets.
 */
@Composable
fun OrderRequestsScreen(
    modifier: Modifier = Modifier,
    viewModel: OrderRequestsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    // The severity of the message currently being shown, so the host can colour it. Set before the
    // (suspending) showSnackbar call, which serializes them one at a time.
    var severity by remember { mutableStateOf(SnackbarSeverity.SUCCESS) }

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            val message = when (effect) {
                OrderRequestsUiEffect.ShowConfirmedSnackbar ->
                    resources.getString(R.string.order_confirmed_snackbar)
                OrderRequestsUiEffect.ShowRejectedSnackbar ->
                    resources.getString(R.string.order_rejected_snackbar)
                is OrderRequestsUiEffect.ShowError ->
                    if (effect.productName != null) {
                        resources.getString(effect.messageRes, effect.productName)
                    } else {
                        resources.getString(effect.messageRes)
                    }
            }
            severity = effect.severity
            snackbarHostState.showSnackbar(message)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        OrderRequestsContent(uiState = uiState, onEvent = viewModel::onEvent)
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = when (severity) {
                    SnackbarSeverity.SUCCESS -> QhathiaColors.success
                    SnackbarSeverity.ERROR -> MaterialTheme.colorScheme.error
                },
                contentColor = when (severity) {
                    SnackbarSeverity.SUCCESS -> QhathiaColors.onSuccess
                    SnackbarSeverity.ERROR -> MaterialTheme.colorScheme.onError
                },
            )
        }
    }
}
