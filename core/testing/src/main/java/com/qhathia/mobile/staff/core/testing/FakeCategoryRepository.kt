package com.qhathia.mobile.staff.core.testing

import com.qhathia.mobile.staff.core.domain.model.Category
import com.qhathia.mobile.staff.core.domain.repository.CategoryRepository

/** Controllable [CategoryRepository] for tests. */
class FakeCategoryRepository : CategoryRepository {

    var categoriesResult: Result<List<Category>> = Result.success(listOf(TestFixtures.category()))

    var callCount: Int = 0
        private set

    override suspend fun getCategories(): Result<List<Category>> {
        callCount++
        return categoriesResult
    }
}
