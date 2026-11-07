package com.qhathia.mobile.staff.core.domain.model

/**
 * The store's catalog as the tablets see it. [active] counts every active product; [visible] the
 * ones a tablet actually shows (out of stock, low on stock and "a consultar" are hidden), or null
 * when the backend does not report it.
 */
data class CatalogCount(
    val active: Int,
    val visible: Int? = null,
) {
    /** What the card shows: the visible products, or every active one when that is unknown. */
    val shown: Int get() = visible ?: active

    /** Active products the tablet hides; 0 when [visible] is unknown. */
    val hidden: Int get() = visible?.let { (active - it).coerceAtLeast(0) } ?: 0
}
