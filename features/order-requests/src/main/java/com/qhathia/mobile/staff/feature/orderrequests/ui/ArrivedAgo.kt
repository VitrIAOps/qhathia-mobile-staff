package com.qhathia.mobile.staff.feature.orderrequests.ui

import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.qhathia.mobile.staff.feature.orderrequests.R
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant

private const val MINUTE_MS = 60_000L

/**
 * A clock that moves once a minute, for texts like "hace 5 min": the list's per-second ticker would
 * recompose them 60 times for one visible change.
 */
@Composable
internal fun rememberMinuteClock(initial: Instant = Instant.now()): State<Instant> {
    val clock = remember { mutableStateOf(initial) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(MINUTE_MS - (System.currentTimeMillis() % MINUTE_MS))
            clock.value = Instant.now()
        }
    }
    return clock
}

/** "hace 5 min" / "hace 2 h", in the app's language; "recién" under a minute. */
@Composable
internal fun arrivedAgoLabel(createdAt: Instant, now: Instant): String =
    if (Duration.between(createdAt, now) < Duration.ofMinutes(1)) {
        stringResource(R.string.order_arrived_just_now)
    } else {
        DateUtils.getRelativeTimeSpanString(
            createdAt.toEpochMilli(),
            now.toEpochMilli(),
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE,
        ).toString()
            // Android capitalizes it ("Hace 2 h"); it goes mid-line, after the code.
            .replaceFirstChar { it.lowercase() }
    }
