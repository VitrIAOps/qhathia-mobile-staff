package com.qhathia.mobile.staff.feature.salesstatistics.ui

/**
 * Which caption the "Lo que necesita atención" card uses. Its amount adds up the listed products
 * only, so the caption must not suggest the list is everything when the backend says more products
 * sold nothing.
 */
internal sealed interface NeedsAttentionCaption {

    /** "Inmovilizados en los N productos sin ventas": the list is the whole set, or all we know. */
    data class All(val listed: Int) : NeedsAttentionCaption

    /** "Inmovilizados en N de los M productos sin ventas". */
    data class Partial(val listed: Int, val total: Int) : NeedsAttentionCaption
}

/**
 * [totalCount] is the backend's count of products without sales. A missing count, or one not larger
 * than the list (including an inconsistent smaller one), never claims more than is on screen.
 */
internal fun needsAttentionCaption(listed: Int, totalCount: Int?): NeedsAttentionCaption =
    if (totalCount != null && totalCount > listed) {
        NeedsAttentionCaption.Partial(listed = listed, total = totalCount)
    } else {
        NeedsAttentionCaption.All(listed = listed)
    }
