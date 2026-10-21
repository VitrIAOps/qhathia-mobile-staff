package com.qhathia.mobile.staff.feature.productdetail.model

import com.qhathia.mobile.staff.core.domain.model.ProductImage
import com.qhathia.mobile.staff.core.domain.model.SelectedImage

/**
 * A photo as the edit form shows it: one the product already has ([Saved]) or one picked on this
 * screen and not uploaded yet ([Pending]). [key] identifies it for removal; [model] is what the
 * image loader draws (the remote URL, or the local content uri).
 */
sealed interface FormPhoto {
    val key: String
    val model: String

    data class Saved(val image: ProductImage) : FormPhoto {
        override val key: String get() = image.id
        override val model: String get() = image.url
    }

    data class Pending(val image: SelectedImage) : FormPhoto {
        override val key: String get() = image.uri
        override val model: String get() = image.uri
    }
}
