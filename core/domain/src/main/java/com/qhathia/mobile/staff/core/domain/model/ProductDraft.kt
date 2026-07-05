package com.qhathia.mobile.staff.core.domain.model

/**
 * An AI-generated draft of a product's editable fields, produced from a product image. [categoryId]
 * is a suggested store category id or null when the model recognized none. An "empty" draft (blank
 * [description], null [categoryId], empty [tags]) is the fail-open signal that the LLM could not
 * produce anything — callers treat it as "leave the form unchanged".
 */
data class ProductDraft(
    val description: String,
    val categoryId: String?,
    val tags: List<String>,
)
