package com.qhathia.mobile.staff.feature.salesstatistics.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.core.app.ShareCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qhathia.mobile.staff.core.ui.component.time.rememberShortTimeFormatter
import com.qhathia.mobile.staff.feature.salesstatistics.R
import com.qhathia.mobile.staff.feature.salesstatistics.model.SalesStatisticsUiEffect
import com.qhathia.mobile.staff.feature.salesstatistics.model.SalesStatisticsUiEvent

/**
 * Stateful "Resumen de ventas" entry point: binds [SalesStatisticsViewModel], renders
 * [SalesStatisticsContent], raises the back navigation to the shell, opens the share sheet, and
 * tells the ViewModel each time the screen comes back so it can reload.
 */
@Composable
fun SalesStatisticsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SalesStatisticsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnBack by rememberUpdatedState(onBack)
    val currentState by rememberUpdatedState(uiState)
    val context = LocalContext.current
    val resources = LocalResources.current
    val locale = LocalConfiguration.current.locales[0]
    val rangeFormatter by rememberUpdatedState(rememberShortTimeFormatter(zone = LIMA))

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                SalesStatisticsUiEffect.NavigateBack -> currentOnBack()
                is SalesStatisticsUiEffect.Share -> {
                    val state = currentState
                    val (start, end) = state.boundsOf(effect.tab)
                    val range = periodRangeLabel(state.period, start, end, locale, rangeFormatter)
                    val text = shareText(effect.tab, state, range) { id, args -> resources.getString(id, *args) }
                    if (text != null) {
                        ShareCompat.IntentBuilder(context)
                            .setType("text/plain")
                            .setText(text)
                            .setChooserTitle(R.string.statistics_share_title)
                            .startChooser()
                    }
                }
            }
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.onEvent(SalesStatisticsUiEvent.ScreenStarted) }

    SalesStatisticsContent(uiState = uiState, onEvent = viewModel::onEvent, modifier = modifier)
}
