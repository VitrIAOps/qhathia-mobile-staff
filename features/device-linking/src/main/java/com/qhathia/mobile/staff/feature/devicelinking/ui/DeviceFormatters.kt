package com.qhathia.mobile.staff.feature.devicelinking.ui

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** The phone's local wall-clock time of this instant, short and localized (e.g. "15:42"). */
internal fun Instant.toShortLocalTime(): String =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        .withZone(ZoneId.systemDefault())
        .format(this)
