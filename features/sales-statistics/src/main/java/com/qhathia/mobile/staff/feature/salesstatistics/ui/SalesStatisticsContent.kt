package com.qhathia.mobile.staff.feature.salesstatistics.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.staff.core.ui.component.QhathiaSegmentedControl
import com.qhathia.mobile.staff.core.ui.component.time.rememberShortTimeFormatter
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme
import com.qhathia.mobile.staff.feature.salesstatistics.R
import com.qhathia.mobile.staff.feature.salesstatistics.model.SalesStatisticsUiEvent
import com.qhathia.mobile.staff.feature.salesstatistics.model.SalesStatisticsUiState
import com.qhathia.mobile.staff.feature.salesstatistics.model.StatisticsTab
import com.qhathia.mobile.staff.feature.salesstatistics.model.TabContent
import kotlinx.collections.immutable.toImmutableList

private val TABS = StatisticsTab.entries.toImmutableList()
private val LoadingPlaceholderHeight = 240.dp

/**
 * Stateless "Resumen de ventas" screen: the top bar stays put while the date label, the tab
 * selector, the filters and the active tab's content scroll beneath it.
 */
@Composable
internal fun SalesStatisticsContent(
    uiState: SalesStatisticsUiState,
    onEvent: (SalesStatisticsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val (start, end) = uiState.boundsOf(uiState.selectedTab)
    val rangeLabel = periodRangeLabel(
        period = uiState.period,
        start = start,
        end = end,
        locale = LocalConfiguration.current.locales[0],
        timeFormatter = rememberShortTimeFormatter(zone = LIMA),
    )
    // When Ranking's figures were computed; only Ranking reports it.
    val asOf = uiState.ranking.data?.asOf?.takeIf { uiState.selectedTab == StatisticsTab.RANKING }
    val asOfFormatter = rememberShortTimeFormatter()
    val onRetry = { onEvent(SalesStatisticsUiEvent.Retry) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        StatisticsTopBar(
            onBack = { onEvent(SalesStatisticsUiEvent.Back) },
            onShare = { onEvent(SalesStatisticsUiEvent.Share) },
            shareEnabled = uiState.isShareEnabled,
        )
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(
                start = Dimens.SpaceMd,
                end = Dimens.SpaceMd,
                top = Dimens.SpaceLg,
                bottom = Dimens.SpaceXl,
            ),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceLg),
        ) {
            // Always present, empty while loading, so a load never inserts items above the list's
            // anchor and pushes the range out of view (F-84 g).
            item(key = "range") {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXs)) {
                    if (rangeLabel != null) {
                        Text(
                            text = rangeLabel,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.semantics { heading() },
                        )
                    }
                    if (asOf != null) {
                        Text(
                            text = stringResource(R.string.statistics_updated_at, asOfFormatter.format(asOf)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item(key = "tabs") {
                QhathiaSegmentedControl(
                    options = TABS,
                    selected = uiState.selectedTab,
                    onSelect = { onEvent(SalesStatisticsUiEvent.SelectTab(it)) },
                    label = { stringResource(it.labelRes) },
                )
            }

            item(key = "filters") {
                FiltersPanel(
                    isExpanded = uiState.isFiltersExpanded,
                    period = uiState.period,
                    isCategoryVisible = uiState.isCategoryFilterVisible,
                    categories = uiState.categories,
                    selectedCategoryId = uiState.selectedCategoryId,
                    onToggle = { onEvent(SalesStatisticsUiEvent.ToggleFilters) },
                    onSelectPeriod = { onEvent(SalesStatisticsUiEvent.SelectPeriod(it)) },
                    onSelectCategory = { onEvent(SalesStatisticsUiEvent.SelectCategory(it)) },
                )
            }

            item(key = "content") {
                when (uiState.selectedTab) {
                    StatisticsTab.SUMMARY -> TabBody(uiState.summary, onRetry) { SummaryTab(it) }
                    StatisticsTab.RANKING -> TabBody(uiState.ranking, onRetry) { RankingTab(it) }
                    StatisticsTab.ACTIVITY -> TabBody(uiState.activity, onRetry) {
                        ActivityTab(activity = it, period = uiState.period)
                    }
                }
            }
        }
    }
}

/**
 * A tab's loading, error and content treatments: data on screen stays next to an error from a
 * failed retry; with no data, a failure shows only the error and anything else shows the spinner.
 */
@Composable
private fun <T : Any> TabBody(
    content: TabContent<T>,
    onRetry: () -> Unit,
    body: @Composable (T) -> Unit,
) {
    val data = content.data
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd)) {
        when {
            data != null -> {
                if (content.isError) ErrorBanner(onRetry = onRetry)
                body(data)
            }
            content.isError -> ErrorBanner(onRetry = onRetry)
            else -> LoadingPlaceholder()
        }
    }
}

