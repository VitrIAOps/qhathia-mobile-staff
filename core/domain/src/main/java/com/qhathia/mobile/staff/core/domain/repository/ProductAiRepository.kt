package com.qhathia.mobile.staff.core.domain.repository

import com.qhathia.mobile.staff.core.domain.model.ProductDraft
import com.qhathia.mobile.staff.core.domain.model.SelectedImage

/** Port for the AI-assisted product drafting endpoints used during registration. */
interface ProductAiRepository {

    /**
     * Drafts a description, suggested category and tags from a single product [image]. Both endpoints
     * are fail-open, so a successful [Result] may still carry an empty [ProductDraft]; only a genuine
     * transport failure yields a failed [Result].
     */
    suspend fun draftFromImage(image: SelectedImage): Result<ProductDraft>

    /**
     * Optimizes [currentDescription] into a paraphrase preserving all facts, returning the new text.
     * Fail-open: on LLM failure the original description is echoed back.
     */
    suspend fun optimizeDescription(currentDescription: String): Result<String>
}
