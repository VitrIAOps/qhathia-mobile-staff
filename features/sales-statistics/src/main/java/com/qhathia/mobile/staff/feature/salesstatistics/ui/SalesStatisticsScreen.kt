package com.qhathia.mobile.staff.feature.salesstatistics.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qhathia.mobile.staff.feature.salesstatistics.model.SalesStatisticsUiEffect

/**
 * Stateful "Resumen de ventas" entry point: binds [SalesStatisticsViewModel], renders
 * [SalesStatisticsContent] and raises the back navigation to the shell.
 */
@Composable
fun SalesStatisticsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SalesStatisticsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnBack by rememberUpdatedState(onBack)

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                SalesStatisticsUiEffect.NavigateBack -> currentOnBack()
            }
        }
    }

    SalesStatisticsContent(uiState = uiState, onEvent = viewModel::onEvent, modifier = modifier)
}
