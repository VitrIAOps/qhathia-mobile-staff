package com.qhathia.mobile.staff.core.ui.component.productpreview

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

/**
 * What a product preview is drawn from: the values currently on a product form, saved or not.
 *
 * A null [description], [categoryName] or [stock] means the merchant has not entered it, and the
 * previews show a "not entered" placeholder in its place. [imageModels] are anything Coil can load —
 * local `content://` URIs or remote URLs — in display order, the first one being the primary photo.
 */
@Immutable
data class ProductPreviewData(
    val name: String,
    val price: Double,
    val description: String?,
    val categoryName: String?,
    val stock: Int?,
    val imageModels: ImmutableList<String>,
)
