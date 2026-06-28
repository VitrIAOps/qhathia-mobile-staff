package com.qhathia.mobile.staff.core.domain.repository

import com.qhathia.mobile.staff.core.domain.model.Category

/** Port for reading the store's product categories. */
interface CategoryRepository {

    suspend fun getCategories(): Result<List<Category>>
}
