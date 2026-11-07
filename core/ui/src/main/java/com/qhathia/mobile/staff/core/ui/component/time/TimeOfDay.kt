package com.qhathia.mobile.staff.core.ui.component.time

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * A short time of day ("5:04 p. m." or "17:04") in [locale], following the phone's 12/24-hour
 * setting rather than the locale's habit: Spanish defaults to 24 hours, so the locale alone shows
 * "17:04" on a phone set to 12. Instants are shown in [zone].
 */
fun shortTimeFormatter(
    locale: Locale,
    is24Hour: Boolean,
    zone: ZoneId = ZoneId.systemDefault(),
): DateTimeFormatter =
    DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, if (is24Hour) "Hm" else "hm"), locale)
        .withZone(zone)

/** [shortTimeFormatter] for the app's language and the phone's 12/24-hour setting. */
@Composable
fun rememberShortTimeFormatter(zone: ZoneId = ZoneId.systemDefault()): DateTimeFormatter {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val is24Hour = DateFormat.is24HourFormat(context)
    return remember(locale, is24Hour, zone) { shortTimeFormatter(locale, is24Hour, zone) }
}
