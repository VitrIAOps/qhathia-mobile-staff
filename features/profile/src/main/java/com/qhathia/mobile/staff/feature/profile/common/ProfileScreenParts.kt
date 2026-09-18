package com.qhathia.mobile.staff.feature.profile.common

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.staff.core.domain.model.Subscription
import com.qhathia.mobile.staff.core.domain.model.SubscriptionStatus
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaColors
import com.qhathia.mobile.staff.feature.profile.R

/** Centered spinner for a screen or section that has nothing to show yet. */
@Composable
internal fun LoadingPane(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().padding(Dimens.SpaceXl), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
    }
}

/** A message with a retry action, for a screen or section that failed to load. */
@Composable
internal fun ErrorPane(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(Dimens.SpaceLg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        TextButton(onClick = onRetry) {
            Text(text = stringResource(R.string.profile_retry), color = MaterialTheme.colorScheme.secondary)
        }
    }
}

/** "PLAN PRO" badge followed by the status dot and label, as drawn on the gold card. */
@Composable
internal fun PlanBadgeRow(subscription: Subscription, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.subscription_plan_badge, subscription.planType.uppercase()),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = QhathiaColors.onPlanBadge,
            modifier = Modifier
                .background(QhathiaColors.planBadge, RoundedCornerShape(Dimens.SpaceXs))
                .padding(horizontal = Dimens.SpaceSm, vertical = 2.dp),
        )
        subscription.status.labelRes()?.let { labelRes ->
            Spacer(Modifier.width(Dimens.SpaceSm))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        if (subscription.status == SubscriptionStatus.ACTIVE) {
                            QhathiaColors.switchChecked
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        CircleShape,
                    ),
            )
            Spacer(Modifier.width(Dimens.SpaceXs))
            Text(
                text = stringResource(labelRes),
                style = MaterialTheme.typography.bodySmall,
                color = QhathiaColors.onSubscriptionCard,
            )
        }
    }
}

private fun SubscriptionStatus.labelRes(): Int? = when (this) {
    SubscriptionStatus.ACTIVE -> R.string.subscription_status_active
    SubscriptionStatus.CANCELLED -> R.string.subscription_status_cancelled
    SubscriptionStatus.EXPIRED -> R.string.subscription_status_expired
    SubscriptionStatus.UNKNOWN -> null
}
