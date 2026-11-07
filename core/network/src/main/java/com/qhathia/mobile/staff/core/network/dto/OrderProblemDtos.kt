package com.qhathia.mobile.staff.core.network.dto

import kotlinx.serialization.Serializable

/**
 * The RFC 7807 ProblemDetail body of a failed order transition. `errorCode` is what distinguishes
 * the two `422`s the contract documents — `STOCK_INSUFFICIENT` (which also carries `productName`,
 * `available` and `requested`) and `INVALID_STATE_TRANSITION` — so the status code alone is not
 * enough to pick a message.
 *
 * Every field is optional: the shared ProblemDetail envelope carries `type`/`title`/`detail` too,
 * and the extra fields only appear on the stock case. Unknown keys are ignored by the shared Json.
 */
@Serializable
data class OrderProblemDto(
    val errorCode: String? = null,
    val detail: String? = null,
    val productId: String? = null,
    val productName: String? = null,
    val available: Int? = null,
    val requested: Int? = null,
    /** Why the product cannot be sold (`PRODUCT_NOT_SELLABLE`): `INACTIVE`, `OUT_OF_STOCK`, `CONSULT`, `LOW_STOCK`. */
    val reason: String? = null,
)
