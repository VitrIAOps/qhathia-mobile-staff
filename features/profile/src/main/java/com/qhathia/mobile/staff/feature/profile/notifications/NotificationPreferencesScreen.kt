package com.qhathia.mobile.staff.feature.profile.notifications

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qhathia.mobile.staff.core.domain.model.NotificationPreference
import com.qhathia.mobile.staff.core.domain.model.NotificationPreferences
import com.qhathia.mobile.staff.core.ui.component.settings.QhathiaGradientTopBar
import com.qhathia.mobile.staff.core.ui.component.settings.QhathiaSettingsSection
import com.qhathia.mobile.staff.core.ui.component.settings.QhathiaSwitchRow
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme
import com.qhathia.mobile.staff.feature.profile.R
import com.qhathia.mobile.staff.feature.profile.common.ErrorPane
import com.qhathia.mobile.staff.feature.profile.common.LoadState
import com.qhathia.mobile.staff.feature.profile.common.LoadingPane

private data class ToggleSpec(
    val preference: NotificationPreference,
    @param:StringRes val title: Int,
    @param:StringRes val subtitle: Int? = null,
)

private data class ToggleGroup(@param:StringRes val title: Int, val toggles: List<ToggleSpec>)

/**
 * The groups are presentation only: the backend returns the seven toggles flat. Which of them are
 * live comes from the backend's `supported`; the rest show "Próximamente".
 */
private val Groups = listOf(
    ToggleGroup(
        R.string.notifications_group_orders,
        listOf(
            ToggleSpec(NotificationPreference.NEW_ORDER, R.string.notifications_new_order),
            // Its subtitle is a plural over the backend's warning minutes, drawn in ToggleGroupSection.
            ToggleSpec(NotificationPreference.ORDER_EXPIRING, R.string.notifications_order_expiring),
        ),
    ),
    ToggleGroup(
        R.string.notifications_group_tablet,
        listOf(
            ToggleSpec(NotificationPreference.TABLET_DISCONNECTED, R.string.notifications_tablet_disconnected),
            ToggleSpec(NotificationPreference.PRODUCT_OUT_OF_STOCK, R.string.notifications_out_of_stock),
            ToggleSpec(NotificationPreference.LOW_BATTERY, R.string.notifications_low_battery),
        ),
    ),
    ToggleGroup(
        R.string.notifications_group_business,
        listOf(
            ToggleSpec(
                NotificationPreference.WEEKLY_REPORT,
                R.string.notifications_weekly_report,
                R.string.notifications_weekly_report_subtitle,
            ),
            ToggleSpec(NotificationPreference.SELLING_SUGGESTIONS, R.string.notifications_selling_suggestions),
        ),
    ),
)

/** Stateful Notificaciones: binds the ViewModel and shows save failures in a snackbar. */
@Composable
fun NotificationPreferencesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotificationPreferencesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                NotificationPreferencesUiEffect.ShowSaveError ->
                    snackbarHostState.showSnackbar(resources.getString(R.string.notifications_save_error))
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        NotificationPreferencesContent(uiState = uiState, onEvent = viewModel::onEvent, onBack = onBack)
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
        )
    }
}

@Composable
fun NotificationPreferencesContent(
    uiState: NotificationPreferencesUiState,
    onEvent: (NotificationPreferencesUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        QhathiaGradientTopBar(title = stringResource(R.string.notifications_title), onBack = onBack)
        when (val preferences = uiState.preferences) {
            LoadState.Loading -> LoadingPane()
            LoadState.Error -> ErrorPane(
                message = stringResource(R.string.notifications_error),
                onRetry = { onEvent(NotificationPreferencesUiEvent.Retry) },
            )
            is LoadState.Loaded -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(Dimens.SpaceMd),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceLg),
            ) {
                Groups.forEach { group ->
                    ToggleGroupSection(
                        group = group,
                        preferences = preferences.value,
                        saving = uiState.saving,
                        onEvent = onEvent,
                    )
                }
            }
        }
    }
}

@Composable
private fun ToggleGroupSection(
    group: ToggleGroup,
    preferences: NotificationPreferences,
    saving: Set<NotificationPreference>,
    onEvent: (NotificationPreferencesUiEvent) -> Unit,
) {
    QhathiaSettingsSection(title = stringResource(group.title)) {
        group.toggles.forEachIndexed { index, toggle ->
            // A toggle the backend does not send yet promises nothing: no switch, no subtitle.
            val supported = preferences.isSupported(toggle.preference)
            val subtitle = when {
                !supported -> null
                toggle.preference == NotificationPreference.ORDER_EXPIRING -> pluralStringResource(
                    R.plurals.notifications_order_expiring_subtitle,
                    preferences.orderExpiringWarningMinutes,
                    preferences.orderExpiringWarningMinutes,
                )
                else -> toggle.subtitle?.let { stringResource(it) }
            }
            QhathiaSwitchRow(
                title = stringResource(toggle.title),
                subtitle = subtitle,
                checked = preferences.isEnabled(toggle.preference),
                enabled = toggle.preference !in saving,
                comingSoon = !supported,
                onCheckedChange = { onEvent(NotificationPreferencesUiEvent.Toggle(toggle.preference, it)) },
                showDivider = index < group.toggles.lastIndex,
            )
        }
    }
}

private val PreviewPreferences = NotificationPreferences(
    values = NotificationPreference.entries.associateWith { it != NotificationPreference.LOW_BATTERY },
    activeCount = 6,
    totalCount = 7,
)

@Preview(name = "Notificaciones — light", showBackground = true, heightDp = 850)
@Preview(name = "Notificaciones — dark", showBackground = true, heightDp = 850, uiMode = 0x20)
@Composable
private fun NotificationPreferencesContentPreview() {
    QhathiaTheme {
        NotificationPreferencesContent(
            uiState = NotificationPreferencesUiState(
                preferences = LoadState.Loaded(PreviewPreferences),
                saving = setOf(NotificationPreference.WEEKLY_REPORT),
            ),
            onEvent = {},
            onBack = {},
        )
    }
}

@Preview(name = "Notificaciones, loading — light", showBackground = true)
@Composable
private fun NotificationPreferencesLoadingPreview() {
    QhathiaTheme {
        NotificationPreferencesContent(uiState = NotificationPreferencesUiState(), onEvent = {}, onBack = {})
    }
}

@Preview(name = "Notificaciones, error — light", showBackground = true)
@Composable
private fun NotificationPreferencesErrorPreview() {
    QhathiaTheme {
        NotificationPreferencesContent(
            uiState = NotificationPreferencesUiState(preferences = LoadState.Error),
            onEvent = {},
            onBack = {},
        )
    }
}
