package com.qhathia.mobile.staff.feature.devicelinking.ui

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
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qhathia.mobile.staff.feature.devicelinking.R
import com.qhathia.mobile.staff.feature.devicelinking.model.DeviceLinkingUiEffect
import com.qhathia.mobile.staff.feature.devicelinking.model.DeviceLinkingUiEvent

/**
 * Stateful Dispositivos entry point: binds [DeviceLinkingViewModel], tells it when the screen is
 * visible (command polling follows that), and renders [DeviceLinkingContent]. [onBack] returns to
 * Mi perfil. [unlinkedDeviceId] is the tablet just unlinked from its settings screen, handed back
 * through navigation; [onUnlinkedConsumed] clears it once applied.
 */
@Composable
fun DeviceLinkingScreen(
    onBack: () -> Unit,
    onOpenTabletSettings: (String) -> Unit,
    modifier: Modifier = Modifier,
    unlinkedDeviceId: String? = null,
    unlinkedAlreadyGone: Boolean = false,
    onUnlinkedConsumed: () -> Unit = {},
    viewModel: DeviceLinkingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is DeviceLinkingUiEffect.ShowCommandError ->
                    snackbarHostState.showSnackbar(resources.getString(R.string.device_command_error))
                is DeviceLinkingUiEffect.ShowUnlinked ->
                    snackbarHostState.showSnackbar(
                        resources.getString(
                            if (effect.alreadyGone) R.string.tablet_settings_not_found else R.string.device_unlinked,
                        ),
                    )
                is DeviceLinkingUiEffect.ShowPaired ->
                    snackbarHostState.showSnackbar(
                        resources.getString(
                            R.string.device_pair_success,
                            effect.name ?: resources.getString(R.string.device_unnamed),
                        ),
                    )
                DeviceLinkingUiEffect.ShowPairRejected ->
                    snackbarHostState.showSnackbar(resources.getString(R.string.device_pair_rejected))
                DeviceLinkingUiEffect.ShowPairUndone ->
                    snackbarHostState.showSnackbar(resources.getString(R.string.device_pair_undone))
            }
        }
    }
    LaunchedEffect(unlinkedDeviceId) {
        unlinkedDeviceId?.let {
            viewModel.onEvent(DeviceLinkingUiEvent.DeviceUnlinked(it, unlinkedAlreadyGone))
            onUnlinkedConsumed()
        }
    }
    LifecycleStartEffect(Unit) {
        viewModel.onEvent(DeviceLinkingUiEvent.ScreenStarted)
        onStopOrDispose { viewModel.onEvent(DeviceLinkingUiEvent.ScreenStopped) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        DeviceLinkingContent(
            uiState = uiState,
            onEvent = viewModel::onEvent,
            onBack = onBack,
            onOpenTabletSettings = onOpenTabletSettings,
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
        )
    }
}