@Composable
private fun LoadingPlaceholder(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.statistics_loading_cd)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(LoadingPlaceholderHeight)
            .clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorBanner(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(Dimens.SpaceSm))
            .padding(horizontal = Dimens.SpaceMd, vertical = Dimens.SpaceSm),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.statistics_error_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(R.string.statistics_retry),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier
                .clickable(role = Role.Button, onClick = onRetry)
                .padding(Dimens.SpaceXs),
        )
    }
}

@Preview(name = "Statistics, Resumen — light", showBackground = true, heightDp = 900)
@Preview(name = "Statistics, Resumen — dark", showBackground = true, uiMode = 0x20, heightDp = 900)
@Composable
private fun SalesStatisticsSummaryPreview() {
    QhathiaTheme {
        SalesStatisticsContent(uiState = SalesStatisticsPreviewData.summaryState, onEvent = {})
    }
}

@Preview(name = "Statistics, filters expanded — light", showBackground = true, heightDp = 1000)
@Preview(name = "Statistics, filters expanded — dark", showBackground = true, uiMode = 0x20, heightDp = 1000)
@Composable
private fun SalesStatisticsFiltersPreview() {
    QhathiaTheme {
        SalesStatisticsContent(uiState = SalesStatisticsPreviewData.filtersExpandedState, onEvent = {})
    }
}

@Preview(name = "Statistics, Ranking — light", showBackground = true, heightDp = 1100)
@Preview(name = "Statistics, Ranking — dark", showBackground = true, uiMode = 0x20, heightDp = 1100)
@Composable
private fun SalesStatisticsRankingPreview() {
    QhathiaTheme {
        SalesStatisticsContent(uiState = SalesStatisticsPreviewData.rankingState, onEvent = {})
    }
}

@Preview(name = "Statistics, Actividad — light", showBackground = true, heightDp = 800)
@Preview(name = "Statistics, Actividad — dark", showBackground = true, uiMode = 0x20, heightDp = 800)
@Composable
private fun SalesStatisticsActivityPreview() {
    QhathiaTheme {
        SalesStatisticsContent(uiState = SalesStatisticsPreviewData.activityState, onEvent = {})
    }
}

@Preview(name = "Statistics, loading — light", showBackground = true, heightDp = 700)
@Preview(name = "Statistics, loading — dark", showBackground = true, uiMode = 0x20, heightDp = 700)
@Composable
private fun SalesStatisticsLoadingPreview() {
    QhathiaTheme {
        SalesStatisticsContent(uiState = SalesStatisticsPreviewData.loadingState, onEvent = {})
    }
}

@Preview(name = "Statistics, error — light", showBackground = true, heightDp = 700)
@Preview(name = "Statistics, error — dark", showBackground = true, uiMode = 0x20, heightDp = 700)
@Composable
private fun SalesStatisticsErrorPreview() {
    QhathiaTheme {
        SalesStatisticsContent(uiState = SalesStatisticsPreviewData.errorState, onEvent = {})
    }
}
