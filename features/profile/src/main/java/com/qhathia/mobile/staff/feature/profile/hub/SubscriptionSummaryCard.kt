package com.qhathia.mobile.staff.feature.profile.hub

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.staff.core.domain.model.Subscription
import com.qhathia.mobile.staff.core.ui.component.sales.toSolesLabel
import com.qhathia.mobile.staff.core.ui.component.settings.QhathiaPlanProgressBar
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaColors
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme
import com.qhathia.mobile.staff.feature.profile.R
import com.qhathia.mobile.staff.feature.profile.common.LoadState
import com.qhathia.mobile.staff.feature.profile.common.PlanBadgeRow
import com.qhathia.mobile.staff.feature.profile.common.currentLocale
import com.qhathia.mobile.staff.feature.profile.common.longDateLabel
import com.qhathia.mobile.staff.feature.profile.common.periodProgress
import com.qhathia.mobile.staff.feature.profile.common.shortDateLabel
import java.time.Instant

/**
 * The gold "Mi suscripción" card of Mi perfil. The whole card opens Mi suscripción in every state,
 * including when the store has no subscription.
 */
@Composable
internal fun SubscriptionSummaryCard(
    state: LoadState<Subscription?>,
    now: Instant,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(Dimens.CardRadius),
        color = QhathiaColors.subscriptionCard,
        contentColor = QhathiaColors.onSubscriptionCard,
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onOpen),
    ) {
        Column(
            modifier = Modifier.padding(Dimens.SpaceMd),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
        ) {
            val subscription = (state as? LoadState.Loaded)?.value
            if (subscription != null) PlanBadgeRow(subscription)
            TitleRow()
            when (state) {
                LoadState.Loading -> CircularProgressIndicator(
                    color = QhathiaColors.onSubscriptionCard,
                    strokeWidth = 2.dp,
                )
                LoadState.Error -> BodyText(stringResource(R.string.subscription_error))
                is LoadState.Loaded -> if (subscription == null) {
                    BodyText(stringResource(R.string.subscription_none))
                } else {
                    SubscriptionDetails(subscription = subscription, now = now)
                }
            }
        }
    }
}

@Composable
private fun TitleRow() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = Icons.Outlined.Diamond, contentDescription = null)
        Spacer(Modifier.width(Dimens.SpaceSm))
        Text(
            text = stringResource(R.string.subscription_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        Icon(imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
    }
}

@Composable
private fun SubscriptionDetails(subscription: Subscription, now: Instant) {
    val locale = currentLocale()
    BodyText(stringResource(R.string.subscription_price_monthly, subscription.priceMonthlyPen.toSolesLabel()))
    subscription.endsAt?.let { endsAt ->
        Text(
            text = stringResource(R.string.subscription_renews_on, endsAt.longDateLabel(locale)),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
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

@Composable
private fun BodyText(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyMedium)
}

@Preview(name = "Subscription summary — light", showBackground = true)
@Preview(name = "Subscription summary — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun SubscriptionSummaryCardPreview() {
    QhathiaTheme {
        SubscriptionSummaryCard(
            state = ProfileHubPreviewData.owner.subscription,
            now = ProfileHubPreviewData.now,
            onOpen = {},
            modifier = Modifier.padding(Dimens.SpaceMd),
        )
    }
}
