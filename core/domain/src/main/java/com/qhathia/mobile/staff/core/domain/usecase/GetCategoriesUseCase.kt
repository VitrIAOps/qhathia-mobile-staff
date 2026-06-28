package com.qhathia.mobile.staff.core.domain.usecase

import com.qhathia.mobile.staff.core.domain.model.Category
import com.qhathia.mobile.staff.core.domain.repository.CategoryRepository
import javax.inject.Inject

/** Lists the store's product categories used by the inventory filter strip. */
class GetCategoriesUseCase @Inject constructor(
    private val repository: CategoryRepository,
) {
    suspend operator fun invoke(): Result<List<Category>> = repository.getCategories()
}
