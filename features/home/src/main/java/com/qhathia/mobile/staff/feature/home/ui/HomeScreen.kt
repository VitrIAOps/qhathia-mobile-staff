package com.qhathia.mobile.staff.feature.home.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qhathia.mobile.staff.core.domain.model.SalesPeriod
import com.qhathia.mobile.staff.feature.home.model.HomeUiEffect

/**
 * Stateful Inicio entry point: binds [HomeViewModel], renders [HomeContent] and raises navigation to
 * the shell — the bell as a tab switch to Solicitudes, `Ver más` as a push of the sales statistics.
 */
@Composable
fun HomeScreen(
    onNavigateToOrderRequests: () -> Unit,
    onNavigateToSalesStatistics: (SalesPeriod) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnNavigateToOrderRequests by rememberUpdatedState(onNavigateToOrderRequests)
    val currentOnNavigateToSalesStatistics by rememberUpdatedState(onNavigateToSalesStatistics)

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                HomeUiEffect.NavigateToOrderRequests -> currentOnNavigateToOrderRequests()
                is HomeUiEffect.NavigateToSalesStatistics ->
                    currentOnNavigateToSalesStatistics(effect.period)
            }
        }
    }

    HomeContent(uiState = uiState, onEvent = viewModel::onEvent, modifier = modifier)
}
