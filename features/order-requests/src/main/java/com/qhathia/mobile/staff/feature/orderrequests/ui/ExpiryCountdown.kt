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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.qhathia.mobile.staff.core.ui.theme.Dimens
import com.qhathia.mobile.staff.feature.orderrequests.R
import java.time.Duration
import java.time.Instant

/** Below this much time left, the countdown switches to its emphasized red pill treatment. */
private const val CRITICAL_MINUTES = 3L

private const val MINUTES_PER_HOUR = 60L

/**
 * Time left until [expiresAt], rendered against a [now] supplied by the list-level ticker.
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
    modifier: Modifier = Modifier,
) {
    val remaining = Duration.between(now(), expiresAt)
    val critical = remaining.toMinutes() < CRITICAL_MINUTES
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = if (critical) {
            modifier
                .background(colors.errorContainer, RoundedCornerShape(Dimens.SpaceMd))
                .padding(horizontal = Dimens.SpaceSm, vertical = Dimens.SpaceXs)
        } else {
            modifier
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceXs),
    ) {
        val contentColor = if (critical) colors.onErrorContainer else colors.onSurfaceVariant
        Icon(
            imageVector = Icons.Outlined.Schedule,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = expiryLabel(remaining),
            style = MaterialTheme.typography.bodySmall,
            color = contentColor,
        )
    }
}

/** "Expira en 20 minutos" / "Expira en 1 hora" / "Expira en menos de 1 minuto" / "Expirado". */
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
