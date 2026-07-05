package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.model.ProductDraft
import com.qhathia.mobile.staff.core.domain.model.SelectedImage
import com.qhathia.mobile.staff.core.domain.repository.ProductAiRepository
import javax.inject.Inject

/** Drafts a product's description, category and tags from a single product [SelectedImage]. */
class DraftProductFromImageUseCase @Inject constructor(
    private val repository: ProductAiRepository,
) {
    suspend operator fun invoke(image: SelectedImage): Result<ProductDraft> =
        repository.draftFromImage(image)
}
