package com.qhathia.mobile.staff.feature.home.ui

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.staff.core.ui.component.sales.QhathiaSalesSummaryCard
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaColors
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme
import com.qhathia.mobile.staff.feature.home.R
import com.qhathia.mobile.staff.feature.home.model.HomeUiEvent
import com.qhathia.mobile.staff.feature.home.model.HomeUiState
import com.qhathia.mobile.staff.core.ui.R as CoreUiR

private val SummaryPlaceholderHeight = 320.dp

/** Matches [SalesSummaryCard], so the placeholder and the card share a silhouette. */
private val SummaryCardRadius = 20.dp

/**
 * Stateless Inicio tab: the gradient header, the "Resumen" card and both suggestion rows.
 *
 * It scrolls vertically because the design already overflows a typical viewport at its own device
 * size, and has to keep working at large font scales. The header scrolls with the content — it is
 * a greeting, not a toolbar, and pinning it would cost a third of the screen.
 */
@Composable
internal fun HomeContent(
    uiState: HomeUiState,
    onEvent: (HomeUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = Dimens.SpaceXl),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceLg),
    ) {
        item(key = "header") {
            HomeHeader(
                merchantFirstName = uiState.merchantFirstName,
                lastUpdatedAt = uiState.lastUpdatedAt,
                pendingOrderCount = uiState.pendingOrderCount,
                onOpenNotifications = { onEvent(HomeUiEvent.OpenNotifications) },
            )
        }

        item(key = "summary") {
            val cardModel = remember(uiState.summary) { uiState.summary?.toCardModel() }
            Column(
                modifier = Modifier.padding(horizontal = Dimens.SpaceMd),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
            ) {
                SummaryHeading(onSeeMore = { onEvent(HomeUiEvent.OpenSalesStatistics) })
                when {
                    uiState.isInitialLoading -> SummaryLoading()
                    uiState.summary == null && uiState.isError -> SummaryError(
                        onRetry = { onEvent(HomeUiEvent.Refresh) },
                    )
                    else -> QhathiaSalesSummaryCard(
                        model = cardModel,
                        header = {
                            PeriodTabs(
                                selectedPeriod = uiState.selectedPeriod,
                                onSelectPeriod = { onEvent(HomeUiEvent.SelectPeriod(it)) },
                            )
                        },
                    )
                }
                // A failure over data that did load degrades to "stale plus an error", never to a
                // blank screen.
                if (uiState.isError && uiState.summary != null) {
                    SummaryError(onRetry = { onEvent(HomeUiEvent.Refresh) })
                }
            }
        }

        if (uiState.hasSuggestions) {
            item(key = "suggestions-heading") {
                SuggestionsHeading(modifier = Modifier.padding(horizontal = Dimens.SpaceMd))
            }
        }

        item(key = "missing-products") {
            MissingProductsSection(
                suggestions = uiState.missingProducts,
                onAdd = { suggestion -> onEvent(HomeUiEvent.AddMissingProduct(suggestion.name)) },
            )
        }

        item(key = "other-requests") {
            OtherRequestsSection(requests = uiState.otherRequests)
        }

        item(key = "restock-products") {
            RestockProductsSection(
                suggestions = uiState.restockProducts,
                onRestock = { suggestion -> onEvent(HomeUiEvent.RestockProduct(suggestion.id)) },
            )
        }
    }
}

/** "Resumen" with its `Ver más ›` action, which opens the sales statistics detail. */
@Composable
private fun SummaryHeading(
    onSeeMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.home_summary_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Row(
            modifier = Modifier
                .clickable(role = Role.Button, onClick = onSeeMore)
                .padding(horizontal = Dimens.SpaceMd, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.home_summary_see_more),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.tertiary,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(Dimens.IconSm),
            )
        }
    }
}

@Composable
private fun SuggestionsHeading(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceMd),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(CoreUiR.drawable.ic_sparks),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(Dimens.SpaceLg),
        )
        Text(
            text = stringResource(R.string.home_suggestions_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

/** Placeholder in roughly the card's own footprint, so the page does not jump when it lands. */
@Composable
private fun SummaryLoading(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.home_loading_cd)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(SummaryPlaceholderHeight)
            .background(QhathiaColors.summaryCard, RoundedCornerShape(SummaryCardRadius))
            .clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = QhathiaColors.onSummaryCard)
    }
}

@Composable
private fun SummaryError(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.errorContainer,
                RoundedCornerShape(Dimens.SpaceSm),
            )
            .padding(horizontal = Dimens.SpaceMd, vertical = Dimens.SpaceSm),
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.home_error_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(R.string.home_retry),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier
                .clickable(onClick = onRetry)
                .padding(Dimens.SpaceXs),
        )
    }
}

@Preview(name = "Home — light", showBackground = true, heightDp = 1200)
@Preview(name = "Home — dark", showBackground = true, uiMode = 0x20, heightDp = 1200)
@Composable
private fun HomeContentPreview() {
    QhathiaTheme {
        HomeContent(uiState = HomePreviewData.loadedState, onEvent = {})
    }
}

@Preview(name = "Home loading — light", showBackground = true, heightDp = 700)
@Preview(name = "Home loading — dark", showBackground = true, uiMode = 0x20, heightDp = 700)
@Composable
private fun HomeContentLoadingPreview() {
    QhathiaTheme {
        HomeContent(uiState = HomePreviewData.loadingState, onEvent = {})
    }
}

@Preview(name = "Home error — light", showBackground = true, heightDp = 700)
@Preview(name = "Home error — dark", showBackground = true, uiMode = 0x20, heightDp = 700)
@Composable
private fun HomeContentErrorPreview() {
    QhathiaTheme {
        HomeContent(uiState = HomePreviewData.errorState, onEvent = {})
    }
}
