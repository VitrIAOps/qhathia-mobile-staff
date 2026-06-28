package com.qhathia.mobile.staff.core.data.mapper

import com.qhathia.mobile.staff.core.domain.model.Availability
import com.qhathia.mobile.staff.core.domain.model.Product
import com.qhathia.mobile.staff.core.network.dto.ProductDto

/** Maps a transport [ProductDto] to the domain [Product]. */
fun ProductDto.toDomain(): Product = Product(
    id = id,
    categoryId = categoryId,
    categoryName = categoryName,
    name = name,
    price = price,
    availability = availability.toAvailability(),
    primaryImageUrl = primaryImageUrl,
)

/** Unknown or absent availability strings degrade to [Availability.CONSULT]. */
private fun String?.toAvailability(): Availability = when (this) {
    "AVAILABLE" -> Availability.AVAILABLE
    "LOW_STOCK" -> Availability.LOW_STOCK
    else -> Availability.CONSULT
}
