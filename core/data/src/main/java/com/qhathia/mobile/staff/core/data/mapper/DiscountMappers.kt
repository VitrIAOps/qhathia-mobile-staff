package com.qhathia.mobile.staff.core.data.mapper

import com.qhathia.mobile.staff.core.domain.model.Discount
import com.qhathia.mobile.staff.core.domain.model.DiscountType
import com.qhathia.mobile.staff.core.domain.model.ProductDiscount
import com.qhathia.mobile.staff.core.network.dto.CreateDiscountRequestDto
import com.qhathia.mobile.staff.core.network.dto.DiscountDto
import java.time.Instant

/**
 * Maps a transport [DiscountDto] to the domain [ProductDiscount], or null when the row cannot be
 * understood (a discount type this app version does not know, or an unparseable date) so one odd
 * row never fails the whole list.
 */
fun DiscountDto.toDomainOrNull(): ProductDiscount? {
    val type = when (discountType) {
        "PERCENTAGE" -> DiscountType.PERCENTAGE
        "FIXED_AMOUNT" -> DiscountType.FIXED_AMOUNT
        else -> return null
    }
    val start = runCatching { Instant.parse(startsAt) }.getOrNull() ?: return null
    val end = runCatching { Instant.parse(endsAt) }.getOrNull() ?: return null
    return ProductDiscount(
        id = id,
        discount = Discount(type, value),
        startsAt = start,
        endsAt = end,
        active = active,
    )
}

/** Maps a domain [Discount] and its validity window to the body of the create call. */
fun Discount.toCreateRequestDto(startsAt: Instant, endsAt: Instant): CreateDiscountRequestDto =
    CreateDiscountRequestDto(
        discountType = type.name,
        value = value,
        startsAt = startsAt.toString(),
        endsAt = endsAt.toString(),
    )
