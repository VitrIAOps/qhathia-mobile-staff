package com.qhathia.mobile.staff.feature.home.ui

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** The device-local wall-clock time of the load, formatted for the device locale. */
internal fun Instant.toLocalTimeLabel(): String =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        .withZone(ZoneId.systemDefault())
        .format(this)
