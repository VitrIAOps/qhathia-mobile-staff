package com.qhathia.mobile.staff.feature.productregister.model

import com.qhathia.mobile.staff.core.common.validation.parsePrice
import com.qhathia.mobile.staff.core.common.validation.parseQuantity
import com.qhathia.mobile.staff.core.ui.component.productpreview.ProductPreviewData
import kotlinx.collections.immutable.toImmutableList

/**
 * What the "Vista previa" dialog draws for the form as it stands, or null while there is no valid
 * price to show. The price is what a shopper would pay, so an applied discount is already taken
 * off. Optional fields the merchant has not filled in map to null, which the preview renders as its
 * "not entered" placeholder.
 */
fun ProductRegisterUiState.toPreviewData(): ProductPreviewData? {
    val shownPrice = discountedPrice ?: parsePrice(price) ?: return null
    return ProductPreviewData(
        name = name.trim(),
        price = shownPrice,
        description = description.trim().ifBlank { null },
        categoryName = categories.firstOrNull { it.id == selectedCategoryId }?.name,
        stock = parseQuantity(quantity),
        imageModels = images.map { it.uri }.toImmutableList(),
    )
}
