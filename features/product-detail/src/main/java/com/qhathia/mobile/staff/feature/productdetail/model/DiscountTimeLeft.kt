package com.qhathia.mobile.staff.feature.productdetail.model

import java.time.Duration
import java.time.Instant

/** How long a discount has left, in the units the countdown line reads in. Whole units, rounded down. */
sealed interface DiscountTimeLeft {
    /** One day or more; [hours] are the leftover hours and may be zero. */
    data class DaysAndHours(val days: Long, val hours: Long) : DiscountTimeLeft

    /** Less than a day but at least one hour. */
    data class Hours(val hours: Long) : DiscountTimeLeft

    /** Less than one hour. */
    data object UnderAnHour : DiscountTimeLeft
}

/** The time left until [endsAt] seen from [now], or null once the discount has ended. */
fun remainingDiscountTime(endsAt: Instant, now: Instant): DiscountTimeLeft? {
    val left = Duration.between(now, endsAt)
    if (left.isNegative || left.isZero) return null
    val days = left.toDays()
    val hours = left.minusDays(days).toHours()
    return when {
        days > 0 -> DiscountTimeLeft.DaysAndHours(days, hours)
        hours > 0 -> DiscountTimeLeft.Hours(hours)
        else -> DiscountTimeLeft.UnderAnHour
    }
}
