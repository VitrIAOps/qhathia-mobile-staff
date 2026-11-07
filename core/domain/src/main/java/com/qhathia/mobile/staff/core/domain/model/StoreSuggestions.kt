package com.qhathia.mobile.staff.core.domain.model

/**
 * Something kiosk shoppers looked for in this store and did not find. [id] is the canonical search
 * term and [name] the same term ready to show (first letter capitalised). [example] is the way
 * shoppers most often asked for it, or null when it says nothing the term does not. [personCount]
 * is how many distinct people asked — someone retrying nine times is one person of demand, not nine.
 * When [personCountEstimated], there were no sessions to tell people apart and [personCount] counts
 * searches instead. [variants] are the other terms shoppers used for the same product, at most three.
 */
data class MissingProductSuggestion(
    val id: String,
    val name: String,
    val personCount: Int,
    val example: String? = null,
    val variants: List<String> = emptyList(),
    val personCountEstimated: Boolean = false,
)

/** A product the store still carries but is about to run out of. [id] is the product id. */
data class RestockSuggestion(
    val id: String,
    val name: String,
    val imageUrl: String?,
    val remainingStock: Int,
)

/**
 * The "sugerencias para vender más" rows. [otherRequests] is demand outside the store's line of
 * business, shown apart and without an "Agregar" action. Any list may be empty — the screen omits an
 * empty row rather than rendering an empty one.
 */
data class StoreSuggestions(
    val missingProducts: List<MissingProductSuggestion>,
    val restockProducts: List<RestockSuggestion>,
    val otherRequests: List<MissingProductSuggestion> = emptyList(),
)
