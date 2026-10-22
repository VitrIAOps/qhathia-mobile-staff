package com.qhathia.mobile.staff.feature.devicelinking.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
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

/**
 * Stateful entry point of Ajustes de la tablet: binds the ViewModel and shows its messages.
 * [onUnlinked] receives the tablet's id once it is unlinked, to close the screen.
 */
@Composable
fun TabletSettingsScreen(
    onBack: () -> Unit,
    onUnlinked: (deviceId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TabletSettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is TabletSettingsUiEffect.ShowMessage ->
                    snackbarHostState.showSnackbar(resources.getString(effect.messageRes))
                is TabletSettingsUiEffect.Unlinked -> onUnlinked(effect.deviceId)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        TabletSettingsContent(uiState = uiState, onEvent = viewModel::onEvent, onBack = onBack)
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
        )
    }
}
