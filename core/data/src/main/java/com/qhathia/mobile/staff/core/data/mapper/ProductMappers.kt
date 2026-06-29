package com.qhathia.mobile.staff.core.data.mapper

import com.qhathia.mobile.staff.core.domain.model.Availability
import com.qhathia.mobile.staff.core.domain.model.NewProduct
import com.qhathia.mobile.staff.core.domain.model.Product
import com.qhathia.mobile.staff.core.network.dto.CreateProductRequestDto
import com.qhathia.mobile.staff.core.network.dto.ProductDto
import com.qhathia.mobile.staff.core.network.dto.ProductResponseDto

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

/** Maps a domain [NewProduct] to the create-product request body (availability defaults to AVAILABLE). */
fun NewProduct.toCreateRequestDto(): CreateProductRequestDto = CreateProductRequestDto(
    name = name,
    description = description,
    price = price,
    categoryId = categoryId,
    tags = tags,
    // TODO: send `quantity` once the API supports a quantity/stock field (pending backend).
)

/** The id of the just-created product. */
fun ProductResponseDto.toProductId(): String = id

/** Unknown or absent availability strings degrade to [Availability.CONSULT]. */
private fun String?.toAvailability(): Availability = when (this) {
    "AVAILABLE" -> Availability.AVAILABLE
    "LOW_STOCK" -> Availability.LOW_STOCK
    else -> Availability.CONSULT
}
