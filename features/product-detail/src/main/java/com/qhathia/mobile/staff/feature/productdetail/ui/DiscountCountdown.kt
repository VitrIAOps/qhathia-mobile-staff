package com.qhathia.mobile.staff.feature.productdetail.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.qhathia.mobile.staff.core.ui.theme.QhathiaTheme
import com.qhathia.mobile.staff.feature.productdetail.R
import com.qhathia.mobile.staff.feature.productdetail.model.DiscountTimeLeft
import com.qhathia.mobile.staff.feature.productdetail.model.remainingDiscountTime
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant

private const val TICK_MILLIS = 60_000L

/**
 * "Termina en 6 días y 4 horas" under the discount summary. Counts down against the phone's clock,
 * refreshing once a minute while on screen, and draws nothing once [endsAt] has passed.
 */
@Composable
internal fun DiscountCountdown(
    endsAt: Instant,
    modifier: Modifier = Modifier,
) {
    val now by produceState(initialValue = Instant.now(), endsAt) {
        while (true) {
            delay(TICK_MILLIS)
            value = Instant.now()
        }
    }
    val left = remainingDiscountTime(endsAt, now) ?: return
    Text(
        text = left.text(),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Composable
private fun DiscountTimeLeft.text(): String = when (this) {
    is DiscountTimeLeft.DaysAndHours -> {
        val daysText = pluralStringResource(R.plurals.detail_discount_days, days.toInt(), days)
        if (hours == 0L) {
            stringResource(R.string.detail_discount_ends_in, daysText)
        } else {
            val hoursText = pluralStringResource(R.plurals.detail_discount_hours, hours.toInt(), hours)
            stringResource(R.string.detail_discount_ends_in_days_hours, daysText, hoursText)
        }
    }
    is DiscountTimeLeft.Hours -> stringResource(
        R.string.detail_discount_ends_in,
        pluralStringResource(R.plurals.detail_discount_hours, hours.toInt(), hours),
    )
    DiscountTimeLeft.UnderAnHour -> stringResource(R.string.detail_discount_ends_soon)
}

@Preview(name = "Discount countdown — light", showBackground = true)
@Preview(name = "Discount countdown — dark", showBackground = true, uiMode = 0x20)
@Composable
private fun DiscountCountdownPreview() {
    QhathiaTheme {
        DiscountCountdown(endsAt = Instant.now().plus(Duration.ofDays(6).plusHours(4).plusMinutes(20)))
    }
}
