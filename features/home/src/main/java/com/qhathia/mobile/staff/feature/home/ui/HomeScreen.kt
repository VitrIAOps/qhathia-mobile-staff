package com.qhathia.mobile.staff.feature.home.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qhathia.mobile.staff.core.domain.model.SalesPeriod
import com.qhathia.mobile.staff.feature.home.model.HomeUiEffect
import com.qhathia.mobile.staff.feature.home.model.HomeUiEvent

/**
 * Stateful Inicio entry point: binds [HomeViewModel], renders [HomeContent] and raises navigation to
 * the shell — the bell as a tab switch to Solicitudes, `Ver más` as a push of the sales statistics,
 * `Agregar` and `Reabastecer` as Registro and Detalle (which the shell opens over Inventario).
 * Tells the ViewModel each time the tab comes back on screen, so it can reload.
 */
@Composable
fun HomeScreen(
    onNavigateToOrderRequests: () -> Unit,
    onNavigateToSalesStatistics: (SalesPeriod) -> Unit,
    onNavigateToProductRegister: (prefillName: String) -> Unit,
    onNavigateToProductDetail: (productId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnNavigateToOrderRequests by rememberUpdatedState(onNavigateToOrderRequests)
    val currentOnNavigateToSalesStatistics by rememberUpdatedState(onNavigateToSalesStatistics)
    val currentOnNavigateToProductRegister by rememberUpdatedState(onNavigateToProductRegister)
    val currentOnNavigateToProductDetail by rememberUpdatedState(onNavigateToProductDetail)

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                HomeUiEffect.NavigateToOrderRequests -> currentOnNavigateToOrderRequests()
                is HomeUiEffect.NavigateToSalesStatistics ->
                    currentOnNavigateToSalesStatistics(effect.period)
                is HomeUiEffect.NavigateToProductRegister ->
                    currentOnNavigateToProductRegister(effect.prefillName)
                is HomeUiEffect.NavigateToProductDetail ->
                    currentOnNavigateToProductDetail(effect.productId)
            }
        }
    }

    // The tab's back-stack entry drops below STARTED whenever another tab, a pushed screen or the
    // background hides it, so ON_START marks every return to Inicio.
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.onEvent(HomeUiEvent.ScreenStarted) }

    HomeContent(uiState = uiState, onEvent = viewModel::onEvent, modifier = modifier)
}
