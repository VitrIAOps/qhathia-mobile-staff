package com.qhathia.mobile.staff.feature.profile.subscription

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.qhathia.mobile.staff.core.domain.model.Subscription
import com.qhathia.mobile.staff.core.ui.component.sales.toSolesLabel
import com.qhathia.mobile.staff.core.ui.component.settings.QhathiaComingSoonTag
import com.qhathia.mobile.staff.core.ui.component.settings.QhathiaGradientTopBar
import com.qhathia.mobile.staff.core.ui.component.settings.QhathiaNavigationRow
import com.qhathia.mobile.staff.core.ui.component.settings.QhathiaPlanProgressBar
import com.qhathia.mobile.staff.core.ui.component.settings.QhathiaSettingsSection
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaColors
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme
import com.qhathia.mobile.staff.feature.profile.R
import com.qhathia.mobile.staff.feature.profile.common.ErrorPane
import com.qhathia.mobile.staff.feature.profile.common.LoadState
import com.qhathia.mobile.staff.feature.profile.common.LoadingPane
import com.qhathia.mobile.staff.feature.profile.common.PlanBadgeRow
import com.qhathia.mobile.staff.feature.profile.common.currentLocale
import com.qhathia.mobile.staff.feature.profile.common.label
import com.qhathia.mobile.staff.feature.profile.common.lines
import com.qhathia.mobile.staff.feature.profile.common.periodProgress
import com.qhathia.mobile.staff.feature.profile.common.shortDateLabel
import com.qhathia.mobile.staff.feature.profile.hub.ProfileHubPreviewData
import java.time.Instant

/** Stateful Mi suscripción. */
@Composable
fun SubscriptionScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SubscriptionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SubscriptionContent(uiState = uiState, onEvent = viewModel::onEvent, onBack = onBack, modifier = modifier)
}

/**
 * Stateless Mi suscripción: the plan card, then the payment sections and cancellation, which the
 * backend does not support yet and are therefore shown as *Próximamente*.
 */
@Composable
fun SubscriptionContent(
    uiState: SubscriptionUiState,
    onEvent: (SubscriptionUiEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        QhathiaGradientTopBar(title = stringResource(R.string.subscription_title), onBack = onBack)
        when (val state = uiState.subscription) {
            LoadState.Loading -> LoadingPane()
            LoadState.Error -> ErrorPane(
                message = stringResource(R.string.subscription_error),
                onRetry = { onEvent(SubscriptionUiEvent.Retry) },
            )
            is LoadState.Loaded -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(Dimens.SpaceMd),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceLg),
            ) {
                val subscription = state.value
                if (subscription == null) {
                    Text(
                        text = stringResource(R.string.subscription_none),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = Dimens.SpaceLg),
                    )
                } else {
                    PlanCard(subscription = subscription, now = uiState.now)
                }
                PaymentSections()
            }
        }
    }
}

@Composable
private fun PlanCard(subscription: Subscription, now: Instant) {
    val locale = currentLocale()
    Surface(
        shape = RoundedCornerShape(Dimens.CardRadius),
        color = QhathiaColors.subscriptionCard,
        contentColor = QhathiaColors.onSubscriptionCard,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box {
            Icon(
                imageVector = Icons.Outlined.Diamond,
                contentDescription = null,
                tint = QhathiaColors.onSubscriptionCard.copy(alpha = 0.2f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(Dimens.SpaceMd)
                    .size(120.dp),
            )
            Column(
                modifier = Modifier.padding(Dimens.SpaceMd),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
            ) {
                PlanBadgeRow(subscription)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = subscription.priceMonthlyPen.toSolesLabel(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.width(Dimens.SpaceSm))
                    Text(
                        text = stringResource(R.string.subscription_per_month),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = Dimens.SpaceXs),
                    )
                }
                subscription.features.lines().forEach { line ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(Dimens.SpaceSm))
                        Text(text = line.label(), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                val startedAt = subscription.startedAt
                val endsAt = subscription.endsAt
                val progress = periodProgress(startedAt, endsAt, now)
                if (progress != null && startedAt != null && endsAt != null) {
                    QhathiaPlanProgressBar(
                        fraction = progress,
                        startLabel = stringResource(R.string.subscription_started_on, startedAt.shortDateLabel(locale)),
                        endLabel = stringResource(R.string.subscription_renewal_on, endsAt.shortDateLabel(locale)),
                        modifier = Modifier.padding(top = Dimens.SpaceSm),
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentSections() {
    QhathiaSettingsSection(title = stringResource(R.string.subscription_payment_method)) {
        QhathiaNavigationRow(
            title = stringResource(R.string.subscription_payment_method_row),
            onClick = null,
            comingSoon = true,
            showDivider = false,
        )
    }
    QhathiaSettingsSection(title = stringResource(R.string.subscription_payment_history)) {
        QhathiaNavigationRow(
            title = stringResource(R.string.subscription_payment_history_row),
            onClick = null,
            comingSoon = true,
            showDivider = false,
        )
    }
    val cancelCd = stringResource(R.string.subscription_cancel_cd)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Dimens.SpaceSm)
            .clearAndSetSemantics {
                contentDescription = cancelCd
                disabled()
            },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.subscription_cancel),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
        )
        Spacer(Modifier.width(Dimens.SpaceSm))
        QhathiaComingSoonTag()
    }
}

@Preview(name = "Mi suscripción — light", showBackground = true, heightDp = 900)
@Preview(name = "Mi suscripción — dark", showBackground = true, heightDp = 900, uiMode = 0x20)
@Composable
private fun SubscriptionContentPreview() {
    QhathiaTheme {
        SubscriptionContent(
            uiState = SubscriptionUiState(
                subscription = LoadState.Loaded(ProfileHubPreviewData.subscription),
                now = ProfileHubPreviewData.now,
            ),
            onEvent = {},
            onBack = {},
        )
    }
}

@Preview(name = "Mi suscripción, none — light", showBackground = true)
@Composable
private fun SubscriptionNonePreview() {
    QhathiaTheme {
        SubscriptionContent(uiState = SubscriptionUiState(LoadState.Loaded(null)), onEvent = {}, onBack = {})
    }
}

@Preview(name = "Mi suscripción, error — light", showBackground = true)
@Composable
private fun SubscriptionErrorPreview() {
    QhathiaTheme {
        SubscriptionContent(uiState = SubscriptionUiState(LoadState.Error), onEvent = {}, onBack = {})
    }
}
