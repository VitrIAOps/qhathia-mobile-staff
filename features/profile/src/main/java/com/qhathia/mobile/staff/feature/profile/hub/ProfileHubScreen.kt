package com.qhathia.mobile.staff.feature.profile.hub

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Stateful Mi perfil entry point: binds [ProfileHubViewModel], refreshes on every resume so values
 * changed in a sub-screen show on return, and raises navigation to the graph.
 */
@Composable
fun ProfileHubScreen(
    onNavigate: (ProfileDestination) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileHubViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnNavigate by rememberUpdatedState(onNavigate)

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is ProfileHubUiEffect.Navigate -> currentOnNavigate(effect.destination)
            }
        }
    }
    LifecycleResumeEffect(Unit) {
        viewModel.onEvent(ProfileHubUiEvent.Refresh)
        onPauseOrDispose { }
    }

    ProfileHubContent(uiState = uiState, onEvent = viewModel::onEvent, modifier = modifier)
}
