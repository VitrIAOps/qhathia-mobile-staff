package com.qhathia.mobile.staff.feature.devicelinking.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Stateful linked-devices entry point: binds [DeviceLinkingViewModel] and renders
 * [DeviceLinkingContent]. [onBack] returns to the previous destination (the Profile section).
 */
@Composable
fun DeviceLinkingScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DeviceLinkingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DeviceLinkingContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
        modifier = modifier,
    )
}
