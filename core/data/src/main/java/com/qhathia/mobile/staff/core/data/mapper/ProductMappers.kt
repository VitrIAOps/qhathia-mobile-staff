package com.qhathia.mobile.staff.core.data.mapper

import com.qhathia.mobile.staff.core.domain.model.Availability
import com.qhathia.mobile.staff.core.domain.model.NewProduct
import com.qhathia.mobile.staff.core.domain.model.Product
import com.qhathia.mobile.staff.core.domain.model.ProductDraft
import com.qhathia.mobile.staff.core.network.dto.CreateProductRequestDto
import com.qhathia.mobile.staff.core.network.dto.DraftFromImageResponseDto
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

/**
 * Maps a domain [NewProduct] to the `product` part of the create-with-images request. `quantity` is
 * sent as `stock`; `availability` is not set, so the backend derives it from the stock.
 */
fun NewProduct.toCreateRequestDto(): CreateProductRequestDto = CreateProductRequestDto(
    name = name,
    description = description,
    price = price,
    stock = quantity,
    categoryId = categoryId,
    tags = tags,
)

/** The id of the just-created product. */
fun ProductResponseDto.toProductId(): String = id

/** Maps a transport [DraftFromImageResponseDto] to the domain [ProductDraft]. */
fun DraftFromImageResponseDto.toDomain(): ProductDraft = ProductDraft(
    description = description,
    categoryId = categoryId,
    tags = tags,
)

/** Unknown or absent availability strings degrade to [Availability.CONSULT]. */
private fun String?.toAvailability(): Availability = when (this) {
    "AVAILABLE" -> Availability.AVAILABLE
    "LOW_STOCK" -> Availability.LOW_STOCK
    else -> Availability.CONSULT
}
