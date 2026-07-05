package com.qhathia.mobile.staff.core.testing

import com.qhathia.mobile.staff.core.domain.model.ProductDraft
import com.qhathia.mobile.staff.core.domain.model.SelectedImage
import com.qhathia.mobile.staff.core.domain.repository.ProductAiRepository

/**
 * Controllable [ProductAiRepository] for tests. Set the `*Result` fields to the value the next call
 * should return; the last arguments and call counts are exposed for assertions.
 */
class FakeProductAiRepository : ProductAiRepository {

    var draftResult: Result<ProductDraft> =
        Result.success(ProductDraft(description = "", categoryId = null, tags = emptyList()))
    var optimizeResult: Result<String> = Result.success("")

    var lastDraftImage: SelectedImage? = null
        private set
    var draftCallCount: Int = 0
        private set

    var lastOptimizeInput: String? = null
        private set
    var optimizeCallCount: Int = 0
        private set

    override suspend fun draftFromImage(image: SelectedImage): Result<ProductDraft> {
        draftCallCount++
        lastDraftImage = image
        return draftResult
    }

    override suspend fun optimizeDescription(currentDescription: String): Result<String> {
        optimizeCallCount++
        lastOptimizeInput = currentDescription
        return optimizeResult
    }
}
