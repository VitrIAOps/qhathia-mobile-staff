package com.qhathia.mobile.staff.core.notifications

/** What the presenter needs to know about one notification on the orders channel. */
internal data class TrayEntry(val tag: String?, val id: Int, val isSummary: Boolean)

/** Counts the order notifications under the group summary, without touching Android. */
internal object OrderSummary {

    /** The key of a child: its order id, or its notification id when it was posted without one. */
    fun keyOf(tag: String?, id: Int): String = tag ?: "#$id"

    /**
     * Keys of the order notifications in [entries], never a group summary (ours or the system's).
     * [including] / [excluding] correct for the tray lagging behind a notify / cancel just issued.
     */
    fun childKeys(
        entries: List<TrayEntry>,
        including: String? = null,
        excluding: String? = null,
    ): Set<String> = entries
        .filterNot { it.isSummary }
        .mapTo(HashSet()) { keyOf(it.tag, it.id) }
        .apply {
            including?.let(::add)
            excluding?.let(::remove)
        }
}
