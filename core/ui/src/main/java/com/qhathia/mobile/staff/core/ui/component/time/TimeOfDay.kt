package com.qhathia.mobile.staff.core.ui.component.time

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import android.provider.Settings
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

/**
 * The phone's 12/24-hour choice: [setting] is `Settings.System.TIME_12_24` ("12", "24" or unset).
 * When the merchant never chose, the system language decides, as it does for the status bar; the
 * app's language must not, or Spanish would force 24 hours on a 12-hour phone (F-76).
 */
fun resolve24Hour(setting: String?, systemLocaleIs24: Boolean): Boolean = when (setting) {
    "24" -> true
    "12" -> false
    else -> systemLocaleIs24
}

/** [resolve24Hour] for this phone, reading its setting and its system (not app) language. */
fun Context.systemIs24HourFormat(): Boolean {
    val setting = runCatching { Settings.System.getString(contentResolver, Settings.System.TIME_12_24) }.getOrNull()
    val configuration = Configuration(Resources.getSystem().configuration).apply { setLocales(systemLocales()) }
    return resolve24Hour(setting, DateFormat.is24HourFormat(createConfigurationContext(configuration)))
}

/**
 * The languages chosen for the whole phone. From Android 13 the app's own language reaches even
 * `Resources.getSystem()`, so the system's are asked of [LocaleManager]; before that, AppCompat
 * applies the app language to activities only and the system resources still hold the phone's.
 */
private fun Context.systemLocales(): LocaleList =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getSystemService(LocaleManager::class.java)?.systemLocales ?: Resources.getSystem().configuration.locales
    } else {
        Resources.getSystem().configuration.locales
    }

/** [shortTimeFormatter] for the app's language and the phone's 12/24-hour setting. */
@Composable
fun rememberShortTimeFormatter(zone: ZoneId = ZoneId.systemDefault()): DateTimeFormatter {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val locale = configuration.locales[0]
    val is24Hour = remember(context, configuration) { context.systemIs24HourFormat() }
    return remember(locale, is24Hour, zone) { shortTimeFormatter(locale, is24Hour, zone) }
}
