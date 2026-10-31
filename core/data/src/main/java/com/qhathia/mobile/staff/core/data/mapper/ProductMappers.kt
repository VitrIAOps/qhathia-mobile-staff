package com.qhathia.mobile.staff.core.data.mapper

import com.qhathia.mobile.staff.core.domain.model.Availability
import com.qhathia.mobile.staff.core.domain.model.NewProduct
import com.qhathia.mobile.staff.core.domain.model.Product
import com.qhathia.mobile.staff.core.domain.model.ProductDetail
import com.qhathia.mobile.staff.core.domain.model.ProductDraft
import com.qhathia.mobile.staff.core.domain.model.ProductImage
import com.qhathia.mobile.staff.core.domain.model.ProductUpdate
import com.qhathia.mobile.staff.core.network.dto.CreateProductRequestDto
import com.qhathia.mobile.staff.core.network.dto.DraftFromImageResponseDto
import com.qhathia.mobile.staff.core.network.dto.ProductDetailDto
import com.qhathia.mobile.staff.core.network.dto.ProductDto
import com.qhathia.mobile.staff.core.network.dto.ProductMediaDto
import com.qhathia.mobile.staff.core.network.dto.ProductResponseDto

private const val MEDIA_TYPE_IMAGE = "IMAGE"

/** Maps a transport [ProductDto] to the domain [Product]. */
fun ProductDto.toDomain(): Product = Product(
    id = id,
    categoryId = categoryId,
    categoryName = categoryName,
    name = name,
    price = price,
    availability = availability.toAvailability(),
    primaryImageUrl = primaryImageUrl,
    stock = stock,
    discountedPrice = discountedPrice,
    discountEndsAt = discountEndsAt.toInstantOrNull(),
)

/**
 * Maps a transport [ProductDetailDto] to the domain [ProductDetail]. Media becomes the product's
 * photos (id kept, so one can be deleted) in the order the DS shows them — primary photo first, then
 * by backend position — and non-image media is dropped, since the detail screen only renders photos.
 */
fun ProductDetailDto.toDomain(): ProductDetail = ProductDetail(
    id = id,
    name = name,
    description = description,
    price = price,
    stock = stock,
    categoryId = categoryId,
    categoryName = categoryName,
    tags = tags,
    images = media
        .filter { it.mediaType == MEDIA_TYPE_IMAGE }
        .sortedWith(compareByDescending<ProductMediaDto> { it.primary }.thenBy { it.position })
        .map { ProductImage(id = it.id, url = it.url) },
    allowDiscountInquiry = allowDiscountInquiry,
    position = position,
    discountedPrice = discountedPrice,
    discountEndsAt = discountEndsAt.toInstantOrNull(),
)

/**
 * Maps a domain [ProductUpdate] to the body of `PUT /api/v1/products/{id}`, which takes the same
 * shape as the create request. As on create, `availability` is not set so the backend derives it
 * from the stock.
 */
fun ProductUpdate.toUpdateRequestDto(): CreateProductRequestDto = CreateProductRequestDto(
    name = name,
    description = description,
    price = price,
    stock = stock,
    allowDiscountInquiry = allowDiscountInquiry,
    categoryId = categoryId,
    tags = tags,
    position = position,
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
