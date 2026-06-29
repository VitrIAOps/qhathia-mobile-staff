package com.qhathia.mobile.staff.core.domain.repository

import com.qhathia.mobile.staff.core.domain.model.SelectedImage

/** Port for compressing and uploading product images to object storage, returning the public URL. */
interface MediaStorageRepository {

    /** Compresses [image] to the size/format limits and uploads it, returning its public URL. */
    suspend fun uploadImage(image: SelectedImage): Result<String>
}
