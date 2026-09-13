package com.qhathia.mobile.staff.core.domain.model

/**
 * Something kiosk shoppers looked for in this store and did not find. [id] is the canonical search
 * term, [name] the way shoppers most often typed it, and [personCount] how many distinct people
 * asked — someone retrying nine times is one person of demand, not nine.
 */
data class MissingProductSuggestion(
    val id: String,
    val name: String,
    val personCount: Int,
)

/** A product the store still carries but is about to run out of. [id] is the product id. */
data class RestockSuggestion(
    val id: String,
    val name: String,
    val imageUrl: String?,
    val remainingStock: Int,
)

/**
 * Both "sugerencias para vender más" rows. Either list may be empty — the screen omits an empty row
 * rather than rendering an empty one.
 */
data class StoreSuggestions(
    val missingProducts: List<MissingProductSuggestion>,
    val restockProducts: List<RestockSuggestion>,
)
