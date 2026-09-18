package com.qhathia.mobile.staff.feature.profile.common

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.qhathia.mobile.staff.core.domain.model.PlanFeatures
import com.qhathia.mobile.staff.core.domain.model.UserRole
import com.qhathia.mobile.staff.feature.profile.R
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** The backend cuts days and renewals in Lima time. */
internal val LIMA_ZONE: ZoneId = ZoneId.of("America/Lima")

/**
 * The language the app is currently rendered in, for dates and numbers. Read from the configuration
 * so a language switch recomposes the labels; a configuration always carries at least one locale.
 */
@Composable
@ReadOnlyComposable
fun currentLocale(): Locale = LocalConfiguration.current.locales[0]

/** "15 de septiembre de 2026" / "September 15, 2026", in Lima time. */
fun Instant.longDateLabel(locale: Locale): String =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale).format(atZone(LIMA_ZONE))

/** "15 ago 2025" / "15 Aug 2025", in Lima time. */
fun Instant.shortDateLabel(locale: Locale): String =
    DateTimeFormatter.ofPattern("d MMM yyyy", locale).format(atZone(LIMA_ZONE)).replace(".", "")

/** "+51 999 888 777" for a 9-digit Peruvian mobile; anything else keeps its digits after the prefix. */
fun formatPeruPhone(digits: String): String =
    if (digits.length == 9) {
        "+51 ${digits.substring(0, 3)} ${digits.substring(3, 6)} ${digits.substring(6)}"
    } else {
        "+51 $digits"
    }

/** "Carla Mendoza" → "CM"; a single name gives one letter; blank gives "?". */
fun initialsOf(fullName: String): String =
    fullName.trim()
        .split(Regex("\\s+"))
        .filter { it.isNotEmpty() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
        .ifEmpty { "?" }

/** Role wording is gender-neutral: the backend exposes no gender to pick "Dueño" or "Dueña". */
@StringRes
fun UserRole.labelRes(): Int = when (this) {
    UserRole.OWNER -> R.string.profile_role_owner
    UserRole.SELLER -> R.string.profile_role_seller
    UserRole.ADMIN -> R.string.profile_role_admin
    UserRole.DEVICE, UserRole.UNKNOWN -> R.string.profile_role_unknown
}

/** One line of the plan checklist, derived only from what the backend reports. */
sealed interface PlanFeatureLine {
    data class ProductLimit(val limit: Int) : PlanFeatureLine
    data object UnlimitedProducts : PlanFeatureLine
    data object AdvancedAnalytics : PlanFeatureLine
    data object VideoProducts : PlanFeatureLine
    data object BulkPriceUpdate : PlanFeatureLine
    data object PrioritySupport : PlanFeatureLine
}

fun PlanFeatures.lines(): List<PlanFeatureLine> = buildList {
    add(productLimit?.let { PlanFeatureLine.ProductLimit(it) } ?: PlanFeatureLine.UnlimitedProducts)
    if (hasAdvancedAnalytics) add(PlanFeatureLine.AdvancedAnalytics)
    if (hasVideoProducts) add(PlanFeatureLine.VideoProducts)
    if (hasBulkPriceUpdate) add(PlanFeatureLine.BulkPriceUpdate)
    if (hasPrioritySupport) add(PlanFeatureLine.PrioritySupport)
}

@Composable
@ReadOnlyComposable
fun PlanFeatureLine.label(): String = when (this) {
    is PlanFeatureLine.ProductLimit ->
        pluralStringResource(R.plurals.subscription_feature_product_limit, limit, limit)
    PlanFeatureLine.UnlimitedProducts -> stringResource(R.string.subscription_feature_unlimited)
    PlanFeatureLine.AdvancedAnalytics -> stringResource(R.string.subscription_feature_analytics)
    PlanFeatureLine.VideoProducts -> stringResource(R.string.subscription_feature_video)
    PlanFeatureLine.BulkPriceUpdate -> stringResource(R.string.subscription_feature_bulk_price)
    PlanFeatureLine.PrioritySupport -> stringResource(R.string.subscription_feature_support)
}

/** Elapsed fraction of the paid period, or `null` when the period has no usable bounds. */
fun periodProgress(startedAt: Instant?, endsAt: Instant?, now: Instant): Float? {
    if (startedAt == null || endsAt == null || !endsAt.isAfter(startedAt)) return null
    val total = Duration.between(startedAt, endsAt).toMillis().toFloat()
    val elapsed = Duration.between(startedAt, now).toMillis().toFloat()
    return (elapsed / total).coerceIn(0f, 1f)
}
