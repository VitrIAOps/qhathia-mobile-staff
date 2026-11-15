package com.qhathia.mobile.staff.feature.orderrequests.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.core.ui.theme.QhathiaColors
import com.qhathia.mobile.staff.feature.orderrequests.R
import java.time.Duration
import java.time.Instant

/** At or below this much time left, the countdown turns red. */
private val CRITICAL_REMAINING: Duration = Duration.ofMinutes(3)

private const val MINUTES_PER_HOUR = 60L

/** How urgent a pending order's countdown looks. */
internal enum class ExpiryLevel { NORMAL, WARNING, CRITICAL }

/**
 * Neutral with more than [warningMinutes] left, amber from there (the backend's
 * `orderExpiringWarningMinutes`, `domain/rules/order-confirmation.md#R2`), red at 3 minutes or less.
 */
internal fun expiryLevel(remaining: Duration, warningMinutes: Int): ExpiryLevel = when {
    remaining <= CRITICAL_REMAINING -> ExpiryLevel.CRITICAL
    remaining <= Duration.ofMinutes(warningMinutes.toLong()) -> ExpiryLevel.WARNING
    else -> ExpiryLevel.NORMAL
}

/**
 * Time left until [expiresAt], rendered against a [now] supplied by the list-level ticker, as a chip
 * whose colour follows [expiryLevel]. The text is always there, so the meaning never rests on colour.
 *
 * [now] is a lambda on purpose: reading the ticking value here — rather than at the card level —
 * keeps the per-second recomposition inside this label instead of the whole row.
 *
 * The server owns expiry; this is a display of `expiresAt`, never a client-side TTL, and a zeroed
 * countdown does not remove the order from the list.
 */
@Composable
internal fun ExpiryCountdown(
    expiresAt: Instant,
    now: () -> Instant,
    warningMinutes: Int,
    modifier: Modifier = Modifier,
) {
    val remaining = Duration.between(now(), expiresAt)
    val (container, content) = when (expiryLevel(remaining, warningMinutes)) {
        ExpiryLevel.CRITICAL -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        ExpiryLevel.WARNING -> QhathiaColors.warningContainer to QhathiaColors.onWarningContainer
        ExpiryLevel.NORMAL -> QhathiaColors.segmentTrack to MaterialTheme.colorScheme.onSurfaceVariant
    }
    StatusChip(
        text = expiryLabel(remaining),
        containerColor = container,
        contentColor = content,
        icon = Icons.Outlined.Schedule,
        modifier = modifier,
    )
}

/** A small pill with an optional leading icon: the countdown, or what happened to a closed order. */
@Composable
internal fun StatusChip(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    Row(
        modifier = modifier
            .background(containerColor, RoundedCornerShape(Dimens.SpaceMd))
            .padding(horizontal = Dimens.SpaceSm, vertical = Dimens.SpaceXs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(14.dp),
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = contentColor,
        )
    }
}

/** "Vence en 20 minutos" / "Vence en 1 hora" / "Vence en menos de 1 minuto" / "Vencida". */
@Composable
private fun expiryLabel(remaining: Duration): String {
    if (remaining.isNegative || remaining.isZero) return stringResource(R.string.order_expired)

    val minutes = remaining.toMinutes()
    val amount = when {
        minutes >= MINUTES_PER_HOUR -> {
            val hours = (minutes / MINUTES_PER_HOUR).toInt()
            pluralStringResource(R.plurals.order_duration_hours, hours, hours)
        }
        minutes >= 1 ->
            pluralStringResource(R.plurals.order_duration_minutes, minutes.toInt(), minutes.toInt())
        else -> stringResource(R.string.order_expires_in_less_than_a_minute)
    }
    return stringResource(R.string.order_expires_in, amount)
}

/**
 * Whether the order's deadline has passed. Its confirm/reject actions are disabled from here on —
 * they are guaranteed to come back as `422` — but the row stays until the next refresh.
 */
internal fun isExpired(expiresAt: Instant?, now: Instant): Boolean =
    expiresAt != null && !now.isBefore(expiresAt)
