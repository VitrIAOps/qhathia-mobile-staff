package com.qhathia.mobile.staff.core.data.mapper

import com.qhathia.mobile.staff.core.domain.model.Category
import com.qhathia.mobile.staff.core.network.dto.CategoryDto

/** Maps a transport [CategoryDto] to the domain [Category]. */
fun CategoryDto.toDomain(): Category = Category(
    id = id,
    name = name,
)
