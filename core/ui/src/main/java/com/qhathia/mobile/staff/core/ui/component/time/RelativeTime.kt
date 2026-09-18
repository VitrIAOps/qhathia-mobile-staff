package com.qhathia.mobile.staff.core.ui.component.time

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.qhathia.mobile.staff.core.ui.R
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** How long ago something happened, bucketed the way the screens word it. */
sealed interface RelativeTime {
    data object JustNow : RelativeTime
    data class Minutes(val count: Int) : RelativeTime
    data class Hours(val count: Int) : RelativeTime
    data class Days(val count: Int) : RelativeTime
    data class Months(val count: Int) : RelativeTime
    data class Years(val count: Int) : RelativeTime
}

private val LIMA: ZoneId = ZoneId.of("America/Lima")

/**
 * Buckets the time from [instant] to [now]. A future [instant] (clock skew between phone and
 * backend) reads as just now rather than as a negative age. Months are calendar months in [zone].
 */
fun relativeTimeBetween(instant: Instant, now: Instant, zone: ZoneId = LIMA): RelativeTime {
    val elapsed = Duration.between(instant, now)
    if (elapsed.isNegative || elapsed.toMinutes() < 1) return RelativeTime.JustNow
    return when {
        elapsed.toMinutes() < 60 -> RelativeTime.Minutes(elapsed.toMinutes().toInt())
        elapsed.toHours() < 24 -> RelativeTime.Hours(elapsed.toHours().toInt())
        elapsed.toDays() < 30 -> RelativeTime.Days(elapsed.toDays().toInt())
        else -> {
            val months = ChronoUnit.MONTHS.between(instant.atZone(zone), now.atZone(zone)).toInt().coerceAtLeast(1)
            if (months < 12) RelativeTime.Months(months) else RelativeTime.Years(months / 12)
        }
    }
}

/** "hace un momento", "hace 4 min", "hace 2 horas", "hace 4 meses"… */
@Composable
@ReadOnlyComposable
fun RelativeTime.label(): String = when (this) {
    RelativeTime.JustNow -> stringResource(R.string.time_just_now)
    is RelativeTime.Minutes -> pluralStringResource(R.plurals.time_minutes_ago, count, count)
    is RelativeTime.Hours -> pluralStringResource(R.plurals.time_hours_ago, count, count)
    is RelativeTime.Days -> pluralStringResource(R.plurals.time_days_ago, count, count)
    is RelativeTime.Months -> pluralStringResource(R.plurals.time_months_ago, count, count)
    is RelativeTime.Years -> pluralStringResource(R.plurals.time_years_ago, count, count)
}
