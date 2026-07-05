package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.repository.ProductAiRepository
import javax.inject.Inject

/** Optimizes an existing product description into a fact-preserving paraphrase. */
class OptimizeDescriptionUseCase @Inject constructor(
    private val repository: ProductAiRepository,
) {
    suspend operator fun invoke(currentDescription: String): Result<String> =
        repository.optimizeDescription(currentDescription)
}
