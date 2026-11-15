package com.qhathia.mobile.staff.core.ui.component

import android.content.res.Resources
import androidx.compose.runtime.Immutable
import com.qhathia.mobile.staff.core.common.validation.MAX_PRODUCT_PHOTOS
import com.qhathia.mobile.staff.core.ui.R

/**
 * What a gallery or camera pick did to a product's photos: how many were [added], how many were left
 * out for not being JPG or PNG ([notJpgOrPng]) or for the limit ([overLimit]), and the [total] now on
 * the form. Registro and Detalle build it the same way and show one message from it (UX-C-03).
 */
@Immutable
data class PhotoPickResult(
    val added: Int,
    val notJpgOrPng: Int,
    val overLimit: Int,
    val total: Int,
) {
    /** A plain pick that fit needs no message; anything skipped, or reaching the limit, does. */
    val needsMessage: Boolean
        get() = notJpgOrPng > 0 || overLimit > 0 || (added > 0 && total >= MAX_PRODUCT_PHOTOS)
}

/** "Se agregaron 5. 3 no son JPG ni PNG y no se agregaron. Ya tienes 10 fotos." */
fun Resources.photoPickMessage(result: PhotoPickResult): String = buildList {
    if (result.added > 0) add(getQuantityString(R.plurals.photo_pick_added, result.added, result.added))
    if (result.notJpgOrPng > 0) {
        add(getQuantityString(R.plurals.photo_pick_not_jpg_png, result.notJpgOrPng, result.notJpgOrPng))
    }
    if (result.overLimit > 0) {
        add(getQuantityString(R.plurals.photo_pick_over_limit, result.overLimit, result.overLimit, MAX_PRODUCT_PHOTOS))
    } else if (result.total >= MAX_PRODUCT_PHOTOS) {
        add(getString(R.string.photo_pick_limit_reached, MAX_PRODUCT_PHOTOS))
    }
}.joinToString(" ")
