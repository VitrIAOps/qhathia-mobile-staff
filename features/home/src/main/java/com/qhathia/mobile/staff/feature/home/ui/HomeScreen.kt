package com.qhathia.mobile.staff.feature.home.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qhathia.mobile.staff.feature.home.model.HomeUiEffect

/**
 * Stateful Inicio entry point: binds [HomeViewModel], renders [HomeContent] and raises the bell's
 * navigation to the shell, which fulfils it as a tab switch.
 */
@Composable
fun HomeScreen(
    onNavigateToOrderRequests: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnNavigate by rememberUpdatedState(onNavigateToOrderRequests)

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                HomeUiEffect.NavigateToOrderRequests -> currentOnNavigate()
            }
        }
    }

    HomeContent(uiState = uiState, onEvent = viewModel::onEvent, modifier = modifier)
}
